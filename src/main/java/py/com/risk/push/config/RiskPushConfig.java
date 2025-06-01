package py.com.risk.push.config;

import java.util.List;

/**
 * Representa la configuración principal del sistema de envío de notificaciones push.
 * 
 * <p>Es la clase raíz que se mapea desde el archivo <code>risk-push.yml</code>, 
 * agrupando la configuración de la base de datos y de los servicios individuales de envío push.</p>
 * 
 * <pre>
 * Ejemplo en YAML:
 * 
 * datasource:
 *   serverName: db-server
 *   port: 1521
 *   serviceName: orclpdb
 *   user: user
 *   password: pass
 * 
 * push:
 *   - nombre: canalFirebase
 *     plataforma: FCM
 *     firebase:
 *       serviceAccountPath: path/to/cred.json
 *   - nombre: canalHuawei
 *     plataforma: HMS
 *     huawei:
 *       appId: ...
 * </pre>
 * 
 * @author Damián Meza
 * @version 1.0.0
 */
public class RiskPushConfig {

    /**
     * Configuración de la fuente de datos (base de datos Oracle).
     */
    private DataSourceConfig datasource;

    /**
     * Lista de configuraciones de servicios de envío push. 
     * Cada entrada representa un canal o proveedor configurado.
     */
    private List<PushConfig> push;

    /**
     * Devuelve la configuración del datasource.
     * Si es nula, retorna una instancia por defecto.
     *
     * @return configuración del origen de datos
     */
    public DataSourceConfig getDatasource() {
        return datasource != null ? datasource : new DataSourceConfig();
    }

    /**
     * Establece la configuración del datasource.
     *
     * @param datasource configuración del origen de datos
     */
    public void setDatasource(DataSourceConfig datasource) {
        this.datasource = datasource;
    }

    /**
     * Devuelve la lista de configuraciones de envío push.
     *
     * @return lista de configuraciones
     */
    public List<PushConfig> getPush() {
        return push;
    }

    /**
     * Establece la lista de configuraciones de envío push.
     *
     * @param push lista de configuraciones
     */
    public void setPush(List<PushConfig> push) {
        this.push = push;
    }
}

