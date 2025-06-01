package py.com.risk.push.firebase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import py.com.risk.push.model.PushMessage;

/**
 * Clase utilitaria encargada de construir el cuerpo JSON (payload) para el envío de notificaciones
 * push mediante Firebase HTTP v1.
 *
 * <p>Este constructor de payload permite combinar datos dinámicos proporcionados en el campo
 * {@code datos_extra} del mensaje con los campos clásicos como título, cuerpo y configuraciones
 * específicas de Android o APNs.</p>
 *
 * <p>Es compatible con personalizaciones complejas y adaptaciones específicas del lado cliente.</p>
 * 
 * @author Damián Meza
 * @version 1.0.0
 */
public class FirebasePayloadBuilder {

    /**
     * Construye el payload JSON completo para el envío del mensaje push mediante Firebase HTTP v1.
     *
     * @param msg Objeto {@link PushMessage} que contiene los datos del mensaje a enviar
     * @return Cadena JSON válida con la estructura requerida por Firebase
     * @throws Exception si ocurre un error al procesar o construir el JSON
     */
    public static String buildHttpJsonPayload(PushMessage msg) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Intenta parsear datos_extra si viene como JSON válido, o crear uno vacío
        JsonNode datosNode = msg.getDatosExtra() != null
                ? mapper.readTree(msg.getDatosExtra())
                : mapper.createObjectNode();

        ObjectNode messageNode = mapper.createObjectNode();

        // Establece el token del dispositivo destino
        messageNode.put("token", msg.getToken());

        // Prepara el nodo "data"
        ObjectNode dataNode = mapper.createObjectNode();
        if (datosNode.has("data") && datosNode.get("data").isObject()) {
            dataNode.setAll((ObjectNode) datosNode.get("data"));
            dataNode.remove("title"); // Limpia duplicados si existen
            dataNode.remove("body");
        }

        // Agrega título y cuerpo también en el bloque "data" (útil para clientes personalizados)
        if (msg.getTitulo() != null) dataNode.put("title", msg.getTitulo());
        if (msg.getCuerpo() != null) dataNode.put("body", msg.getCuerpo());

        messageNode.set("data", dataNode);

        // Si hay configuración Android en datos_extra, la incluye
        if (datosNode.has("android")) {
            messageNode.set("android", datosNode.get("android"));
        }

        // Construcción de bloque "apns" para notificaciones iOS
        ObjectNode apnsNode = datosNode.has("apns") && datosNode.get("apns").isObject()
                ? (ObjectNode) datosNode.get("apns")
                : mapper.createObjectNode();

        ObjectNode payloadNode = apnsNode.has("payload") && apnsNode.get("payload").isObject()
                ? (ObjectNode) apnsNode.get("payload")
                : mapper.createObjectNode();

        ObjectNode apsNode = payloadNode.has("aps") && payloadNode.get("aps").isObject()
                ? (ObjectNode) payloadNode.get("aps")
                : mapper.createObjectNode();

        // Si hay título o cuerpo, arma el objeto alert para APNs
        if (msg.getTitulo() != null || msg.getCuerpo() != null) {
            ObjectNode alertNode = mapper.createObjectNode();
            if (msg.getTitulo() != null) alertNode.put("title", msg.getTitulo());
            if (msg.getCuerpo() != null) alertNode.put("body", msg.getCuerpo());
            apsNode.set("alert", alertNode);
        }

        payloadNode.set("aps", apsNode);
        apnsNode.set("payload", payloadNode);
        messageNode.set("apns", apnsNode);

        // También incluye el bloque "notification" para compatibilidad visual en Android/iOS
        if (msg.getTitulo() != null || msg.getCuerpo() != null) {
            ObjectNode notificationNode = mapper.createObjectNode();
            if (msg.getTitulo() != null) notificationNode.put("title", msg.getTitulo());
            if (msg.getCuerpo() != null) notificationNode.put("body", msg.getCuerpo());
            messageNode.set("notification", notificationNode);
        }

        // Envolvemos todo dentro del bloque raíz "message"
        ObjectNode root = mapper.createObjectNode();
        root.set("message", messageNode);

        return mapper.writeValueAsString(root);
    }
}
