
# Risk Push Service

**`risk-push`** es una herramienta desarrollada en Java para el **envío de notificaciones push móviles**, diseñada como **parte del ecosistema [risk](https://github.com/riskpy/risk)** e integrada con su módulo de _Mensajería_.

Este servicio permite el envío de mensajes push a dispositivos Android/iOS utilizando **Firebase** y/o **Huawei Push Kit**, siendo ideal para diferentes tipos de negocios.

---

## 🚀 Funcionalidades principales

* 📤 Envío de notificaciones push a través de **múltiples plataformas** (Firebase FCM y Huawei HMS).
* 🗃️ Lectura de mensajes pendientes desde base de datos Oracle.
* 📡 Soporte para personalización avanzada de payloads según proveedor.
* 🧩 Configuración desacoplada mediante archivo YAML externo.

---

## 🧱 Requisitos

- Java 17 o superior
- Maven 3.6+
- Archivo de credenciales para Firebase (`.json`)
- Acceso a la API de Huawei Push
- Base de datos Oracle con la tabla:
    - `t_notificaciones`: contiene los mensajes pendientes de envío.

---

## 📦 Estructura del proyecto

```
risk-push/
├── config/
│   ├── risk-push.yml.example     # Archivo de configuración ejemplo
│   └── risk-push.yml             # Archivo real (no versionado)
├── src/                          # Código fuente
├── target/                       # Archivos compilados y .jar
├── pom.xml                       # Configuración Maven
├── LICENSE
└── README.md
```

---

## ⚙️ Configuración

El archivo `risk-push.yml` contiene los parámetros necesarios para la conexión a base de datos y configuración por cada proveedor push (Firebase, Huawei).

```bash
cp config/risk-push.yml.example config/risk-push.yml
```

Ejemplo de configuración:

```yaml
# Conexion a la base de datos
datasource:
  serverName: serverName.com.py
  port: 1521
  serviceName: serviceName
  user: user
  password: password
  maximumPoolSize: 50
  minimumIdle: 5
  idleTimeout: 30000
  connectionTimeout: 10000

# Configuración de servicios de mensajería push
push:
  - nombre: FCM-OTP
    plataforma: FCM                      # Plataforma: FCM, HMS.
    clasificacion: OTP                   # Clasificacion: OTP, AVISO, PROMOCION (u otros). Opcional
    cantidadMaximaPorLote: 100           # Cantidad maxima de Push a enviar por lote. Opcional. Por defecto 100
    modoEnvioLote: secuencial_espaciado  # Modo de envío: paralelo, paralelo_espaciado, secuencial_espaciado, secuencial_espaciado_async
    intervaloEntreLotesMs: 1000          # Tiempo de espera entre lotes de Push a enviar (en milisegundos)
    maximoIntentos: 3                    # Número máximo de intentos de envío permitidos de Push. Opcional. Por defecto 5
    # Conexion al servicio de Firebase Cloud Messaging
    firebase:
      serviceAccountPath: ./path/a/firebase-service-account.json

  - nombre: HUAWEI-AVISO
    plataforma: HMS
    clasificacion: AVISO
    cantidadMaximaPorLote: 100
    modoEnvioLote: secuencial_espaciado
    intervaloEntreLotesMs: 10000
    maximoIntentos: 5
    # Conexion al servicio de Huawei Mobile Services
    huawei:
      appId: tu-app-id
      appSecret: tu-app-secret
      tokenUrl: https://oauth-login.cloud.huawei.com/oauth2/v3/token
      apiUrl: https://push-api.cloud.huawei.com/v1/tu-app-id/messages:send
```

> ⚠️ **Importante:** No subas el archivo `risk-push.yml` real al repositorio. Usá solo `risk-push.yml.example`.

---

## 🛠️ Compilación

```bash
mvn clean install
```

El JAR generado estará en:

```
target/risk-push.jar
```

---


## ▶️ Ejecución
Para ejecutar el JAR:
```bash
java -Xms500M -Xmx500M -XX:MaxDirectMemorySize=250M -server -XX:+UseG1GC -XX:+ExplicitGCInvokesConcurrent -XX:MaxGCPauseMillis=500 -jar target/risk-push.jar
```
Por defecto, busca el archivo `config/risk-push.yml`.

También podés especificar otro archivo:

```bash
java -Xms500M -Xmx500M -XX:MaxDirectMemorySize=250M -server -XX:+UseG1GC -XX:+ExplicitGCInvokesConcurrent -XX:MaxGCPauseMillis=500 -jar target/risk-push.jar path/a/otro-risk-push.yml
```
---

## 🧪 Testing
Podés agregar mensajes de prueba en la tabla `t_notificaciones` de tu base de datos y verificar que se procesen correctamente, en cuanto al envío.

---

## 🪵 Logging

* Se utiliza **Log4j2** para los logs.
* La configuración puede personalizarse en el archivo `log4j2.xml`.

---

## 🙋‍♂️ Sugerencias

¿Encontraste un error o querés proponer una mejora?  
Abrí un [issue](https://github.com/riskpy/risk-push/issues) para comentarlo.

---

## 🤝 Contribuciones

Las contribuciones son bienvenidas:
1. Creá una rama con tus cambios.
2. Enviá un pull request explicando qué mejoras hiciste.

¡Gracias por contribuir a **`risk-push`**!

---

## 📄 Licencia

Este proyecto está bajo la licencia MIT. Ver el archivo [LICENSE](/LICENSE).

MIT © 2025 – [DamyGenius](https://github.com/DamyGenius)
