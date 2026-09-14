# README.AGENT.md — RutaExpress

## 1. Objetivo del proyecto

RutaExpress es una plataforma para una red de couriers PyME que centraliza la creación y seguimiento de envíos, la administración de servicios y capacidad de flota, y el acceso según rol.

Esta primera entrega se enfoca en:

- Frontend Angular.
- Login corporativo con Azure AD mediante MSAL.
- Uso de JWT en llamadas al backend.
- Backend con 3 aplicaciones Spring Boot:
  - `ms-rutaexpress-bff`
  - `ms-rutaexpress-shipments`
  - `ms-rutaexpress-catalog`
- Spring Security + OAuth2 Resource Server.
- Persistencia Oracle para `shipments` y `catalog`.
- CRUD y reglas de negocio mínimas.
- Integración Angular -> BFF -> microservicios.
- Buenas prácticas, compilación correcta y `.gitignore`.

Fuera de alcance de esta primera entrega:

- RabbitMQ.
- Kafka.
- Zookeeper.
- Analítica por streaming.
- Notificaciones asíncronas reales.
- Docker / Docker Compose, salvo que el equipo decida adelantarlo.
- Despliegue productivo en AWS, salvo que sea requerido expresamente.

---

## 2. Arquitectura objetivo de la primera entrega

```text
Angular + MSAL
      |
      | Authorization: Bearer <JWT>
      v
AWS API Gateway / entorno local equivalente
      |
      v
ms-rutaexpress-bff
      |
      +--------------------+
      |                    |
      v                    v
ms-rutaexpress-shipments   ms-rutaexpress-catalog
      |                    |
      v                    v
   Oracle               Oracle
```

Durante desarrollo local puede trabajarse sin API Gateway:

```text
Angular :4200
   |
   v
BFF :8080
   |
   +--> Shipments :8081
   |
   +--> Catalog :8082
```

---

## 3. Proyectos Spring Boot

### 3.1 ms-rutaexpress-bff

Responsabilidad:

- Ser el punto de entrada del frontend.
- Validar el JWT.
- Aplicar autorización.
- Consumir los microservicios internos.
- No acceder directamente a Oracle.

Configuración recomendada:

```text
Group: cl.rutaexpress
Artifact: ms-rutaexpress-bff
Package: cl.rutaexpress.bff
Java: 21
Packaging: Jar
Build: Maven
```

Dependencias:

- Spring Web
- HTTP Client
- Spring Security
- OAuth2 Resource Server
- Validation
- Lombok
- Spring Boot DevTools

Puerto local:

```properties
server.port=8080
```

Estructura sugerida:

```text
cl.rutaexpress.bff
├── config
├── security
├── controller
├── client
├── dto
├── exception
└── BffApplication
```

---

### 3.2 ms-rutaexpress-shipments

Responsabilidad:

- Crear envíos.
- Consultar envíos.
- Filtrar envíos.
- Cambiar estado.
- Validar transiciones de estado.
- Coordinar cambios de capacidad mediante `catalog`.

Configuración:

```text
Group: cl.rutaexpress
Artifact: ms-rutaexpress-shipments
Package: cl.rutaexpress.shipments
Java: 21
Packaging: Jar
Build: Maven
```

Dependencias:

- Spring Web
- HTTP Client
- Spring Data JPA
- Oracle Driver
- Spring Security
- OAuth2 Resource Server
- Validation
- Lombok
- Spring Boot DevTools

Puerto local:

```properties
server.port=8081
```

Estructura sugerida:

```text
cl.rutaexpress.shipments
├── config
├── security
├── controller
├── service
├── repository
├── entity
├── dto
├── client
├── exception
└── ShipmentsApplication
```

Endpoints mínimos:

```http
POST /api/shipments
GET /api/shipments/{id}
PUT /api/shipments/{id}/status
GET /api/shipments?status=...&from=...&to=...
```

Estados permitidos:

```text
CREADO
ACEPTADO
EN_BODEGA
EN_RUTA
ENTREGADO
CANCELADO
```

Regla mínima:

- No se puede pasar a `EN_RUTA` sin haber sido aceptado previamente.
- Las reglas de estado deben validarse en backend, no solamente en Angular.

---

### 3.3 ms-rutaexpress-catalog

Responsabilidad:

- CRUD de servicios de envío.
- Administrar tarifas.
- Administrar capacidad disponible.
- Exponer operaciones para que `shipments` consulte o actualice capacidad.

Configuración:

```text
Group: cl.rutaexpress
Artifact: ms-rutaexpress-catalog
Package: cl.rutaexpress.catalog
Java: 21
Packaging: Jar
Build: Maven
```

Dependencias:

- Spring Web
- Spring Data JPA
- Oracle Driver
- Spring Security
- OAuth2 Resource Server
- Validation
- Lombok
- Spring Boot DevTools

Puerto local:

```properties
server.port=8082
```

