# MyStreeT (Foundation) — Sneaker Shopping Web Application

> **Estado del documento:** Sprint 1 completado (catálogo conectado a la API).
> Los comandos y el registro de prompts se actualizan al cierre de cada sprint.

## 1. Descripción del proyecto

MyStreeT es una aplicación web full-stack de tienda de sneakers que demuestra fundamentos sólidos de desarrollo web: catálogo de productos con filtros, detalle de producto, carrito de compras persistente, flujo de checkout con pago simulado y un panel administrativo sencillo para la gestión del catálogo (CRUD).

**Alcance deliberadamente simple (Foundation):**

- Sin pasarelas de pago reales: se usa pago simulado ("Cash on Delivery" o "Mock UPI").
- Sin notificaciones, reportes ni reglas avanzadas de inventario: solo se decrementa `stock_qty` al crear un pedido.
- Control de acceso simplificado mediante una bandera booleana `isAdmin` en la entidad `User` (sin RBAC completo).

## 2. Arquitectura y stack tecnológico

| Capa | Tecnología | Responsabilidad principal |
|---|---|---|
| Frontend | Angular + TypeScript + SCSS | Vistas de catálogo, detalle, carrito, checkout, autenticación y panel admin; Routing, Services, Reactive Forms, HttpInterceptor y Route Guards. |
| Backend | Java 17+ y Spring Boot 3 | API REST (Controllers, Services, DTOs), validación de entradas (`@Valid`), manejo consistente de errores JSON y hashing de contraseñas con BCrypt. |
| Base de datos | PostgreSQL (prod) / H2 (dev) | Persistencia relacional con Spring Data JPA / Hibernate (`User`, `Product`, `Order`, `OrderItem`). |
| Autenticación | JWT stateless (Bearer) | Emisión y verificación de tokens firmados con claims de usuario e indicador `isAdmin`. |
| Estado del carrito | `localStorage` + `CartService` | Persistencia local en el navegador con actualización reactiva de totales. |
| Documentación y pruebas | Swagger/OpenAPI (springdoc), Postman, JUnit 5 + Mockito, JaCoCo | Contratos API documentados, colección Postman y pruebas unitarias backend (≥ 60 % de cobertura a nivel de método). |

## 3. Decisiones de arquitectura y trade-offs

### 3.1 Frontend en Angular

- **Decisión:** interfaz con Angular (TypeScript, Services, Routing, Reactive Forms, Guards e Interceptors).
- **Ventajas:** arquitectura estructurada y opinada de fábrica (inyección de dependencias, tipado fuerte, enrutamiento y manejo HTTP integrados), lo que reduce decisiones dispersas y facilita mantener estándares claros.
- **Trade-offs:** más archivos y boilerplate inicial que con bibliotecas más ligeras, y una curva de aprendizaje inicial (componentes standalone, servicios, signals e interceptores).

### 3.2 Backend en Spring Boot

- **Decisión:** API REST con Spring Boot 3 sobre Java 17, organizada por capas (`web` → `service` → `repository`) con DTOs de entrada y salida.
- **Ventajas:** ecosistema maduro para seguridad (Spring Security), persistencia (Spring Data JPA) y validación (Bean Validation); convenciones claras y buen soporte de pruebas con JUnit 5 y Mockito.
- **Trade-offs:** más configuración y mayor consumo de memoria al arrancar que opciones más ligeras, y el modelo de seguridad de Spring tiene curva de aprendizaje propia.

### 3.3 Autenticación con JWT stateless

- **Decisión:** JSON Web Tokens sin estado enviados en el encabezado `Authorization: Bearer <token>`, almacenados en `localStorage` en el frontend.
- **Ventajas:**
  - **Desacoplamiento cliente-servidor:** el backend no necesita almacenar sesiones para validar cada solicitud, lo que simplifica la arquitectura REST y la escalabilidad horizontal.
  - **Integración limpia en Angular:** un `HttpInterceptor` adjunta el token a cada petición protegida y los Route Guards (`authGuard`, `adminGuard`) evalúan la sesión y la bandera `isAdmin`.
- **Trade-offs:**
  - **Revocación manual limitada:** un token válido sigue aceptándose hasta su expiración aunque el usuario cierre sesión en el cliente. Revocarlo antes requeriría una lista de bloqueo (denylist) o tokens de vida corta con refresh tokens, lo cual queda fuera del alcance Foundation.
  - **Almacenamiento en `localStorage`:** sencillo para desarrollo en `localhost` y sobrevive a recargas, pero en producción real es más expuesto a ataques XSS que una cookie `HttpOnly` + `SameSite`.

### 3.4 Carrito de compras en `localStorage`

