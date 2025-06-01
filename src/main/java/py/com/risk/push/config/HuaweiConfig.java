package py.com.risk.push.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuración específica para el servicio de envío de notificaciones push mediante Huawei Push Kit.
 * <p>
 * Esta clase contiene los parámetros requeridos para autenticar y enviar notificaciones a dispositivos Huawei,
 * utilizando el flujo de autenticación OAuth2 y el endpoint de mensajería HTTP.
 * </p>
 *
 * <p>Los campos de configuración deben mapearse desde el archivo {@code risk-push.yml} para cada servicio definido.</p>
 * 
 * @author Damián Meza
 * @version 1.0.0
 */
@Getter
@Setter
public class HuaweiConfig {

    /**
     * Identificador de la aplicación registrado en la consola de Huawei Developer (App ID).
     */
    private String appId;

    /**
     * Secreto de cliente asociado al App ID, utilizado para obtener el token OAuth2.
     */
    private String appSecret;

    /**
     * URL del endpoint de autenticación de Huawei para obtener el access token.
     * Por ejemplo: {@code https://oauth-login.cloud.huawei.com/oauth2/v3/token}
     */
    private String tokenUrl;

    /**
     * URL del endpoint principal de envío de notificaciones.
     * Por ejemplo: {@code https://push-api.cloud.huawei.com/v1/{appId}/messages:send}
     */
    private String apiUrl;
}
