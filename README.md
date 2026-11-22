# CoWorkHub — API REST de gestión de espacios de coworking

Proyecto final del curso Spring Boot (módulos 01–08). Backend **exclusivamente REST** que reemplaza las hojas de cálculo
de una red de coworking: sedes, salas, equipamiento, planes de membresía, miembros y cuentas, disponibilidad, reservas,
servicios adicionales, costos, penalizaciones y consumo mensual de horas, con control de acceso por rol y por propiedad
del recurso.

---

## 1. Tecnologías

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 (compila con `release 21`; probado también con JDK 26) | Lenguaje |
| Spring Boot | **4.1.2** | Framework |
| Maven | 3.9.16 vía **Maven Wrapper** (`mvnw`) | Build — no hace falta instalar Maven |
| MySQL | 8.x | Base de datos `coworkhub` |
| Spring Web MVC | (Boot) | API REST |
| Spring Data JPA + Hibernate | 7.x | Persistencia |
| Spring Security + JWT (jjwt 0.13.0) + BCrypt | (Boot) | Autenticación stateless y autorización |
| Bean Validation | (Boot) | Validación de DTO |
| springdoc-openapi | 3.2.0 | Swagger UI / OpenAPI 3 |
| Spring Boot Actuator | (Boot) | `/actuator/health` |
| JUnit 5 + Mockito + Spring Security Test | (Boot) | Pruebas |

No se usa Lombok ni MapStruct (no forman parte del stack exigido): getters/setters y mappers están escritos a mano.

---

## 2. Arquitectura

> El análisis completo (actores y casos de uso, glosario, diagrama entidad-relación con la justificación
> de lado dueño, `fetch` y `cascade`, matriz de trazabilidad RF → RN → endpoints, supuestos y fuera de
> alcance) está en [`docs/analisis.md`](docs/analisis.md).

Arquitectura por capas; los controladores solo delegan, las reglas viven en `business`, y nunca se devuelven entidades JPA.

```
src/main/java/com/coworkhub
├── CoworkhubApplication.java
├── presentation/   Controladores REST (@RestController) — solo HTTP + DTO
├── business/       Interfaces de servicio (con @PreAuthorize)
│   ├── impl/       Implementaciones (@Service, @Transactional)
│   └── rules/      Reglas puras: BookingRules, CostCalculator, CancellationPolicy
├── persistence/    Repositorios Spring Data JPA (consultas JPQL, @EntityGraph, @Lock)
├── domain/         Entidades JPA
│   └── enums/      RoomType, MemberStatus, BookingStatus, Role
├── dto/            Records de entrada/salida (auth, catalog, member, booking)
├── mapper/         Entidad <-> DTO
├── config/         AppProperties (@Value + @PostConstruct), BeansConfig (@Bean, @Primary, @Qualifier),
│                   SecurityConfig, OpenApiConfig
├── security/       JwtService, JwtAuthenticationFilter, UserDetailsService, 401/403 handlers, CurrentUserProvider
├── exception/      Excepciones de negocio + @RestControllerAdvice
└── validation/     Validaciones personalizadas (@ValidTimeRange, @ValidAccountData)
src/main/resources
├── application.properties
└── import.sql      Datos iniciales (Hibernate lo ejecuta al arrancar)
docs/openapi.json   Especificación OpenAPI exportada
postman/            Colección con los 14 escenarios de aceptación
scripts/            SQL de creación de BD/usuario y prueba de concurrencia
```

Flujo de una petición: `JwtAuthenticationFilter` → reglas URL del `SecurityFilterChain` → Controlador (`@Valid`) →
Servicio (`@PreAuthorize` + propiedad del recurso + reglas RN) → Repositorio → MySQL. Cualquier excepción la traduce
`GlobalExceptionHandler` al formato uniforme de error.

### Modelo de datos

```
Location 1──N Room N──M Equipment          (tabla intermedia room_equipment)
                │
                1
                │
MembershipPlan 1──N Member 1──N Booking N──1 Room
                    │             │
                    1             1──N BookingItem N──1 AdditionalService
                    │ (opcional)
                 UserAccount
```