- **Decisión:** persistir el carrito del lado del cliente, gestionado por un `CartService` reactivo en Angular.
- **Ventajas:**
  - Permite a usuarios invitados agregar productos, cambiar tallas y cantidades y ver el total al instante sin iniciar sesión ni generar tráfico al servidor.
  - Reduce la complejidad del modelo de datos en backend (no hay tablas de carrito temporal).
- **Trade-offs:**
  - El carrito está ligado al navegador y dispositivo; no se sincroniza entre dispositivos ni sobrevive si se borran los datos del navegador.
  - Precios y disponibilidad deben validarse de nuevo en el backend al confirmar el pedido (`POST /api/orders`).

### 3.5 Control de acceso simplificado (`isAdmin`)

- **Decisión:** atributo booleano `isAdmin` en `User` (`false` por defecto; el administrador inicial se carga por seed) en lugar de un sistema RBAC completo.
- **Trade-off:** cumple el alcance mínimo viable con bajo costo de implementación, pero no soporta múltiples roles granulares ni permisos combinables sin refactorizar el modelo de seguridad.

### 3.6 Perfiles de base de datos (H2 en dev, PostgreSQL en prod)

- **Decisión:** H2 en memoria (modo compatible con PostgreSQL) en el perfil `dev` para acelerar el arranque y las pruebas locales, y PostgreSQL en el perfil `prod` para la entrega y validación final.
- **Mitigación de diferencias de dialecto:** los identificadores son UUID generados por JPA (`@GeneratedValue`), de modo que el mismo seed funciona en ambos motores sin depender de funciones SQL propias de PostgreSQL.
- **Trade-offs:**
  - Agiliza el desarrollo temprano, pero las consultas deben validarse contra PostgreSQL antes del cierre.
  - En `prod` el esquema se gestiona con `ddl-auto: update`, suficiente para este alcance; un entorno real usaría migraciones versionadas (Flyway o Liquibase).

### 3.7 Filtros del catálogo con JPA Specifications

- **Decisión:** los filtros opcionales `?brand=&size=` se construyen con `Specification`, combinando solo los criterios recibidos.
- **Ventaja:** evita consultas con parámetros nulos y permite añadir filtros nuevos sin multiplicar métodos de repositorio.
- **Trade-off:** es más abstracto que una consulta JPQL fija, y la búsqueda por talla depende de una colección de elementos (`product_sizes`).

## 4. Estructura del repositorio

```
MyStreet/
├── mystreet-backend/      # Proyecto Spring Boot (Java 17+, JPA, JWT, Swagger)
│   └── src/main/java/com/mystreet/
│       ├── config/        # Seguridad, CORS, seed de datos
│       ├── domain/        # Entidades JPA
│       ├── dto/           # Request / Response / ApiError
│       ├── exception/     # Excepciones de negocio
│       ├── repository/    # Spring Data JPA
│       ├── service/       # Lógica de negocio
│       └── web/           # Controllers y manejo global de errores
├── mystreet-web/          # Proyecto Angular
│   └── src/app/
│       ├── models/        # Interfaces TypeScript
│       ├── services/      # ProductService (HttpClient)
│       ├── product-list/  # Catálogo con filtros por marca y talla
│       └── product-detail/# Detalle de producto
├── postman/               # Colección de Postman exportada (Sprint 3)
├── .vscode/tasks.json     # Tareas de arranque: backend, frontend y "dev: all"
└── README.md
```

## 5. Prerrequisitos y orden de ejecución

**Prerrequisitos locales**

- Java 17 o superior (`java -version`)
- Maven, o el Maven Wrapper incluido en el backend (`mvnw.cmd`)
- Node.js y npm (versiones con las que se desarrolló: Node 24, Angular CLI 22)
- Angular CLI: `npm install -g @angular/cli`
- PostgreSQL (solo para el perfil `prod`; opcional durante el desarrollo con H2)
- Git y Postman

**Orden de ejecución**

1. **Base de datos:** H2 se levanta sola con el perfil `dev` (perfil activo por defecto). Para PostgreSQL, crea la base `mystreet` y arranca con el perfil `prod`.

2. **Backend (Spring Boot)** desde `mystreet-backend/`:

   ```powershell
   mvn spring-boot:run
   # o, con el wrapper:
   .\mvnw.cmd spring-boot:run
   ```

   Para el perfil `prod`, define `DB_URL`, `DB_USER` y `DB_PASSWORD` y añade `-Dspring-boot.run.profiles=prod`.

3. **Seed de datos:** se carga automáticamente al arrancar (es idempotente):
   - Administrador de desarrollo: `admin@mystreet.com` / `Admin123!` (`is_admin = true`).
   - 8 sneakers iniciales con imágenes de relleno (`placehold.co`).

   > ⚠️ La contraseña del administrador es **solo para desarrollo**. No debe reutilizarse ni desplegarse tal cual.

