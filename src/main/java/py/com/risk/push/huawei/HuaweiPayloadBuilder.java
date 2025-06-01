package py.com.risk.push.huawei;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import py.com.risk.push.model.PushMessage;

/**
 * Clase utilitaria que construye el cuerpo JSON para el envío de mensajes push a través de Huawei Push Kit.
 * <p>
 * Utiliza la estructura requerida por la API de Huawei, incluyendo campos como "validate_only",
 * "token", "notification", "data" y configuraciones específicas de Android si están presentes.
 * 
 * @author Damián Meza
 * @version 1.0.0
 */
public class HuaweiPayloadBuilder {

    /**
     * Construye el JSON requerido para enviar una notificación a Huawei.
     *
     * @param msg Objeto {@link PushMessage} con los datos del mensaje a enviar
     * @return Cadena JSON compatible con el formato exigido por Huawei Push Kit
     * @throws Exception si ocurre un error al serializar los datos o convertir los campos extra
     */
    public static String buildJsonPayload(PushMessage msg) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();
        ObjectNode message = mapper.createObjectNode();

        // Campo obligatorio por la API de Huawei
        message.put("validate_only", false);

        ObjectNode msgNode = mapper.createObjectNode();

        // Token del destinatario como array (obligatorio por especificación de Huawei)
        msgNode.set("token", mapper.createArrayNode().add(msg.getToken()));

        // Nodo de notificación (título y cuerpo)
        ObjectNode notification = mapper.createObjectNode();
        if (msg.getTitulo() != null) notification.put("title", msg.getTitulo());
        if (msg.getCuerpo() != null) notification.put("body", msg.getCuerpo());
        msgNode.set("notification", notification);

        // Datos extra opcionales: data y configuración específica de Android
        if (msg.getDatosExtra() != null && !msg.getDatosExtra().isEmpty()) {
            JsonNode extras = mapper.readTree(msg.getDatosExtra());
            if (extras.has("data")) msgNode.set("data", extras.get("data"));
            if (extras.has("android")) msgNode.set("android", extras.get("android"));
        }

        message.set("message", msgNode);
        root.set("message", message);

        return mapper.writeValueAsString(root);
    }
}
