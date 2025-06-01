package py.com.risk.push.config;

/**
 * Configuración específica para el servicio de envío de notificaciones push mediante Firebase.
 * <p>
 * Esta clase encapsula únicamente la ruta del archivo de credenciales del Service Account de Firebase.
 * Se utiliza para inicializar el SDK de Firebase Admin en el sistema.
 * </p>
 * 
 * @author Damián Meza
 * @version 1.0.0
 */
public class FirebaseConfig {

    /** Ruta absoluta o relativa al archivo JSON del Service Account de Firebase */
    private String serviceAccountPath;

    /**
     * Obtiene la ruta del archivo de credenciales de Firebase.
     *
     * @return ruta al archivo JSON del Service Account
     */
    public String getServiceAccountPath() {
        return serviceAccountPath;
    }

    /**
     * Define la ruta al archivo JSON del Service Account de Firebase.
     *
     * @param serviceAccountPath ruta al archivo de credenciales
     */
    public void setServiceAccountPath(String serviceAccountPath) {
        this.serviceAccountPath = serviceAccountPath;
    }
}
