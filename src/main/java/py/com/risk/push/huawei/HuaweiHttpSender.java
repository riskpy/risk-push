package py.com.risk.push.huawei;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Clase encargada de realizar el envío HTTP de notificaciones push hacia el servicio Huawei Push Kit.
 * <p>
 * Esta clase utiliza un token de acceso generado por {@link HuaweiTokenManager} para autorizar
 * el envío de mensajes y realiza una conexión HTTP POST con cuerpo JSON.
 * 
 * @author Damián Meza
 * @version 1.0.0
 */
public class HuaweiHttpSender {

    private static final Logger logger = LogManager.getLogger(HuaweiHttpSender.class);

    private final HuaweiTokenManager tokenManager;
    private final String apiUrl;

    /**
     * Constructor que recibe las dependencias necesarias para el envío.
     *
     * @param tokenManager Administrador de tokens de acceso OAuth 2.0 para Huawei
     * @param apiUrl       URL del endpoint de envío de notificaciones de Huawei (sin token)
     */
    public HuaweiHttpSender(HuaweiTokenManager tokenManager, String apiUrl) {
        this.tokenManager = tokenManager;
        this.apiUrl = apiUrl;
    }

    /**
     * Realiza el envío de un mensaje push a Huawei usando HTTP POST.
     *
     * @param jsonBody Cuerpo JSON completo del mensaje a enviar
     * @return Cadena con la respuesta del servidor Huawei (normalmente un JSON)
     * @throws Exception si ocurre un error de red, autenticación o conexión
     */
    public String sendToHuawei(String jsonBody) throws Exception {
        String accessToken = tokenManager.getAccessToken();
        String url = apiUrl + "?access_token=" + accessToken;

        logger.debug("Enviando push a Huawei: {}", jsonBody);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        InputStream is = responseCode >= 200 && responseCode < 300
                ? conn.getInputStream()
                : conn.getErrorStream();

        String response = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));

        logger.info("Respuesta Huawei: {}", response);
        conn.disconnect();

        return response;
    }
}