4. **Frontend (Angular)** desde `mystreet-web/`:

   ```powershell
   npm install
   npm start
   ```

**Arranque con un solo comando en VS Code:** `Terminal → Run Task → dev: all` lanza backend y frontend en paralelo.

**URLs locales**

| Servicio | URL |
|---|---|
| Aplicación Angular | http://localhost:4200 |
| API REST | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Consola H2 (dev) | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:mystreet`, usuario `sa`, sin contraseña) |

## 6. Resumen de endpoints (API contract)

La documentación interactiva está disponible en Swagger UI una vez iniciado el backend.

| Módulo | Método y ruta | Autorización | Descripción | Estado |
|---|---|---|---|---|
| Auth | `POST /api/auth/register` | Pública | Registra un nuevo usuario (201 Created). | Sprint 2 |
| Auth | `POST /api/auth/login` | Pública | Autentica credenciales y devuelve JWT (200 OK). | Sprint 2 |
| Products | `GET /api/products` | Pública | Lista sneakers (filtros opcionales `?brand=&size=`). | ✅ Sprint 1 |
| Products | `GET /api/products/{id}` | Pública | Devuelve el detalle de un producto. | ✅ Sprint 1 |
| Products | `POST /api/products` | JWT + `isAdmin` | Crea un producto nuevo. | Implementado; protección en Sprint 2 |
| Products | `PUT /api/products/{id}` | JWT + `isAdmin` | Actualiza un producto existente. | Implementado; protección en Sprint 2 |
| Products | `DELETE /api/products/{id}` | JWT + `isAdmin` | Elimina un producto. | Implementado; protección en Sprint 2 |
| Orders | `POST /api/orders` | JWT (User) | Crea un pedido con estado `PLACED` y decrementa stock. | Sprint 3 |
| Orders | `GET /api/orders/mine` | JWT (User) | Lista los pedidos del usuario autenticado. | Sprint 3 |
| Orders | `GET /api/orders/{id}` | JWT (User) | Muestra el detalle de un pedido específico. | Sprint 3 |

> Hasta el Sprint 2, los endpoints de escritura de productos están abiertos para facilitar las pruebas locales.

**Formato de errores:** las respuestas de error son JSON con `timestamp`, `status`, `error`, `message` y, en validaciones, `fieldErrors` por campo.

## 7. Pruebas, rendimiento y calidad

- **Pruebas unitarias backend:** JUnit 5 + Mockito sobre la capa de servicios (objetivo ≥ 60 % de cobertura a nivel de método). Ejecución: `mvn test`. El reporte de cobertura de JaCoCo se genera en `target/site/jacoco`.
- **Pruebas manuales y de API:** Swagger UI y colección de Postman en `/postman` (Sprint 3).
- **Concurrencia y SLAs locales:** prueba de 5–10 peticiones concurrentes (Postman Runner o múltiples pestañas), verificando < 1 s en llamadas API y < 5 s en la carga inicial en `localhost`.
- **Flujo Git:** rama `main` estable, desarrollo en ramas `feature/*` y revisión mediante Pull Requests.

## 8. Plan por sprints

| Sprint | Alcance | Estado |
|---|---|---|
| **Sprint 1** | Proyecto Spring Boot, entidades JPA, CRUD de productos con filtros y seed; catálogo Angular conectado a la API con filtros por marca y talla, "Limpiar filtros" y detalle de producto. | ✅ Completado |
| **Sprint 2** | Registro y login con JWT y BCrypt, `isAdmin`, interceptor y guards, `CartService` en `localStorage`, validación de entradas y protección de endpoints de administración. | Pendiente |
| **Sprint 3** | `POST /api/orders` con decremento de stock, `GET /api/orders/mine`, checkout con pago simulado, panel admin, colección Postman, pruebas ≥ 60 % y README final. | Pendiente |

## 9. Registro de uso de IA generativa (prompts)

*Completar conforme avancen los sprints (10 % de la rúbrica de evaluación).*

| Fase / Sprint | Herramienta | Propósito | Resumen del prompt utilizado |
|---|---|---|---|
| Fase 0 | Gemini | Planificación y documentación | Creación del plan de actividades paso a paso y borrador inicial del README acoplado a Angular y JWT. |
| Sprint 1 | Claude | Backend, frontend y documentación | Revisión del README y ajuste del stack; scaffold del backend Spring Boot (entidades, CRUD de productos, seed, pruebas unitarias); conexión del catálogo Angular a la API, filtros por marca y talla, detalle de producto; configuración del monorepo en VS Code; primer commit y actualización de este README. |
| Sprint 2 | (Por completar) | (Por completar) | (Por completar) |
| Sprint 3 | (Por completar) | (Por completar) | (Por completar) |
