package py.com.risk.push.config;

/**
 * Clase de configuración para la fuente de datos utilizada por el sistema.
 * <p>
 * Define los parámetros necesarios para establecer conexiones a la base de datos Oracle
 * utilizando un pool de conexiones HikariCP.
 * </p>
 * 
 * @author Damián Meza
 * @version 1.0.0
 */
public class DataSourceConfig {

    /** Nombre del servidor o IP donde se encuentra la base de datos */
    private String serverName;

    /** Puerto de conexión a la base de datos */
    private Integer port;

    /** SID de la base de datos (no utilizado en esta implementación, opcional) */
    private String sid;

    /** Nombre del servicio de Oracle (service name) */
    private String serviceName;

    /** Usuario de base de datos */
    private String user;

    /** Contraseña del usuario de base de datos */
    private String password;

    /** Máximo número de conexiones simultáneas en el pool */
    private Integer maximumPoolSize;

    /** Número mínimo de conexiones ociosas en el pool */
    private Integer minimumIdle;

    /** Tiempo máximo en milisegundos que una conexión puede permanecer ociosa antes de cerrarse */
    private Long idleTimeout;

    /** Tiempo máximo en milisegundos que se espera para obtener una conexión del pool */
    private Long connectionTimeout;

    /**
     * Construye la URL JDBC para conexión a Oracle usando los parámetros configurados.
     *
     * @return URL JDBC en formato `jdbc:oracle:thin:@//host:port/service`
     */
    public String getJdbcUrl() {
        return String.format("jdbc:oracle:thin:@//%s:%s/%s", serverName, port, serviceName);
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Devuelve el número máximo de conexiones simultáneas permitidas en el pool.
     * 
     * @return valor configurado o 50 si es nulo
     */
    public Integer getMaximumPoolSize() {
        return maximumPoolSize != null ? maximumPoolSize : 50;
    }

    public void setMaximumPoolSize(Integer maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    /**
     * Devuelve el número mínimo de conexiones ociosas en el pool.
     * 
     * @return valor configurado o 5 si es nulo
     */
    public Integer getMinimumIdle() {
        return minimumIdle != null ? minimumIdle : 5;
    }

    public void setMinimumIdle(Integer minimumIdle) {
        this.minimumIdle = minimumIdle;
    }

    /**
     * Devuelve el tiempo máximo de inactividad de una conexión en milisegundos.
     *
     * @return valor configurado o 30000ms si es nulo
     */
    public Long getIdleTimeout() {
        return idleTimeout != null ? idleTimeout : 30000L;
    }

    public void setIdleTimeout(Long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    /**
     * Devuelve el tiempo máximo de espera para obtener una conexión en milisegundos.
     *
     * @return valor configurado o 10000ms si es nulo
     */
    public Long getConnectionTimeout() {
        return connectionTimeout != null ? connectionTimeout : 10000L;
    }

    public void setConnectionTimeout(Long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
}
