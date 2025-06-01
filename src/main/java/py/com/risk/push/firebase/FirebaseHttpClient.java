package py.com.risk.push.firebase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Cliente HTTP personalizado para enviar notificaciones push a Firebase Cloud Messaging (FCM),
 * usando directamente el endpoint HTTP v1 de la API REST de Firebase.
 *
 * <p>Este cliente utiliza el archivo de credenciales del servicio para autenticar las solicitudes
 * y generar un token de acceso válido.</p>
 *
 * <p>Recomendado para cargas complejas o mensajes que requieren mayor control del payload.</p>
 * 
 * @author Damián Meza
 * @version 1.0.0
 */
public class FirebaseHttpClient {

    private static final Logger logger = LogManager.getLogger(FirebaseHttpClient.class);

    private static final String FCM_ENDPOINT = "https://fcm.googleapis.com/v1/projects/%s/messages:send";

    private final String projectId;
    private final String serviceAccountPath;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String accessToken;
    private long tokenExpirationTimeMillis;

    /**
     * Crea una instancia del cliente HTTP de Firebase.
     *
     * @param projectId ID del proyecto de Firebase (se encuentra en el archivo JSON de credenciales)
     * @param serviceAccountPath Ruta al archivo de credenciales JSON del servicio
     */
    public FirebaseHttpClient(String projectId, String serviceAccountPath) {
        this.projectId = projectId;
        this.serviceAccountPath = serviceAccountPath;
    }

    /**
     * Envía un mensaje push a Firebase utilizando HTTP.
     *
     * @param jsonBody Cuerpo del mensaje en formato JSON (completo y válido según la API de FCM)
     * @return El ID del mensaje enviado si fue exitoso (por ejemplo, "projects/XYZ/messages/123")
     * @throws Exception si ocurre un error durante el envío o autenticación
     */
    public String sendPush(String jsonBody) throws Exception {
        refreshAccessTokenIfNeeded();

        String endpoint = String.format(FCM_ENDPOINT, projectId);
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Content-Type", "application/json; UTF-8");

        logger.debug("Sending JSON: " + jsonBody);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("UTF-8"));
        }

        int responseCode = conn.getResponseCode();
        Scanner scanner;

        if (responseCode >= 200 && responseCode < 300) {
            scanner = new Scanner(conn.getInputStream(), "UTF-8");
        } else {
            scanner = new Scanner(conn.getErrorStream(), "UTF-8");
        }

        StringBuilder response = new StringBuilder();
        while (scanner.hasNextLine()) {
            response.append(scanner.nextLine());
        }

        scanner.close();
        conn.disconnect();

        if (responseCode >= 200 && responseCode < 300) {
            JsonNode jsonResponse = objectMapper.readTree(response.toString());
            return jsonResponse.has("name") ? jsonResponse.get("name").asText() : null;
        } else {
            JsonNode jsonResponse = objectMapper.readTree(response.toString());
            String message = jsonResponse.has("error") && jsonResponse.get("error").has("message")
                    ? jsonResponse.get("error").get("message").asText()
                    : "Respuesta desconocida de Firebase.";
            throw new RuntimeException("Error Firebase HTTP: " + message);
        }
    }

    /**
     * Refresca el token de acceso si no existe o si está próximo a expirar.
     *
     * @throws Exception si no se puede leer el archivo de credenciales o generar el token
     */
    private synchronized void refreshAccessTokenIfNeeded() throws Exception {
        long now = System.currentTimeMillis();
        if (accessToken == null || now >= tokenExpirationTimeMillis - 5 * 60 * 1000) { // 5 minutos de margen
            try (FileInputStream serviceAccount = new FileInputStream(serviceAccountPath)) {
                GoogleCredentials credentials = GoogleCredentials
                    .fromStream(serviceAccount)
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"));

                credentials.refreshIfExpired();
                AccessToken token = credentials.getAccessToken();
                this.accessToken = token.getTokenValue();
                this.tokenExpirationTimeMillis = token.getExpirationTime().getTime();
            }
        }
    }
}