| Entidad (tabla) | Relaciones JPA |
|---|---|
| `Location` (`locations`) | `@OneToMany(mappedBy="location")` rooms — sin cascade REMOVE (RN-12) |
| `Room` (`rooms`) | `@ManyToOne(LAZY) @JoinColumn(location_id)`; `@ManyToMany(LAZY) @JoinTable(room_equipment)`; único (`location_id`,`name`) |
| `Equipment` (`equipment`) | `@ManyToMany(mappedBy="equipment")` lado inverso; nombre único |
| `MembershipPlan` (`membership_plans`) | `@OneToMany(mappedBy="plan")` members; nombre único |
| `Member` (`members`) | `@ManyToOne(LAZY)` plan; `@OneToOne(mappedBy="member")` userAccount opcional; documento y email únicos |
| `UserAccount` (`user_accounts`) | `@OneToOne(LAZY, optional=true) @JoinColumn(member_id, unique)` — los empleados no tienen miembro |
| `Booking` (`bookings`) | `@ManyToOne(LAZY)` room y member; `@OneToMany(cascade=ALL, orphanRemoval=true)` items |
| `BookingItem` (`booking_items`) | `@ManyToOne(LAZY)` booking y service; guarda `appliedUnitPrice` (precio histórico) |
| `AdditionalService` (`additional_services`) | `@OneToMany(mappedBy="service")` items; nombre único |

**Campo adicional justificado:** `Booking.includedHoursApplied` guarda cuántas horas de la reserva se cubrieron con las
horas del plan. Sin él no es posible calcular las horas disponibles del mes (RN-07), "devolver horas" en una
cancelación gratuita (RN-08) ni el reporte de consumo.

**N+1 y serialización:** todas las asociaciones son `LAZY`, `spring.jpa.open-in-view=false`, y las consultas usan
`@EntityGraph`/`join fetch` para cargar lo que el mapper necesita. Como solo se serializan DTO (records), no hay
referencias circulares.

---

## 3. Reglas de negocio

| Regla | Dónde | Resumen |
|---|---|---|
| RN-01 | `BookingRules.validateNoOverlap` + bloqueos en `BookingServiceImpl.create` | Sin cruces PENDING/CONFIRMED; 09–10 y 10–11 son válidas; soporta concurrencia |
| RN-02 | `BookingRules.validateDuration` | 30 min – 8 h, múltiplos de 30 min, mismo día |
| RN-03 | `validateAdvance`, `validateWithinOpeningHours` | Futura, ≥ 1 h de anticipación, dentro del horario de la sede |
| RN-04 | `validateRoom` | Sala activa y capacidad suficiente |
| RN-05 | `validateMemberActive` | Solo miembros ACTIVE |
| RN-06 | `validateActiveBookingLimit` | Máximo de reservas activas futuras del plan |
| RN-07 | `CostCalculator` | Excedente = horas × tarifa × (1 − descuento); servicios = Σ cantidad × precio aplicado |
| RN-08 | `CancellationPolicy` | ≥ 24 h gratis + devolución de horas; 24–2 h → 50 % sala; < 2 h o iniciada → 409 |
| RN-09 | `BookingStatus.canTransitionTo` | PENDING→CONFIRMED→COMPLETED; PENDING/CONFIRMED→CANCELLED |
| RN-10 | `validateServicesEditable` | Servicios solo en PENDING |
| RN-11 | Servicios + `unique` en BD | Documento, email, usuario, sala por sede, equipamiento, servicio, plan |
| RN-12 | `delete` de cada servicio | No se elimina lo que está en uso |
| RN-13 | `SecurityConfig` + `@PreAuthorize` + `CurrentUserProvider` | Rol y propiedad del recurso |

### Concurrencia (RN-01, escenario 13)

`BookingServiceImpl.create` corre en una transacción `READ_COMMITTED` y antes de validar ejecuta
`SELECT ... FOR UPDATE` sobre el **miembro** y luego sobre la **sala** (`@Lock(PESSIMISTIC_WRITE)`), siempre en ese
orden para evitar interbloqueos. Dos solicitudes simultáneas para la misma sala quedan en fila: la segunda espera a
que la primera confirme, vuelve a leer y encuentra la reserva nueva, así que responde **409 RN-01**. Bloquear el
miembro protege además el límite del plan (RN-06) y el cálculo de horas incluidas (RN-07). Si un bloqueo supera 5 s
(`jakarta.persistence.lock.timeout`), la respuesta también es 409.

---

## 4. Requisitos previos

- JDK 21 o superior (`java -version`).
- MySQL 8 corriendo en `localhost:3306`.
- No hace falta instalar Maven: se usa `mvnw` / `mvnw.cmd`.

## 5. Configuración de MySQL

Ejecute una sola vez como **root** el script que crea la base `coworkhub` y el usuario `coworkhub` / `coworkhub`:

```bash
mysql -u root -p < scripts/01-create-database.sql
```

