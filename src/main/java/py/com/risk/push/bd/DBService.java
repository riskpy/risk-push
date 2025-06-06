package py.com.risk.push.bd;

import oracle.jdbc.OracleTypes;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import py.com.risk.push.config.DataSourceConfig;
import py.com.risk.push.model.Plataforma;
import py.com.risk.push.model.PushMessage;
import py.com.risk.push.model.PushMessage.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Servicio de acceso a la base de datos para el manejo de notificaciones push.
 * 
 * Esta clase encapsula la lógica de conexión mediante HikariCP y las operaciones
 * de consulta y actualización de mensajes pendientes.
 * 
 * @author Damián Meza
 * @version 1.0.0
 */
public class DBService {

    private static final Logger logger = LogManager.getLogger(DBService.class);

    /** Fuente de datos HikariCP para manejo de conexiones */
    private final DataSource dataSource;

    /** Número máximo de intentos de envío antes de marcar el mensaje como rechazado */
    private Integer maximoIntentos = 5;

    /**
     * Consulta SQL para obtener notificaciones pendientes de envío.
     * Se filtra por estado, plataforma, clasificación y se limita por cantidad.
     */
    private static final String QUERY_OBTENER_NOTIFICACIONES_PENDIENTES = 
        "SELECT id_notificacion, token_notificacion, titulo, contenido, plataforma, datos_extra\r\n" +
        "  FROM t_notificaciones b\r\n" +
        "  JOIN t_mensajeria_categorias c\r\n" +
        "    ON b.id_categoria = c.id_categoria\r\n" +
        " WHERE b.estado = ?\r\n" +
        "   AND b.plataforma = nvl(?, b.plataforma)\r\n" +
        "   AND c.clasificacion = nvl(?, c.clasificacion)\r\n" +
        " ORDER BY NVL(c.prioridad, 997), b.id_notificacion\r\n" +
        " FETCH FIRST NVL(?, 100) ROWS ONLY";

    /**
     * Consulta SQL para actualizar el estado de envío de notificaciones masivamente.
     * Maneja lógica para verificar si puede actualizar los registros o están siendo usados por otro proceso.
     */
    private static final String QUERY_ACTUALIZAR_ESTADO_NOTIFICACIONES =
        "DECLARE\n" + 
        "  v_ids SYS.ODCINUMBERLIST := ?;\n" +
        "  v_estado VARCHAR2(1) := ?;\n" +
        "  v_no_bloqueados SYS.ODCINUMBERLIST := SYS.ODCINUMBERLIST();\n" +
        "  v_bloqueados SYS.ODCINUMBERLIST := SYS.ODCINUMBERLIST();\n" +
        "  v_bloqueado number;\n" +
        "BEGIN\n" +
        "  -- Intentar bloquear los registros\n" +
        "  FOR i IN 1 .. v_ids.COUNT LOOP\n" +
        "    BEGIN\n" +
        "      SELECT id_notificacion\n" +
        "        INTO v_bloqueado\n" +
        "        FROM t_notificaciones\n" +
        "       WHERE id_notificacion = v_ids(i)\n" +
        "         FOR UPDATE NOWAIT;\n" +
        "\n" +
        "      v_bloqueados.EXTEND;\n" +
        "      v_bloqueados(v_bloqueados.COUNT) := v_bloqueado;\n" +
        "    EXCEPTION\n" +
        "      WHEN OTHERS THEN\n" +
        "        IF SQLCODE = -54 THEN -- ORA-00054: recurso ocupado\n" +
        "          v_no_bloqueados.EXTEND;\n" +
        "          v_no_bloqueados(v_no_bloqueados.COUNT) := v_ids(i);\n" +
        "        ELSE\n" +
        "          RAISE;\n" +
        "        END IF;\n" +
        "    END;\n" +
        "  END LOOP;\n" +
        "\n" +
        "  -- Actualizar estado si el bloqueo fue exitoso\n" +
        "  UPDATE t_notificaciones\n" +
        "     SET estado = v_estado\n" +
        "   WHERE id_notificacion IN (SELECT * FROM TABLE(v_bloqueados));\n" +
        "\n" +
        "  -- Devolver los no bloqueados\n" +
        "  ? := v_no_bloqueados;\n" +
        "END;";