Estructura sugerida:

```text
cl.rutaexpress.catalog
├── config
├── security
├── controller
├── service
├── repository
├── entity
├── dto
├── exception
└── CatalogApplication
```

Endpoints mínimos:

```http
GET /api/catalog/services
POST /api/catalog/services
PUT /api/catalog/services/{id}
```

---

## 4. Frontend Angular

Responsabilidades:

- Login con Azure AD mediante MSAL.
- Protección de rutas.
- Roles:
  - Admin
  - Operador
  - Cliente
  - Auditor
- Adjuntar JWT a requests HTTP.
- Consumir solamente el BFF.

No hacer:

```text
Angular -> Shipments directamente
Angular -> Catalog directamente
```

Hacer:

```text
Angular -> BFF -> microservicios
```

Estructura sugerida:

```text
src/app
├── core
│   ├── auth
│   ├── guards
│   ├── interceptors
│   └── services
├── features
│   ├── shipments
│   ├── catalog
│   ├── dashboard
│   └── audit
├── shared
└── app.routes.ts
```

---

## 5. Seguridad

Azure AD / Microsoft Entra ID será el IDaaS.

El frontend obtiene un access token mediante MSAL.

Cada request protegida debe enviar:

```http
Authorization: Bearer <access_token>
```

Cada backend protegido usa:

- Spring Security.
- OAuth2 Resource Server.
- `issuer-uri` correspondiente al tenant de Azure.
- Validación de roles desde el JWT.

Ejemplo conceptual:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.microsoftonline.com/<TENANT_ID>/v2.0
```

No colocar secretos, tenant IDs privados, contraseñas ni connection strings reales en Git.

Usar variables de entorno cuando corresponda.

---

## 6. Base de datos

`shipments` y `catalog` usan Oracle.

No hacer que un microservicio acceda directamente a las tablas del otro.

Preferencia:

```text
Shipments -> API Catalog
```

y no:

```text
Shipments -> tablas internas de Catalog
```

Separar responsabilidades incluso si ambos esquemas se encuentran en una misma instancia Oracle.

---

## 7. Convenciones de código

### Java

- Clases: `PascalCase`.
- Métodos/variables: `camelCase`.
- Constantes: `UPPER_SNAKE_CASE`.
- Paquetes en minúsculas.
- No usar guiones en package names.
- No exponer entidades JPA directamente desde controllers.
- Usar DTOs para request/response.
- Validar entradas con Bean Validation.
- Mantener controllers delgados.
- Colocar lógica de negocio en `service`.
- Acceso a datos únicamente mediante `repository`.
- Manejar excepciones de forma centralizada cuando sea posible.
- Evitar lógica duplicada.

Ejemplo de capas:

```text
Controller
   |
   v
Service
   |
   v
Repository
   |
   v
