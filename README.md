# ms-rutaexpress-catalog

Microservicio de catálogo de RutaExpress. Administra servicios de envío, tarifas,
capacidad disponible y estado activo/inactivo mediante Oracle.

Java 21, Spring Boot 4.1.1, Maven. Coordenadas:
`cl.rutaexpress:ms-rutaexpress-catalog:0.0.1-SNAPSHOT`.
Paquete base: `cl.rutaexpress.catalog`. Puerto local: **8082**.

## Estructura

```text
src/main/java/cl/rutaexpress/catalog/
├── CatalogApplication.java
├── controller/ServicioController.java
├── dto/ServicioRequest.java
├── dto/ServicioResponse.java
├── entity/Servicio.java
├── exception/GlobalExceptionHandler.java
├── exception/ResourceNotFoundException.java
├── repository/ServicioRepository.java
├── security/SecurityConfig.java
├── service/ServicioService.java
└── service/ServicioServiceImpl.java

src/test/java/cl/rutaexpress/catalog/
├── controller/ServicioControllerTests.java
└── service/ServicioServiceImplTests.java
```

El controller valida solicitudes y delega al servicio. El servicio contiene las
operaciones transaccionales y la conversión a DTO. El repositorio maneja JPA.
Las respuestas no exponen entidades.

La estructura inicial usaba `com.rutaexpress.ms_rutaexpress_catalog`; se alineó con
el paquete y grupo solicitados para RutaExpress. Se mantienen artifact, versión del
proyecto, Java, Spring Boot y dependencias originales. Se añadieron los starters
`spring-boot-starter-security` y
`spring-boot-starter-security-oauth2-resource-server`.

## Variables de entorno

| Variable | Uso / formato de ejemplo |
| --- | --- |
| `DB_URL` | URL JDBC Oracle: `jdbc:oracle:thin:@//<HOST>:1521/<SERVICE_NAME>` |
| `DB_USERNAME` | Usuario del esquema de Catalog |
| `DB_PASSWORD` | Contraseña del usuario Oracle |
| `AZURE_ISSUER_URI` | Emisor: `https://login.microsoftonline.com/<TENANT_ID>/v2.0` |
| `AZURE_AUDIENCE` | Audiencia de esta API; debe coincidir con el claim `aud` del access token |

Los valores entre `<...>` son marcadores, no configuraciones utilizables.
Configura las variables en la terminal o en la configuración de ejecución del IDE.
Spring Boot no carga archivos `.env` automáticamente.

El esquema Oracle debe permitir conexiones, operaciones sobre `SERVICIOS` y,
para `ddl-auto=update`, crear/actualizar la tabla y crear la secuencia
`SERVICIOS_SEQ` (además de tener cuota de almacenamiento suficiente).
El ID se genera con una secuencia JPA de incremento 1. Hibernate detecta el
dialecto mediante los metadatos de Oracle; el driver es `oracle.jdbc.OracleDriver`.

## Ejecutar

### Conexión Oracle con Wallet

El proyecto incluye `oraclepki` junto con `ojdbc17`, ambos con la versión
administrada por Spring Boot. Para esta familia de driver (23), Oracle Wallet
requiere `oraclepki`; no se añaden las bibliotecas antiguas `osdt_core` y `osdt_cert`.

1. Descomprime el wallet en una carpeta fuera del repositorio, por ejemplo
   `C:/Oracle/Wallet_RutaExpress`. No copies el wallet a `src/main/resources`.
2. Busca el alias de conexión en el archivo `tnsnames.ora` de esa carpeta.
3. Configura `DB_URL` con este formato (reemplaza el alias y la ruta):

   ```text
   jdbc:oracle:thin:@<ALIAS_TNS>?TNS_ADMIN=C:/Oracle/Wallet_RutaExpress
   ```

4. Configura `DB_USERNAME` y `DB_PASSWORD` con el usuario y contraseña de la
   base de datos, no con la contraseña usada al descargar el wallet.

`TNS_ADMIN` en la URL apunta a la carpeta descomprimida, no al ZIP ni a un archivo
individual. Conserva los archivos suministrados en el wallet, incluidos
`tnsnames.ora`, `ojdbc.properties` y `cwallet.sso` cuando estén presentes.
Las variables de Azure siguen siendo necesarias para arrancar la aplicación.

