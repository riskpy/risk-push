package py.com.risk.push.huawei;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import py.com.risk.push.model.PushClient;
import py.com.risk.push.model.PushMessage;

/**
 * Implementación del cliente de envío de mensajes push para la plataforma Huawei.
 * <p>
 * Esta clase se encarga de construir el payload adecuado para Huawei Push Kit y enviarlo
 * a través del componente {@link HuaweiHttpSender}.
 * 
 * @author Damián Meza
 * @version 1.0.0
 */
public class HuaweiPushClient implements PushClient {

    private static final Logger logger = LogManager.getLogger(HuaweiPushClient.class);

    /** Cliente HTTP responsable de enviar el mensaje a la API de Huawei */
    private final HuaweiHttpSender httpSender;

    /**
     * Constructor que inicializa el cliente con un {@link HuaweiHttpSender} configurado.
     *
     * @param httpSender Cliente HTTP utilizado para realizar el envío del mensaje a Huawei
     */
    public HuaweiPushClient(HuaweiHttpSender httpSender) {
        this.httpSender = httpSender;
    }

    /**
     * Envía un mensaje push utilizando la API de Huawei.
     *
     * @param msg Objeto {@link PushMessage} que contiene los datos del mensaje a enviar
     * @return La respuesta en formato String recibida por parte de Huawei Push Kit
     * @throws Exception si ocurre un error en la construcción o el envío del mensaje
     */
    @Override
    public String send(PushMessage msg) throws Exception {
        logger.debug("Construyendo payload Huawei para el mensaje: {}", msg.getIdMensaje());

        // Construir el JSON requerido por Huawei
        String payload = HuaweiPayloadBuilder.buildJsonPayload(msg);

        logger.debug("Payload generado: {}", payload);

        // Enviar el mensaje a través del cliente HTTP
        String response = httpSender.sendToHuawei(payload);

        logger.info("Respuesta Huawei para mensaje {}: {}", msg.getIdMensaje(), response);

        return response;
    }
}