En Windows PowerShell (donde `<` no existe):

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' -u root -p -e "source scripts/01-create-database.sql"
```

Alternativa con Docker:

```bash
docker run -d --name coworkhub-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=coworkhub -e MYSQL_USER=coworkhub -e MYSQL_PASSWORD=coworkhub mysql:8.4
```

Las tablas **no** se crean a mano: con `spring.jpa.hibernate.ddl-auto=create` Hibernate recrea el esquema en cada
arranque y ejecuta `src/main/resources/import.sql` (2 sedes, 6 salas, 4 equipamientos, 3 servicios, 3 planes,
5 miembros, 4 cuentas y 15 reservas). Las fechas son relativas a `NOW()`/`CURDATE()`, así que los datos no caducan.
Puede cambiar las credenciales con las variables de entorno `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET`.

## 6. Ejecución

```bash
./mvnw spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

La API queda en `http://localhost:8080`. Comprobación: `GET http://localhost:8080/actuator/health` → `{"status":"UP"}`.

## 7. Usuarios de prueba

| Usuario | Contraseña | Rol | Miembro |
|---|---|---|---|
| `admin` | `Admin123*` | ADMIN | — |
| `recepcion` | `Recep123*` | RECEPTIONIST | — |
| `ana` | `Ana123*` | MEMBER | 1 — Ana María Gómez (plan Profesional) |
| `carlos` | `Carlos123*` | MEMBER | 2 — Carlos Ruiz (plan Básico) |

Las contraseñas se guardan con BCrypt en `import.sql`. El miembro 4 (Jorge Díaz) está **SUSPENDED**.

> ⚠️ **Advertencia educativa.** Las contraseñas anteriores, las credenciales de MySQL
> (`coworkhub`/`coworkhub`) y la clave de firma JWT incluidas en `application.properties` existen
> **solo con fines académicos**, para que el proyecto se pueda ejecutar sin configuración extra. En un
> sistema real nunca se versionan: la clave se inyecta con la variable de entorno `JWT_SECRET` (y la
> base de datos con `DB_USERNAME`/`DB_PASSWORD`) o con un gestor de secretos. Quien conozca la clave
> JWT puede firmar tokens válidos con cualquier rol. Los datos personales de `import.sql` son ficticios.

## 8. Autenticación

```http
POST /auth/login
Content-Type: application/json

{ "usuario": "admin", "password": "Admin123*" }
```

Respuesta: `{ "token": "eyJ...", "tipo": "Bearer", "expiraEnMinutos": 60, "usuario": "admin", "rol": "ADMIN", "miembroId": null }`.
Envíe el token en cada petición: `Authorization: Bearer eyJ...`. El token dura 60 minutos.

