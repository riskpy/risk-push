package py.com.risk.push.model;

/**
 * Enum que representa las plataformas de mensajería push soportadas por el sistema.
 * Actualmente se contemplan:
 * <ul>
 *     <li>{@code FCM} - Firebase Cloud Messaging</li>
 *     <li>{@code HMS} - Huawei Mobile Services (Push Kit)</li>
 * </ul>
 * 
 * @author Damián Meza
 * @version 1.0.0
 */
public enum Plataforma {
    /** Plataforma Firebase Cloud Messaging */
    FCM,

    /** Plataforma Huawei Mobile Services */
    HMS;

    /**
     * Obtiene una instancia de {@code Plataforma} a partir de su nombre en texto.
     *
     * @param code Cadena de texto que representa el nombre de la plataforma (no sensible a mayúsculas)
     * @return Valor correspondiente de {@code Plataforma}, o {@code null} si no existe coincidencia
     */
    public static Plataforma fromCode(String code) {
        for (Plataforma p : values()) {
            if (p.name().equalsIgnoreCase(code)) return p;
        }
        return null;
    }
}
