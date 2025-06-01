package py.com.risk.push.model;

/**
 * Interfaz funcional que representa un cliente genérico para el envío de mensajes push.
 * <p>
 * Esta interfaz permite abstraer el mecanismo de envío para distintas plataformas (Firebase, Huawei, etc.),
 * de manera que se puedan intercambiar dinámicamente las implementaciones según la plataforma destino.
 * </p>
 *
 * <p>Ejemplos de implementaciones: {@code FirebasePushClient}, {@code HuaweiPushClient}</p>
 * 
 * @author Damián Meza
 * @version 1.0.0
 */
public interface PushClient {

    /**
     * Envía un mensaje push a través del cliente específico.
     *
     * @param msg Objeto {@link PushMessage} que contiene los datos del mensaje a enviar.
     * @return Una cadena con la respuesta del proveedor (por ejemplo, el ID del mensaje enviado).
     * @throws Exception si ocurre un error durante el envío del mensaje (por red, autenticación, formato, etc.).
     */
    String send(PushMessage msg) throws Exception;
}