Referencias: [conexión JDBC con Wallet](https://docs.oracle.com/en/cloud/paas/autonomous-database/adbsa/connect-jdbc-thin-wallet.html)
y [dependencias Oracle JDBC 23](https://blogs.oracle.com/developers/using-ojdbcbom-to-get-the-jars-that-you-need).

### Comandos

Requiere JDK 21 en `JAVA_HOME` y su carpeta `bin` en el PATH.
Configura las cinco variables anteriores con valores de tu entorno.

```shell
mvn clean test
mvn clean package
mvn spring-boot:run
```

Si Maven no está en el PATH, usa el wrapper incluido:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

También puedes ejecutar el artefacto generado:

```shell
java -jar target/ms-rutaexpress-catalog-0.0.1-SNAPSHOT.jar
```

URL base local: `http://localhost:8082`.
Clase principal para IntelliJ: `cl.rutaexpress.catalog.CatalogApplication`.
Si conservabas una configuración que apuntaba a la clase inicial, actualízala.

## Seguridad

Todas las operaciones reciben `Authorization: Bearer <ACCESS_TOKEN>`.
Debe ser un access token para esta API, no un ID token.

Se validan firma, emisor, vigencia y audiencia mediante OAuth2 Resource Server.
`JwtAuthenticationConverter` lee `roles` y añade el prefijo `ROLE_`:
`roles: ["ADMIN"]` se transforma en la autoridad `ROLE_ADMIN`.
Los roles distinguen mayúsculas y minúsculas. Se conservan los scopes como authorities separadas: `scp: "OT.Create"` genera `SCOPE_OT.Create`. Un scope por sí solo no concede acceso a Catalog.

| Método y ruta | Roles permitidos | Respuesta |
| --- | --- | --- |
| `GET /api/catalog/services` | ADMIN, DISPATCHER, CLIENT | 200, lista (incluye inactivos) |
| `GET /api/catalog/services/{id}` | ADMIN, DISPATCHER, CLIENT | 200 o 404 |
| `POST /api/catalog/services` | ADMIN | 201, recurso y cabecera Location |
| `PUT /api/catalog/services/{id}` | ADMIN | 200, recurso actualizado, o 404 |

Sin token o con token inválido: **401**. Sin rol suficiente: **403**.
La API no usa sesiones ni cookies de autenticación. CSRF está desactivado para
este flujo Bearer. Las demás rutas/métodos se deniegan. Solo el despacho interno
de errores se permite para conservar las respuestas de error de Spring.

### Desarrollo sin Azure

Las pruebas automatizadas funcionan sin Azure ni Oracle: sustituyen el decoder JWT
y el servicio/repositorio por mocks. El filtro Bearer, el convertidor de roles, la
autorización y Bean Validation sí se ejecutan en las pruebas HTTP.

```shell
mvn test
```

Para pruebas manuales por HTTP se necesita Oracle y un emisor JWT operativo.
Si Azure aún no está disponible, puedes apuntar temporalmente
`AZURE_ISSUER_URI` y `AZURE_AUDIENCE` a un emisor OIDC de desarrollo que publique
sus claves y emita tokens firmados con el claim `roles`. Restaura ambos valores al
integrar Azure. No hay un perfil que omita autenticación ni un decoder simulado
en el código de producción.

Referencia del mecanismo de conversión:
[Spring Security — JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

## Ejemplos de solicitudes

### Crear

`POST http://localhost:8082/api/catalog/services`

Cabeceras:
`Content-Type: application/json` y `Authorization: Bearer <ACCESS_TOKEN_ADMIN>`.

```json
{
  "nombre": "Entrega Express",
  "descripcion": "Entrega durante el mismo día",
  "tarifa": 7990,
  "capacidadDisponible": 20,
  "activo": true
}
```

Respuesta **201 Created**, con `Location: http://localhost:8082/api/catalog/services/1`
si el ID generado es 1:

```json
{
  "id": 1,
  "nombre": "Entrega Express",
  "descripcion": "Entrega durante el mismo día",
  "tarifa": 7990,
  "capacidadDisponible": 20,
  "activo": true
}
```

### Consultar

`GET http://localhost:8082/api/catalog/services` devuelve un arreglo JSON de
servicios, o `[]` si aún no existen.

`GET http://localhost:8082/api/catalog/services/1` devuelve el objeto anterior.
Usa el ID devuelto por POST; los IDs de secuencia no necesariamente son consecutivos.

Ejemplo PowerShell, suponiendo un access token en `$env:ACCESS_TOKEN`:

```powershell
$headers = @{ Authorization = "Bearer $env:ACCESS_TOKEN" }
Invoke-RestMethod -Uri 'http://localhost:8082/api/catalog/services' -Headers $headers
Invoke-RestMethod -Uri 'http://localhost:8082/api/catalog/services/1' -Headers $headers
```

### Actualizar

`PUT http://localhost:8082/api/catalog/services/1`, con un token de ADMIN:

```json
{
  "nombre": "Entrega Express",
  "descripcion": "Entrega durante el mismo día",
  "tarifa": 8490,
  "capacidadDisponible": 15,
  "activo": false
}
```

Responde **200 OK** con los mismos datos y el ID. PUT reemplaza todos los campos
editables. `descripcion` es opcional: omitirla o enviar `null` la borra.
Consultar/actualizar un ID inexistente devuelve **404**, nunca crea un recurso.

### Validación

- `nombre`: obligatorio, no vacío ni compuesto solo de espacios; máximo 150 caracteres.
- `descripcion`: opcional; máximo 1000 caracteres.
- `tarifa`: obligatoria y no negativa; hasta 12 dígitos enteros y 2 decimales.
- `capacidadDisponible`: entero obligatorio, entre 0 y 2147483647.
- `activo`: booleano obligatorio tanto en POST como en PUT.

Ejemplo inválido, que devuelve **400 Bad Request**:

```json
{
  "nombre": " ",
  "tarifa": -1,
  "capacidadDisponible": -2,
  "activo": true
}
```

Los errores de validación usan `application/problem+json` con `status`,
`detail` y un objeto `errores` con los campos afectados.
JSON malformado e IDs no numéricos también devuelven 400.

## Pruebas y pendientes de integración

Las pruebas cubren creación, consulta, listado vacío y con inactivos, actualización
de todos los campos, recursos inexistentes, validaciones, JSON malformado, 401,
403 y conversión de roles (incluidos varios roles y ausencia de roles).

Las pruebas simulan persistencia y validación criptográfica; no verifican una
conexión Oracle ni tokens reales de Entra. Queda pendiente validar en el entorno
real la creación de tabla/secuencia, persistencia tras reiniciar y acceso con
tokens reales y roles asignados.

El consumidor BFF deberá usar estas rutas/DTOs y propagar un access token con
audiencia válida para Catalog. No se modificó el BFF ni Shipments.

Esta entrega no incluye eliminación, endpoints adicionales de capacidad,
mensajería, notificaciones, dashboard, auditoría, Docker Compose, AWS ni lógica de envíos.

## Docker

Este repositorio construye únicamente Catalog. Requiere Docker Engine o Docker
Desktop en modo contenedores Linux. No necesita los repositorios de otros servicios.

El Dockerfile usa `maven:3.9.16-eclipse-temurin-21` para ejecutar Maven y las pruebas.
La imagen final usa `eclipse-temurin:21-jre-jammy`, recibe solamente el JAR generado
y ejecuta Java 21 como usuario `catalog` (UID 10001), sin Maven.

Desde la raíz de este repositorio:

```powershell
docker build -t ms-rutaexpress-catalog .
```

La construcción no requiere Oracle, Azure ni el wallet. `.dockerignore` permite
únicamente el POM, los fuentes Java y `application.properties`; excluye archivos
locales, secretos y wallets. Si se agregan otros recursos de aplicación o de
pruebas en el futuro, deberán habilitarse explícitamente en ese archivo.

### Ejecutar en Windows (PowerShell)

Configura en la terminal `DB_USERNAME`, `DB_PASSWORD`, `AZURE_ISSUER_URI` y
`AZURE_AUDIENCE` con tus valores reales. Las variables de una configuración de
Run de IntelliJ no se transfieren automáticamente a la terminal ni a Docker.
El comando siguiente las pasa al contenedor sin escribir sus valores en el comando.

```powershell
$walletPath = (Resolve-Path -LiteralPath (Read-Host 'Carpeta del wallet descomprimido')).Path
$env:DB_URL = 'jdbc:oracle:thin:@rutaexpress_medium?TNS_ADMIN=/opt/oracle/wallet'

docker run --rm --name ms-rutaexpress-catalog `
  -p 8082:8082 `
  -e DB_URL `
  -e DB_USERNAME `
  -e DB_PASSWORD `
  -e AZURE_ISSUER_URI `
  -e AZURE_AUDIENCE `
  --mount "type=bind,source=$walletPath,target=/opt/oracle/wallet,readonly" `
  ms-rutaexpress-catalog
```

Cambia `rutaexpress_medium` por el alias de tu `tnsnames.ora`. La ruta de
`TNS_ADMIN` es la ruta Linux dentro del contenedor; la ruta local se usa solamente
en el montaje. Se monta la carpeta descomprimida completa, nunca el ZIP, y siempre
en modo de solo lectura. El usuario del contenedor debe poder leer sus archivos.
No copies el wallet al repositorio ni a la imagen.

Las cinco variables requeridas son `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
`AZURE_ISSUER_URI` y `AZURE_AUDIENCE`. Se conserva la validación de audiencia,
issuer, firma y roles. No se cambia la configuración de Oracle ni de Spring Security.

Detén la ejecución local de Catalog antes de usar el mismo puerto 8082.
Con el contenedor iniciado, verifica los logs y prueba desde otra terminal:

```powershell
docker logs ms-rutaexpress-catalog
curl.exe -i http://localhost:8082/api/catalog/services
```

Sin Bearer token se espera 401. Para obtener los servicios utiliza un access token
válido con un rol autorizado. El arranque completo requiere acceso de red a Oracle
y la configuración real de Azure; un build exitoso no comprueba esa conexión.

Imágenes oficiales: [Maven](https://hub.docker.com/_/maven) y
[Eclipse Temurin](https://hub.docker.com/_/eclipse-temurin).
