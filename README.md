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
`roles: ["Admin"]` se transforma en la autoridad `ROLE_Admin`.
Los roles distinguen mayúsculas y minúsculas. No se utilizan los scopes como roles.

| Método y ruta | Roles permitidos | Respuesta |
| --- | --- | --- |
| `GET /api/catalog/services` | Admin, Operador, Cliente, Auditor | 200, lista (incluye inactivos) |
| `GET /api/catalog/services/{id}` | Admin, Operador, Cliente, Auditor | 200 o 404 |
| `POST /api/catalog/services` | Admin | 201, recurso y cabecera Location |
| `PUT /api/catalog/services/{id}` | Admin | 200, recurso actualizado, o 404 |

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

`PUT http://localhost:8082/api/catalog/services/1`, con un token de Admin:

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
mensajería, notificaciones, dashboard, auditoría, Docker, AWS ni lógica de envíos.