## 9. Endpoints principales

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/auth/login` | Público |
| GET | `/auth/perfil` | Autenticado |
| GET | `/actuator/health` | Público |
| GET | `/api/{sedes\|salas\|equipamientos\|servicios-adicionales\|planes}[/{id}]` | Autenticado (`/api/salas?sedeId=`) |
| POST/PUT/DELETE | `/api/{catálogo}[/{id}]` | ADMIN |
| GET | `/api/miembros?nombre=&estado=` | ADMIN, RECEPTIONIST |
| GET | `/api/miembros/{id}` | ADMIN, RECEPTIONIST, MEMBER (solo el propio) |
| POST/PUT/DELETE | `/api/miembros[/{id}]` | ADMIN, RECEPTIONIST |
| PATCH | `/api/miembros/{id}/suspender`, `/api/miembros/{id}/activar` | ADMIN, RECEPTIONIST |
| GET | `/api/miembros/{id}/consumo?mes=yyyy-MM` | ADMIN, RECEPTIONIST, MEMBER (solo el propio) |
| GET | `/api/disponibilidad?sedeId=&fecha=&horaInicio=&horaFin=&capacidadMinima=&equipamientoIds=` | Autenticado |
| POST | `/api/reservas` | ADMIN, RECEPTIONIST, MEMBER (a su nombre) |
| GET | `/api/reservas?miembroId=&salaId=&fecha=&estado=` | Autenticado (MEMBER solo las suyas) |
| GET | `/api/reservas/{id}` | Autenticado (MEMBER solo las suyas) |
| POST | `/api/reservas/{id}/servicios` · DELETE `/api/reservas/{id}/servicios/{servicioId}` | Autenticado (MEMBER solo las suyas) |
| POST | `/api/reservas/{id}/confirmar`, `/api/reservas/{id}/completar` | ADMIN, RECEPTIONIST |
| POST | `/api/reservas/{id}/cancelar` | Autenticado (MEMBER solo las suyas) |

### Formato de error y códigos HTTP

```json
{ "error": "La sala ya tiene una reserva que se cruza con el horario solicitado", "codigo": "RN-01", "timestamp": "2026-11-17T10:15:30" }
```

| HTTP | Cuándo | `codigo` |
|---|---|---|
| 200 / 201 / 204 | Éxito / creado / eliminado | — |
| 400 | Datos inválidos, parámetros con formato erróneo | `VALIDACION` |
| 401 | Sin token, token alterado o expirado, credenciales incorrectas | `NO_AUTENTICADO` |
| 403 | Rol insuficiente o recurso de otro miembro | `RN-13` |
| 404 | Recurso inexistente | `NO_ENCONTRADO` |
| 409 | Regla de negocio o duplicado | `RN-01` … `RN-12` |

## 10. Swagger / OpenAPI

- Swagger UI: **http://localhost:8080/doc/swagger-ui.html**
- JSON: http://localhost:8080/v3/api-docs (copia exportada en [`docs/openapi.json`](docs/openapi.json))

Haga `POST /auth/login`, copie el `token`, pulse **Authorize** y péguelo. Para volver a exportar el JSON con la API
corriendo:

```bash
curl -o docs/openapi.json http://localhost:8080/v3/api-docs
```

## 11. Pruebas

```bash
./mvnw test
```

67 pruebas JUnit 5 / Mockito, que no necesitan base de datos:

| Clase | Qué cubre |
|---|---|
| `BookingRulesTest` | Duración, horarios, anticipación, capacidad, miembro suspendido, límite del plan, cruces, consecutivas, transiciones |
| `CostAndCancellationTest` | Costos con horas incluidas, excedente y descuento, servicios, precio histórico, penalizaciones 0 % / 50 % / prohibida |
| `BookingServiceImplTest` | Crear reserva y costos, evitar cruces, permitir consecutivas, 45 min, fuera de horario, suspendido, MEMBER reservando para otro, MEMBER viendo o listando reservas ajenas, cancelaciones, RN-09, RN-10 |
| `CatalogDeletionTest` | RN-12 en sala, sede, equipamiento, servicio, plan y miembro |
| `BookingControllerSecurityTest` | 401 sin token, 401 con token alterado, 403 por rol, 200 por rol permitido, 400 por validación |
| `JwtServiceTest` | Emisión y validación del JWT, expiración a los 60 min, rechazo de tokens con caracteres añadidos o payload modificado |

### Escenarios de aceptación (Postman)

Importe `postman/CoWorkHub.postman_collection.json` y ejecute la colección completa en el **Runner**. Antes de cada
ejecución completa reinicie la aplicación, porque los escenarios 5, 6 y 7 usan reservas sembradas a +30 h, +5 h y +1 h.

| # | Escenario | Esperado |
|---|---|---|
| 1 | Reservar sala libre con 2 servicios | 201 con costo |
| 2 | Reserva que se cruza | 409 RN-01 |
| 3 | 10:00–11:00 después de 09:00–10:00 | 201 |
| 4 | 45 min / fuera del horario | 409 RN-02 / RN-03 |
| 5 | Cancelar con 30 h | 200, cargo 0 |
| 6 | Cancelar con 5 h | 200, cargo 50 % (34 000) |
| 7 | Cancelar con 1 h | 409 RN-08 |
| 8 | Confirmar una cancelada | 409 RN-09 |
| 9 | Miembro consulta reserva de otro | 403 |
| 10 | Sin token / token alterado | 401 |
| 11 | Eliminar sala con reservas futuras | 409 RN-12 |
| 12 | Crear sede sin nombre | 400 |
| 13 | Dos solicitudes simultáneas | una 201, otra 409 |
| 14 | `?estado=SUSPENDED` | 200 solo suspendidos |

Sin Postman, los mismos 14 escenarios se ejecutan con PowerShell (incluido el 13 con solicitudes realmente simultáneas):

```powershell
powershell -ExecutionPolicy Bypass -File scripts\acceptance-test.ps1
```

Para someter el escenario 13 a más carga:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\concurrency-test.ps1 -Requests 5
```

---

## 12. Configuración y beans (módulos del curso)

| Concepto | Dónde |
|---|---|
| `@Configuration` + `@Bean` | `BeansConfig`, `SecurityConfig`, `OpenApiConfig` |
| `@Primary` | `BeansConfig.businessClock`: reloj en zona America/Bogota usado por todas las reglas |
| `@Qualifier` | `JwtService` recibe `@Qualifier("utcClock")`, porque `iat`/`exp` del JWT son instantes UTC |
| `@Value` | `AppProperties` (zona horaria, secreto y duración del JWT) |
| `@PostConstruct` | `AppProperties.validate()`: impide arrancar si la zona es inválida, el secreto JWT tiene menos de 256 bits o la expiración no está entre 1 y 60 min |
| `@Transactional` | Servicios (`readOnly` por defecto; `READ_COMMITTED` al crear reservas) |
| `Clock` / `PasswordEncoder` | Beans en `BeansConfig` (`BCryptPasswordEncoder`) |
| Inyección por constructor | En todas las clases |
