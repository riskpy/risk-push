package py.com.risk.push.model;

import java.math.BigDecimal;

/**
 * Representa un mensaje push pendiente o en proceso de ser enviado a una plataforma específica
 * como Firebase o Huawei. Contiene todos los datos necesarios para construir y enviar la notificación.
 * 
 * <p>Esta clase es utilizada principalmente por el motor de envío para transportar los datos desde
 * la base de datos hasta el cliente de envío correspondiente.</p>
 * 
 * <p>Puede incluir un contenido enriquecido en formato JSON dentro del campo {@code datosExtra},
 * el cual se interpreta según la plataforma destino.</p>
 * 
 * @author Damián Meza
 * @version 1.0.0
 */
public class PushMessage {

    /** ID único del mensaje en la base de datos. */
    private final BigDecimal idMensaje;

    /** Token del dispositivo receptor de la notificación. */
    private final String token;

    /** Título visible de la notificación. */
    private final String titulo;

    /** Texto principal o cuerpo del mensaje de la notificación. */
    private final String cuerpo;

    /** Plataforma destino del mensaje (Firebase, Huawei, etc.). */
    private final Plataforma plataforma;

    /** Datos adicionales en formato JSON que complementan la notificación. */
    private final String datosExtra;

    /**
     * Constructor para inicializar un mensaje push.
     *
     * @param idMensaje    ID único del mensaje.
     * @param token        Token del dispositivo destinatario.
     * @param titulo       Título de la notificación.
     * @param cuerpo       Cuerpo del mensaje.
     * @param plataforma   Plataforma de destino (FCM, HMS, etc.).
     * @param datosExtra   JSON con datos adicionales personalizados para el envío.
     */
    public PushMessage(BigDecimal idMensaje, String token, String titulo, String cuerpo, Plataforma plataforma, String datosExtra) {
        this.idMensaje = idMensaje;
        this.token = token;
        this.titulo = titulo;
        this.cuerpo = cuerpo;
        this.plataforma = plataforma;
        this.datosExtra = datosExtra;
    }

    public BigDecimal getIdMensaje() {
        return idMensaje;
    }

    public String getToken() {
        return token;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public Plataforma getPlataforma() {
        return plataforma;
    }

    public String getDatosExtra() {
        return datosExtra;
    }

    /**
     * Enum que representa los estados posibles del mensaje push dentro del sistema.
     * Utilizado para controlar el ciclo de vida de cada mensaje.
     */
    public enum Status {
        /** El mensaje aún no fue procesado. */
        PENDIENTE_ENVIO("P", "Pendiente de envío"),

        /** El mensaje está siendo procesado o encolado. */
        EN_PROCESO_ENVIO("N", "En proceso de envío"),

        /** El mensaje fue enviado correctamente. */
        ENVIADO("E", "Enviado"),

        /** Se produjo un error durante el envío y fue marcado como rechazado. */
        PROCESADO_ERROR("R", "Error al procesar"),

        /** El mensaje fue anulado manualmente. */
        ANULADO("A", "Anulado");

        private final String code;
        private final String description;

        Status(String code, String description) {
            this.code = code;
            this.description = description;
        }

        /**
         * Retorna el código corto del estado.
         *
         * @return Código de una sola letra (por ejemplo, "P", "E", etc.).
         */
        public String getCode() {
            return code;
        }

        /**
         * Retorna la descripción legible del estado.
         *
         * @return Texto descriptivo del estado.
         */
        public String getDescription() {
            return description;
        }

        /**
         * Retorna el estado correspondiente al código recibido.
         *
         * @param value Código del estado.
         * @return Instancia de {@link Status} o {@code null} si no coincide.
         */
        public static Status fromCode(String value) {
            for (Status status : Status.values()) {
                if (status.code.equals(value)) {
                    return status;
                }
            }
            return null;
        }
    }
}
