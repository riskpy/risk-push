package py.com.risk.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import py.com.risk.push.bd.DBService;
import py.com.risk.push.config.PushConfig;
import py.com.risk.push.firebase.FirebaseHttpClient;
import py.com.risk.push.firebase.FirebasePayloadBuilder;
import py.com.risk.push.huawei.HuaweiPushClient;
import py.com.risk.push.model.ModoEnvioLote;
import py.com.risk.push.model.Plataforma;
import py.com.risk.push.model.PushClient;
import py.com.risk.push.model.PushMessage;
import py.com.risk.push.model.PushMessage.Status;
import py.com.risk.push.util.ContextAwareThreadFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Encargado de enviar mensajes push a múltiples plataformas (Firebase, Huawei).
 * 
 * Esta clase administra la lógica de concurrencia, el control de reintentos,
 * la inicialización del SDK Firebase y la delegación del envío mediante interfaces PushClient.
 * 
 * @author Damián Meza
 * @version 1.0.0
 */
public class PushSender {

    private static final Logger logger = LogManager.getLogger(PushSender.class);

    /** Executor para envíos paralelos sin retardo */
    private final ExecutorService executor = Executors.newFixedThreadPool(20, new ContextAwareThreadFactory());

    /** Executor programado para envíos espaciados */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new ContextAwareThreadFactory());

    /** Mapa de plataformas soportadas (FCM, HMS) a su respectivo cliente de envío */
    private final Map<Plataforma, PushClient> pushClients;

    /** Servicio de base de datos utilizado para consultar y actualizar mensajes */
    private final DBService dbService;

    /** Retardo por defecto entre mensajes en modos espaciados (en milisegundos) */
    private static final long DEFAULT_DELAY_MS = 500;

    /**
     * Constructor principal de PushSender.
     * 
     * @param dbService servicio de acceso a datos
     * @param config configuración del envío push
     * @param firebaseHttpClient cliente para envíos HTTP a Firebase
     * @param huaweiPushClient cliente para envíos a Huawei
     * @throws FileNotFoundException si el archivo de credenciales Firebase no se encuentra
     * @throws IOException si ocurre un error al leer las credenciales
     */
    public PushSender(DBService dbService, PushConfig config, FirebaseHttpClient firebaseHttpClient, HuaweiPushClient huaweiPushClient) throws FileNotFoundException, IOException {
        this.dbService = dbService;
        this.pushClients = new HashMap<>();

        // Inicializa Firebase si no se hizo previamente
        if(config.getFirebase() != null && config.getFirebase().getServiceAccountPath() != null) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new FileInputStream(config.getFirebase().getServiceAccountPath())))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        }

        // Registro del cliente FCM
        this.pushClients.put(Plataforma.FCM, msg -> {
            if (msg.getDatosExtra() != null && !msg.getDatosExtra().isEmpty() && isComplexPayload(msg.getDatosExtra())) {
                logger().debug("Usando Firebase HTTP por datos_extra complejo");
                String jsonBody = FirebasePayloadBuilder.buildHttpJsonPayload(msg);
                return firebaseHttpClient.sendPush(jsonBody);
            } else {
                logger().debug("Usando Firebase Admin SDK");
                Message message = Message.builder()
                        .setToken(msg.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(msg.getTitulo())
                                .setBody(msg.getCuerpo())
                                .build())
                        .build();
                return FirebaseMessaging.getInstance().send(message);
            }
        });

        // Registro del cliente Huawei
        this.pushClients.put(Plataforma.HMS, huaweiPushClient);    
    }

    /**
     * Envía una lista de mensajes en el modo de envío especificado.
     * 
     * @param modoEnvio modo de ejecución (paralelo, secuencial, etc.)
     * @param messages lista de mensajes a enviar
     */
    public void sendMessages(ModoEnvioLote modoEnvio, List<PushMessage> messages) {
        long delayMs = DEFAULT_DELAY_MS;

        switch (modoEnvio) {
            case paralelo:
                String count = ThreadContext.get("contador");
                messages.forEach(msg -> executor.submit(() -> sendSingleMessage(msg, count)));
                break;
            case paralelo_espaciado:
                new ParallelWithDelaySender(messages, delayMs).start();
                break;
            case secuencial_espaciado:
                try {
                    sendMessagesSequentialWithDelayAsync(messages, delayMs).get();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
                break;
            case secuencial_espaciado_async:
                sendMessagesSequentialWithDelayAsync(messages, delayMs);
                break;
            default:
                sendMessagesSequentialWithDelayAsync(messages, delayMs);
        }
    }

    /**
     * Envía mensajes de forma secuencial con retardo asincrónico entre cada envío.
     */
    private CompletableFuture<Void> sendMessagesSequentialWithDelayAsync(List<PushMessage> messages, long delayMs) {
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        long delay = delayMs > 0 ? delayMs : DEFAULT_DELAY_MS;

        String count = ThreadContext.get("contador");
        for (PushMessage msg : messages) {
            future = future.thenRunAsync(() -> sendSingleMessage(msg, count), executor)
                           .thenCompose(v -> delayAsync(delay));
        }
        return future;
    }

    /**
     * Devuelve un future que se completa luego de cierto retardo.
     */
    private CompletableFuture<Void> delayAsync(long delayMs) {
        CompletableFuture<Void> delayFuture = new CompletableFuture<>();
        scheduler.schedule(() -> delayFuture.complete(null), delayMs, TimeUnit.MILLISECONDS);
        return delayFuture;
    }

    /**
     * Envía un mensaje individual y actualiza el estado en la base de datos.
     */
    private void sendSingleMessage(PushMessage msg, String count) {
        ThreadContext.put("contador", count);
        ThreadContext.put("idMensaje", String.valueOf(msg.getIdMensaje()));

        try {
            logger().info("Enviando mensaje push a token [{}]: {}", msg.getToken(), msg.getCuerpo());
            //dbService.updateMessageStatus(msg.getIdMensaje(), Status.EN_PROCESO_ENVIO, null, null, null);

            Plataforma plataforma = msg.getPlataforma();
            PushClient client = pushClients.get(plataforma);

            if (client == null) {
                String error = "Plataforma de push no soportado: " + plataforma;
                logger().error(error);
                dbService.updateMessageStatus(msg.getIdMensaje(), Status.PENDIENTE_ENVIO, "ERROR", error, null);
                return;
            }

            String response = client.send(msg);

            logger().info("Mensaje enviado correctamente. Response: {}", response);
            dbService.updateMessageStatus(msg.getIdMensaje(), Status.ENVIADO, "OK", null, response);

        } catch (Exception e) {
            logger().error("Error al enviar mensaje push: {}", e.getMessage(), e);
            dbService.updateMessageStatus(msg.getIdMensaje(), Status.PENDIENTE_ENVIO, "ERROR", e.getMessage(), null);
        } finally {
            ThreadContext.remove("idMensaje");
            ThreadContext.remove("contador");
        }
    }

    /**
     * Clase auxiliar que envía mensajes en paralelo con un retardo entre cada envío.
     */
    private class ParallelWithDelaySender {
        private final Iterator<PushMessage> iterator;
        private final long delayMs;
        private ScheduledFuture<?> future;

        public ParallelWithDelaySender(List<PushMessage> messages, long delayMs) {
            this.iterator = messages.iterator();
            this.delayMs = delayMs;
        }

        public void start() {
            String count = ThreadContext.get("contador");
            future = scheduler.scheduleWithFixedDelay(() -> {
                if (iterator.hasNext()) {
                    sendSingleMessage(iterator.next(), count);
                } else {
                    future.cancel(false);
                }
            }, 0, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Detiene los ejecutores de envío.
     */
    public void shutdown() {
        executor.shutdown();
        scheduler.shutdown();
    }

    /**
     * Determina si el contenido de datos_extra representa una carga compleja
     * que requiere el uso del cliente HTTP.
     */
    private boolean isComplexPayload(String datosExtra) {
        if (datosExtra == null || datosExtra.trim().isEmpty()) return false;
        return datosExtra.contains("apns") || datosExtra.contains("android") || datosExtra.contains("notification");
    }

    /**
     * Retorna el logger con la clase actual como contexto.
     */
    private org.apache.logging.log4j.Logger logger() {
        return org.apache.logging.log4j.LogManager.getLogger(PushSender.class);
    }

    /**
     * Inicializa manualmente Firebase desde una ruta, si es necesario.
     */
    public static void initFirebase(String path) throws Exception {
        PushSenderSingleton.getInstance().initFirebase(path);
    }

    /**
     * Singleton para inicialización única de Firebase.
     */
    private static class PushSenderSingleton {
        private static final PushSenderSingleton INSTANCE = new PushSenderSingleton();

        public static PushSenderSingleton getInstance() {
            return INSTANCE;
        }

        public static void initFirebase(String path) throws Exception {
            if (FirebaseApp.getApps().isEmpty()) {
                FileInputStream serviceAccount = new FileInputStream(path);
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
                logger.info("Firebase inicializado correctamente.");
            }
        }
    }
}
