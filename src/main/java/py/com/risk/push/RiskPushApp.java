package py.com.risk.push;

import py.com.risk.push.bd.DBService;
import py.com.risk.push.config.*;
import py.com.risk.push.model.ModoEnvioLote;
import py.com.risk.push.model.Plataforma;
import py.com.risk.push.model.PushMessage;
import py.com.risk.push.util.ContextAwareThreadFactory;
import py.com.risk.push.firebase.*;
import py.com.risk.push.huawei.HuaweiHttpSender;
import py.com.risk.push.huawei.HuaweiPushClient;
import py.com.risk.push.huawei.HuaweiTokenManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * Clase principal de ejecución del servicio RiskPushApp.
 * 
 * Esta aplicación se encarga de leer mensajes pendientes desde una base de datos,
 * clasificarlos por servicio/proveedor configurado y enviar notificaciones push
 * mediante Firebase (Admin SDK o HTTP) o Huawei Push Kit.
 * 
 * El comportamiento es altamente configurable mediante un archivo YAML, permitiendo múltiples servicios.
 * Soporta procesamiento concurrente, modos de envío escalonados, y shutdown controlado.
 * 
 * @author Damián Meza
 * @version 1.0.0
 */
public class RiskPushApp {

    private static final Logger logger = LogManager.getLogger(RiskPushApp.class);

    /** Lista de instancias activas de PushSender, una por configuración de envío. */
    private static final List<py.com.risk.push.PushSender> senderList = new CopyOnWriteArrayList<>();

    /** Bandera global para mantener la aplicación corriendo. */
    private static volatile boolean running = true;

    /** Ruta por defecto al archivo de configuración YAML. */
    private static String propsFilePath = "config/risk-push.yml";

    /**
     * Método principal de ejecución del programa.
     * 
     * @param args argumentos de línea de comandos (puede incluir la ruta del archivo YAML).
     * @throws Exception si ocurre un error en la inicialización o ejecución principal.
     */
    public static void main(String[] args) throws Exception {
        ThreadContext.put("servicio", "default");
        logger.info("Iniciando RiskPushApp...");
        propsFilePath = args.length > 0 ? args[0] : propsFilePath;

        final RiskPushConfig config = loadConfig(propsFilePath);
        final DataSourceConfig ds = config.getDatasource();
        List<PushConfig> pushConfigs = config.getPush();

        ExecutorService executor = Executors.newFixedThreadPool(pushConfigs.size(), new ContextAwareThreadFactory());

        // Manejo de apagado ordenado
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ThreadContext.put("servicio", "default");
            logger.info("Apagando RiskPushApp...");
            running = false;
            senderList.forEach(py.com.risk.push.PushSender::shutdown);
            executor.shutdown();
            logger.info("RiskPushApp finalizado.");
        }));

        // Crear un hilo para cada servicio configurado
        pushConfigs.forEach(pushConfig -> {
            executor.submit(() -> {
                ThreadContext.put("servicio", pushConfig.getNombre());
                try {
                    FirebaseHttpClient firebaseHttpClient = new FirebaseHttpClient(
                        "banco-atlas-app-ios",
                        pushConfig.getFirebase().getServiceAccountPath()
                    );

                    HuaweiPushClient huaweiPushClient = null;

                    if (pushConfig.getHuawei() != null) {
                        HuaweiTokenManager tokenManager = new HuaweiTokenManager(
                            pushConfig.getHuawei().getAppId(),
                            pushConfig.getHuawei().getAppSecret(),
                            pushConfig.getHuawei().getTokenUrl()
                        );

                        HuaweiHttpSender httpSender = new HuaweiHttpSender(tokenManager, pushConfig.getHuawei().getApiUrl());

                        huaweiPushClient = new HuaweiPushClient(httpSender);
                    }

                    runExecution(ds, pushConfig, firebaseHttpClient, huaweiPushClient);
                } catch (Exception e) {
                    logger.error(String.format("Error inesperado al ejecutar el envío de: [%s]", pushConfig.getNombre()), e);
                } finally {
                    ThreadContext.clearAll();
                }
            });
        });
    }

    /**
     * Carga la configuración YAML desde el archivo indicado.
     * 
     * @param filePath ruta del archivo de configuración.
     * @return objeto de configuración cargado.
     * @throws IOException si no se puede leer el archivo.
     */
    public static RiskPushConfig loadConfig(String filePath) throws IOException {
        Yaml yaml = new Yaml(new Constructor(RiskPushConfig.class));
        try (FileInputStream input = new FileInputStream(filePath)) {
            return yaml.load(input);
        }
    }

    /**
     * Ejecuta el envío de mensajes en modo bucle según la configuración del servicio.
     * 
     * @param dsConfig configuración de origen de datos.
     * @param pushConfig configuración del servicio de envío push.
     * @param firebaseHttpClient cliente HTTP para Firebase.
     * @param huaweiPushClient cliente de envío para Huawei.
     * @throws Exception si ocurre un error durante la ejecución.
     */
    public static void runExecution(
        DataSourceConfig dsConfig,
        PushConfig pushConfig,
        FirebaseHttpClient firebaseHttpClient,
        HuaweiPushClient huaweiPushClient
    ) throws Exception {
        logger.info("Nombre del servicio: [{}]", pushConfig.getNombre());

        final DBService dbService = new DBService(dsConfig);
        dbService.setMaximoIntentos(pushConfig.getMaximoIntentos());

        if (Plataforma.FCM.equals(pushConfig.getPlataforma())) {
            PushSender.initFirebase(pushConfig.getFirebase().getServiceAccountPath());
        }

        final py.com.risk.push.PushSender sender = new py.com.risk.push.PushSender(
            dbService, pushConfig, firebaseHttpClient, huaweiPushClient
        );

        senderList.add(sender);

        int count = 1;
        final long intervalo = pushConfig.getIntervaloEntreLotesMs();
        final ModoEnvioLote modo = pushConfig.getModoEnvioLote();

        while (running) {
            ThreadContext.put("contador", String.valueOf(count));
            try {
                List<PushMessage> mensajes = dbService.loadPendingPushMessages(
                    pushConfig.getPlataforma().name(),
                    pushConfig.getClasificacion(),
                    pushConfig.getCantidadMaximaPorLote()
                );

                if (!mensajes.isEmpty()) {
                    logger.info("Mensajes pendientes para enviar: [{}], Modo de envío: [{}]", mensajes.size(), modo);
                    sender.sendMessages(modo, mensajes);
                } else {
                    logger.info("No se encontraron mensajes pendientes para enviar");
                }

                logger.info("Durmiendo [{}] ms...", intervalo);
                Thread.sleep(intervalo);
                logger.info("Reintentando lectura...");
            } catch (Exception e) {
                logger.error("Error al procesar lote de mensajes: [{}]", e.getMessage());
                Thread.sleep(intervalo);
            }
            count = (count >= 100) ? 1 : count + 1;
        }
    }
}