package py.com.risk.push.huawei;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Clase encargada de gestionar el token de acceso (OAuth2) necesario para enviar
 * notificaciones push mediante Huawei Push Kit.
 * <p>
 * Se encarga de renovar automáticamente el token cuando ha expirado.
 * 
 * @author Damián Meza
 * @version 1.0.0
 */
public class HuaweiTokenManager {

    private static final Logger logger = LogManager.getLogger(HuaweiTokenManager.class);

    private final String clientId;
    private final String clientSecret;
    private final String tokenUrl;

    /** Token de acceso actual */
    private String accessToken;

    /** Fecha y hora de expiración del token */
    private Instant expirationTime;

    /** Lock para garantizar que solo un hilo renueve el token a la vez */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Constructor que recibe las credenciales necesarias y la URL del servicio de autenticación de Huawei.
     *
     * @param clientId     ID de cliente proporcionado por Huawei
     * @param clientSecret Secreto del cliente
     * @param tokenUrl     URL del endpoint de autenticación
     */
    public HuaweiTokenManager(String clientId, String clientSecret, String tokenUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUrl = tokenUrl;
    }

    /**
     * Devuelve el token de acceso actual. Si ha expirado o no existe, lo renueva automáticamente.
     *
     * @return Token de acceso válido
     */
    public String getAccessToken() {
        if (accessToken == null || Instant.now().isAfter(expirationTime)) {
            refreshToken();
        }
        return accessToken;
    }

    /**
     * Solicita un nuevo token de acceso al endpoint de autenticación de Huawei
     * y actualiza los valores de `accessToken` y `expirationTime`.
     */
    private void refreshToken() {
        lock.lock();
        try {
            logger.debug("Renovando token Huawei...");

            String params = "grant_type=client_credentials"
                    + "&client_id=" + clientId
                    + "&client_secret=" + clientSecret;

            URL url = new URL(tokenUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(params.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            InputStream is = responseCode >= 200 && responseCode < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            String response = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines().collect(java.util.stream.Collectors.joining("\n"));

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);

            this.accessToken = root.get("access_token").asText();
            int expiresIn = root.get("expires_in").asInt();

            // Se resta 60 segundos como margen de seguridad
            this.expirationTime = Instant.now().plusSeconds(expiresIn - 60);

            logger.info("Token Huawei renovado correctamente.");

        } catch (Exception e) {
            logger.error("Error al renovar token Huawei: " + e.getMessage(), e);
            throw new RuntimeException("No se pudo obtener el token Huawei", e);
        } finally {
            lock.unlock();
        }
    }
}