    /**
     * Consulta SQL para actualizar el estado de envío de una notificación.
     * Maneja lógica para incrementar intentos, marcar como rechazado, y registrar respuesta.
     */
    private static final String QUERY_ACTUALIZAR_NOTIFICACION_ENVIADA = 
        "UPDATE t_notificaciones SET " +
        "estado = CASE " +
        "  WHEN ? = 'P' AND cantidad_intentos_envio >= ? THEN 'R' " +
        "  ELSE NVL(?, estado) END, " +
        "codigo_respuesta_envio = NVL(SUBSTR(?, 1, 10), codigo_respuesta_envio), " +
        "respuesta_envio = NVL(SUBSTR(?, 1, 1000), respuesta_envio), " +
        "id_externo_envio = NVL(SUBSTR(?, 1, 100), id_externo_envio), " +
        "cantidad_intentos_envio = CASE WHEN ? = 'N' THEN NVL(cantidad_intentos_envio,0) " +
        "  ELSE NVL(cantidad_intentos_envio,0) + 1 END, " +
        "fecha_envio = CASE WHEN ? = 'E' THEN CURRENT_TIMESTAMP ELSE fecha_envio END " +
        "WHERE id_notificacion = ?";

    /**
     * Constructor que inicializa el pool de conexiones Hikari con la configuración recibida.
     *
     * @param ds configuración del origen de datos (JDBC, usuario, contraseña, etc.)
     */
    public DBService(DataSourceConfig ds) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(buildJdbcUrl(ds));
        config.setUsername(ds.getUser());
        config.setPassword(ds.getPassword());
        config.setMaximumPoolSize(ds.getMaximumPoolSize());
        config.setMinimumIdle(ds.getMinimumIdle());
        config.setIdleTimeout(ds.getIdleTimeout());
        config.setConnectionTimeout(ds.getConnectionTimeout());
        this.dataSource = new HikariDataSource(config);
    }

    /**
     * Construye la URL JDBC en base a la configuración recibida.
     *
     * @param ds objeto con los datos del servidor, puerto y servicio
     * @return cadena de conexión JDBC para Oracle
     */
    private String buildJdbcUrl(DataSourceConfig ds) {
        return String.format("jdbc:oracle:thin:@//%s:%d/%s", ds.getServerName(), ds.getPort(), ds.getServiceName());
    }

    /**
     * Establece el número máximo de intentos permitidos antes de marcar un mensaje como rechazado.
     *
     * @param max cantidad máxima de intentos
     */
    public void setMaximoIntentos(Integer max) {
        this.maximoIntentos = max;
    }

    /**
     * Recupera las notificaciones pendientes de envío desde la base de datos.
     *
     * @param plataforma código de la plataforma (FCM, HMS, etc.)
     * @param clasificacion clasificación opcional para filtrar categorías
     * @param maxSize número máximo de registros a recuperar
     * @return lista de mensajes pendientes representados como PushMessage
     * @throws SQLException en caso de error de conexión o consulta
     */
    public List<PushMessage> loadPendingPushMessages(String plataforma, String clasificacion, Integer maxSize) throws SQLException {
        logger.debug("Recuperando mensajes push pendientes: plataforma={}, clasificacion={}, max={}", plataforma, clasificacion, maxSize);
        List<PushMessage> lista = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(QUERY_OBTENER_NOTIFICACIONES_PENDIENTES)) {

            stmt.setString(1, Status.PENDIENTE_ENVIO.getCode());
            stmt.setString(2, plataforma);
            stmt.setString(3, clasificacion);
            stmt.setObject(4, maxSize, Types.INTEGER);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new PushMessage(
                            rs.getBigDecimal("id_notificacion"),
                            rs.getString("token_notificacion"),
                            rs.getString("titulo"),
                            rs.getString("contenido"),
                            Plataforma.fromCode(rs.getString("plataforma")),
                            rs.getString("datos_extra")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al recuperar mensajes pendientes", e);
        }

        return lista;
    }

    /**
     * Actualiza el estado de las notificaciones de forma masiva.
     *
     * @param mensajes lista de notificaciones
     * @param estado nuevo estado (P: pendiente, E: enviado, R: rechazado)
     */
    public void updateMessagesStatus(List<PushMessage> mensajes, Status estado) {
        if (mensajes == null || mensajes.isEmpty())
            return;
        logger.debug("Marcando [{}] mensajes como [{}]", mensajes.size(), estado);

        try (Connection conn = dataSource.getConnection()) {
            // Convertir a arreglo de BigDecimal
            BigDecimal[] ids = mensajes.stream()
                    .map(PushMessage::getIdMensaje)
                    .toArray(BigDecimal[]::new);

            // Desempaquetar la conexión real de Oracle
            oracle.jdbc.OracleConnection oraConn = conn.unwrap(oracle.jdbc.OracleConnection.class);

            // Crear el descriptor y el ARRAY
            ArrayDescriptor descriptor = ArrayDescriptor.createDescriptor("SYS.ODCINUMBERLIST", oraConn);
            ARRAY array = new ARRAY(descriptor, oraConn, ids);

            // Ejecutar el bloque anónimo PL/SQL
            try (CallableStatement stmt = conn.prepareCall(QUERY_ACTUALIZAR_ESTADO_NOTIFICACIONES)) {
                stmt.setArray(1, array); // v_ids
                stmt.setString(2, estado.getCode()); // v_estado
                stmt.registerOutParameter(3, OracleTypes.ARRAY, "SYS.ODCINUMBERLIST");
                stmt.execute();

                ARRAY resultArray = (ARRAY) stmt.getArray(3);
                if (resultArray != null) {
                    BigDecimal[] notUpdatedIds = (BigDecimal[]) resultArray.getArray();
                    Set<BigDecimal> noBloqueadosSet = new HashSet<>(Arrays.asList(notUpdatedIds));
                    logger.debug("Mensajes no bloqueados: [{}]", noBloqueadosSet);

                    // Excluir los no bloqueados de la lista original
                    mensajes.removeIf(m -> noBloqueadosSet.contains(m.getIdMensaje()));
                }
            }

        } catch (SQLException e) {
            if (e.getErrorCode() == 54) { // ORA-00054: recurso ocupado
                logger.warn("No se pudo bloquear los registros para actualizar estado de mensajes: están siendo usados por otro proceso.");
            } else {
                logger.error("Error al actualizar estado de mensajes", e);
            }
        } catch (Exception e) {
            logger.error("Fallo inesperado en actualización de estado de mensajes", e);
        }
    }

    /**
     * Actualiza el estado de un mensaje push luego del intento de envío.
     *
     * @param idMensaje ID del mensaje en la base de datos
     * @param estado nuevo estado (P: pendiente, E: enviado, R: rechazado)
     * @param codigo código de respuesta del proveedor
     * @param respuesta cuerpo de respuesta o mensaje de error
     * @param idExterno ID asignado por el proveedor (Firebase/Huawei)
     */
    public void updateMessageStatus(BigDecimal idMensaje, Status estado, String codigo, String respuesta, String idExterno) {
        logger.debug("Actualizando estado del mensaje id=[{}] a [{}]", idMensaje, estado);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(QUERY_ACTUALIZAR_NOTIFICACION_ENVIADA)) {

            stmt.setString(1, estado.getCode());
            stmt.setInt(2, maximoIntentos - 1);
            stmt.setString(3, estado.getCode());
            stmt.setString(4, codigo);
            stmt.setString(5, respuesta);
            stmt.setString(6, idExterno);
            stmt.setString(7, estado.getCode());
            stmt.setString(8, estado.getCode());
            stmt.setBigDecimal(9, idMensaje);

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al actualizar estado del mensaje id=[{}]", idMensaje, e);
        }
    }
}
