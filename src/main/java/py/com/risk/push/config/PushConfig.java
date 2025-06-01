package py.com.risk.push.config;

import py.com.risk.push.model.ModoEnvioLote;
import py.com.risk.push.model.Plataforma;

/**
 * Configuración individual de cada servicio de envío de notificaciones push.
 * 
 * <p>Define todos los parámetros necesarios para controlar el comportamiento del envío,
 * como la plataforma (Firebase, Huawei), tipo de clasificación, cantidad de mensajes por lote,
 * modalidad de envío y configuraciones específicas del proveedor.</p>
 * 
 * <p>Esta clase representa cada entrada en la lista <code>push:</code> del archivo de configuración <code>risk-push.yml</code>.</p>
 * 
 * @author Damián Meza
 * @version 1.0.0
 */
public class PushConfig {

    /**
     * Nombre identificador del servicio push.
     * Generalmente usado para diferenciar la configuración por cliente, canal o proveedor.
     */
    private String nombre;

    /**
     * Plataforma de envío configurada: FCM (Firebase) o HMS (Huawei).
     */
    private Plataforma plataforma;

    /**
     * Clasificación del mensaje: puede ser "ALERTA", "AVISO", "PROMOCION", etc.
     * Esta clasificación se utiliza como filtro al consultar mensajes pendientes en la base de datos.
     */
    private String clasificacion;

    /**
     * Cantidad máxima de mensajes a enviar en cada lote. Si no se especifica, se usa el valor por defecto de 100.
     */
    private Integer cantidadMaximaPorLote;

    /**
     * Modo de envío del lote: puede ser paralelo, secuencial, con o sin espera entre mensajes.
     * Por defecto, se utiliza {@link ModoEnvioLote#secuencial_espaciado}.
     */
    private ModoEnvioLote modoEnvioLote;

    /**
     * Tiempo de espera (en milisegundos) entre cada lote de mensajes. Valor por defecto: 10.000 ms.
     */
    private Long intervaloEntreLotesMs;

    /**
     * Número máximo de intentos de reenvío por mensaje antes de marcarlo como rechazado. Por defecto: 5.
     */
    private Integer maximoIntentos;

    /**
     * Configuración específica para Firebase Cloud Messaging.
     */
    private FirebaseConfig firebase;

    /**
     * Configuración específica para Huawei Push Kit.
     */
    private HuaweiConfig huawei;

    // Getters y Setters

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Plataforma getPlataforma() {
        return plataforma;
    }

    public void setPlataforma(Plataforma plataforma) {
        this.plataforma = plataforma;
    }

    public String getClasificacion() {
        return clasificacion;
    }

    public void setClasificacion(String clasificacion) {
        this.clasificacion = clasificacion;
    }

    public Integer getCantidadMaximaPorLote() {
        return cantidadMaximaPorLote != null ? cantidadMaximaPorLote : 100;
    }

    public void setCantidadMaximaPorLote(Integer cantidadMaximaPorLote) {
        this.cantidadMaximaPorLote = cantidadMaximaPorLote;
    }

    public ModoEnvioLote getModoEnvioLote() {
        return modoEnvioLote != null ? modoEnvioLote : ModoEnvioLote.secuencial_espaciado;
    }

    public void setModoEnvioLote(ModoEnvioLote modoEnvioLote) {
        this.modoEnvioLote = modoEnvioLote;
    }

    public Long getIntervaloEntreLotesMs() {
        return intervaloEntreLotesMs != null ? intervaloEntreLotesMs : 10000L;
    }

    public void setIntervaloEntreLotesMs(Long intervaloEntreLotesMs) {
        this.intervaloEntreLotesMs = intervaloEntreLotesMs;
    }

    public Integer getMaximoIntentos() {
        return maximoIntentos != null ? maximoIntentos : 5;
    }

    public void setMaximoIntentos(Integer maximoIntentos) {
        this.maximoIntentos = maximoIntentos;
    }

    public FirebaseConfig getFirebase() {
        return firebase != null ? firebase : new FirebaseConfig();
    }

    public void setFirebase(FirebaseConfig firebase) {
        this.firebase = firebase;
    }

    public HuaweiConfig getHuawei() {
        return huawei != null ? huawei : new HuaweiConfig();
    }

    public void setHuawei(HuaweiConfig huawei) {
        this.huawei = huawei;
    }
}