Oracle
```

### DTOs

Preferir records cuando sea apropiado:

```java
public record ServicioRequest(
    String nombre,
    BigDecimal tarifa,
    Integer capacidad
) {}
```

### Estados

Usar `enum`, no strings libres:

```java
public enum ShipmentStatus {
    CREADO,
    ACEPTADO,
    EN_BODEGA,
    EN_RUTA,
    ENTREGADO,
    CANCELADO
}
```

---

## 8. Reglas para el agente IA

Al trabajar en este repositorio:

1. No introducir RabbitMQ, Kafka o Zookeeper en la primera entrega salvo solicitud explícita.
2. No cambiar la arquitectura sin explicar primero el motivo.
3. No agregar dependencias innecesarias.
4. Mantener Java 21, Maven y Spring Boot en la versión definida por el proyecto.
5. No cambiar nombres de paquetes, artifacts o endpoints sin autorización.
6. No reemplazar Oracle por otra base de datos.
7. No poner credenciales reales en código ni configuración versionada.
8. No hacer que el BFF acceda directamente a Oracle.
9. No hacer que Angular llame directamente a `shipments` o `catalog`.
10. Mantener la validación JWT en backend aunque el frontend también proteja rutas.
11. Implementar primero la solución más simple que cumpla el requisito.
12. Antes de crear clases nuevas, revisar si ya existe una responsabilidad equivalente.
13. Evitar sobreingeniería.
14. Mantener cada microservicio compilable después de cada cambio.
15. Si se modifica una API, indicar qué componentes consumidores deben actualizarse.
16. Cuando se proponga código, indicar el archivo y ruta donde debe ir.
17. No asumir datos que no estén definidos; si una regla de negocio es ambigua, preguntar.
18. Priorizar buenas prácticas de separación de responsabilidades.
19. Incluir pruebas básicas en servicios y controladores cuando sea razonable.
20. Mantener `.gitignore` correcto para Java/Maven, Angular, IDE y archivos de entorno.

---

## 9. Orden de implementación

### Fase 1 — Crear los proyectos

- [ ] Crear `ms-rutaexpress-bff`.
- [ ] Crear `ms-rutaexpress-shipments`.
- [ ] Crear `ms-rutaexpress-catalog`.
- [ ] Crear/confirmar frontend Angular.
- [ ] Configurar `.gitignore`.
- [ ] Confirmar que todos compilan sin errores.

### Fase 2 — Catalog

- [ ] Configurar Oracle.
- [ ] Crear entidad `Servicio`.
- [ ] Crear repository.
- [ ] Crear DTOs.
- [ ] Crear service.
- [ ] Crear controller.
- [ ] Implementar `GET /api/catalog/services`.
- [ ] Implementar `POST /api/catalog/services`.
- [ ] Implementar `PUT /api/catalog/services/{id}`.
- [ ] Probar endpoints.
- [ ] Agregar manejo básico de errores.

### Fase 3 — Shipments

- [ ] Configurar Oracle.
- [ ] Crear entidad `Shipment`.
- [ ] Crear `ShipmentStatus`.
- [ ] Crear repository.
- [ ] Crear DTOs.
- [ ] Crear service.
- [ ] Crear controller.
- [ ] Implementar `POST /api/shipments`.
- [ ] Implementar `GET /api/shipments/{id}`.
- [ ] Implementar `PUT /api/shipments/{id}/status`.
- [ ] Implementar filtros de búsqueda.
- [ ] Validar transiciones de estado.
- [ ] Probar endpoints.

### Fase 4 — Comunicación interna

- [ ] Configurar HTTP Client en BFF.
- [ ] Crear `ShipmentsClient`.
- [ ] Crear `CatalogClient`.
- [ ] Exponer endpoints en BFF.
- [ ] Configurar HTTP Client en Shipments si debe consultar capacidad.
- [ ] Probar flujo BFF -> servicios.

### Fase 5 — Seguridad backend

- [ ] Configurar OAuth2 Resource Server.
- [ ] Configurar `issuer-uri`.
- [ ] Validar JWT.
- [ ] Mapear roles de Azure.
- [ ] Restringir endpoints según rol.
- [ ] Probar request sin JWT -> 401.
- [ ] Probar JWT sin rol suficiente -> 403.
- [ ] Probar JWT válido -> acceso correcto.

### Fase 6 — Angular + Azure

- [ ] Configurar MSAL.
- [ ] Configurar login.
- [ ] Configurar logout.
- [ ] Proteger rutas.
- [ ] Crear interceptor para Bearer token.
- [ ] Consumir únicamente el BFF.
- [ ] Crear vistas mínimas para envíos.
- [ ] Crear vistas mínimas para catálogo.
- [ ] Mostrar acciones según rol.

### Fase 7 — Pruebas y entrega

- [ ] `mvn clean test` en BFF.
- [ ] `mvn clean test` en Shipments.
- [ ] `mvn clean test` en Catalog.
- [ ] Compilar Angular.
- [ ] Revisar que no existan secretos en Git.
- [ ] Revisar `.gitignore`.
- [ ] Probar flujo completo.
- [ ] Documentar variables de entorno.
- [ ] Documentar cómo ejecutar los componentes.
- [ ] Confirmar que los repositorios contengan solo archivos necesarios.

---

## 10. Prioridad actual

No intentar construir todo a la vez.

Orden inmediato:

```text
1. Generar los 3 Spring Boot.
2. Verificar que los 3 levantan.
3. Implementar Catalog.
4. Conectar Catalog a Oracle.
5. Probar Catalog.
6. Implementar Shipments.
7. Conectar Shipments a Oracle.
8. Probar Shipments.
9. Implementar BFF.
10. Integrar BFF con ambos servicios.
11. Agregar seguridad JWT.
12. Integrar Angular + MSAL.
```

---

## 11. Definición de terminado para la primera entrega

La entrega está lista cuando:

- Los 3 proyectos Spring Boot compilan.
- Angular compila.
- Catalog persiste datos en Oracle.
- Shipments persiste datos en Oracle.
- Los endpoints mínimos funcionan.
- Las reglas de estado funcionan.
- El BFF consume los microservicios.
- Angular consume el BFF.
- Azure AD permite iniciar sesión.
- El JWT viaja desde Angular al backend.
- Spring Security valida el JWT.
- Los roles restringen endpoints.
- No hay credenciales reales en Git.
- `.gitignore` está correctamente configurado.
- Existen pruebas básicas y el código mantiene una estructura limpia.

---

## 12. Comandos útiles

Backend:

```bash
mvn clean test
mvn clean package
mvn spring-boot:run
```

Frontend Angular:

```bash
npm install
ng serve
ng build
```

Antes de hacer commit:

```bash
git status
```

Verificar especialmente que no se suban:

```text
target/
node_modules/
.env
.env.*
.idea/
.vscode/
*.log
```

Si `.vscode` contiene configuraciones compartidas necesarias para el equipo, evaluar caso a caso antes de ignorarla completamente.
