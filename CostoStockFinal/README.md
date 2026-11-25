# CostoStock

Plataforma móvil Android para **comprar y vender insumos con control de costos y stock**.
Está formada por **dos apps Android** (Kotlin + Jetpack Compose) conectadas a **tres microservicios
Spring Boot** propios, a una **API externa** de indicadores económicos y a un **panel web** para el equipo interno.
Maneja **4 roles de usuario**: Cliente, Proveedor, Soporte y Administrador.

> Evaluación Final Transversal · DSY1105 Desarrollo de Aplicaciones Móviles · Duoc UC · **Equipo N° 4**

## 1. Nombre del proyecto

**CostoStock** — app de Clientes (`CostoStock/`) y app de Proveedores (`CostoStockProveedor/CostoStockProveedor/`).

| Componente | Para quién | Carpeta | Tecnología |
|---|---|---|---|
| App **CostoStock** | Quien compra | `CostoStock/` | Kotlin · Compose · Retrofit |
| App **CostoStock Proveedores** | Quien vende | `CostoStockProveedor/CostoStockProveedor/` | Kotlin · Compose · Retrofit |
| **ms-usuarios** (puerto 8081) | Cuentas de clientes y proveedores | `backend/ms-usuarios/` | Spring Boot · JPA · BCrypt |
| **ms-productos** (puerto 8082) | Publicaciones y stock | `backend/ms-productos/` | Spring Boot · JPA |
| **ms-pedidos** (puerto 8083) | Compras (llama a ms-productos) | `backend/ms-pedidos/` | Spring Boot · JPA · RestClient |
| API externa **mindicador.cl** | Dólar, UF, euro del día | https://mindicador.cl/api | Retrofit + Gson |
| **Panel de administración** | Soporte y Administrador | `CostoStock · Panel de administración.html` | HTML + React (sin instalación) |

## 2. Integrantes

| Integrante | Responsabilidad principal |
|---|---|
| **Nicolas Rojas** | App de Proveedores · ms-usuarios · ms-productos |
| **Benjamin Perez** | App de Clientes · ms-pedidos · API externa |

> Ajusten la tabla si se repartieron distinto: debe coincidir con los commits de cada uno.

### Evidencia de trabajo colaborativo (commits por persona)
- Historial de commits: _pegar enlace a_ `https://github.com/<usuario>/<repo>/commits`
- Aportes por integrante: _pegar enlace a_ `https://github.com/<usuario>/<repo>/graphs/contributors` · captura en `entregables/capturas/commits.png`

| Integrante | Commits | Áreas principales |
|---|---|---|
| Nicolas Rojas | _n_ | _completar_ |
| Benjamin Perez | _n_ | _completar_ |

Para obtener los números: `git shortlog -sn --all`.

## 3. Funcionalidades

### Roles de usuario (4) y privilegios

| Rol | Dónde trabaja | Puede | No puede |
|---|---|---|---|
| **Cliente** | App CostoStock | Comprar, ver/cancelar/eliminar **sus** pedidos, editar o eliminar **su** cuenta | Publicar, ver datos de otros |
| **Proveedor** | App Proveedores | Crear, editar, pausar y eliminar **sus** publicaciones, editar su perfil | Comprar, tocar publicaciones ajenas |
| **Soporte** | Panel web | Ver todo (usuarios, publicaciones, pedidos), **pausar/activar** cualquier publicación, **cancelar** pedidos | Eliminar registros, gestionar el equipo |
| **Administrador** | Panel web | Todo lo de Soporte **+ eliminar** publicaciones, pedidos y cuentas **+ crear/eliminar** cuentas del equipo | Eliminar su propia cuenta |

Los permisos se validan **en el backend** con un token firmado (HMAC-SHA256) que entrega ms-usuarios al iniciar sesión
y que las apps envían en `Authorization: Bearer …`. Sin token → **401**; con un rol sin permiso → **403**.
Cada rol es una subclase de `PerfilAcceso` (herencia y polimorfismo, ver `docs/defensa.md`).


### App de Clientes
- **Registro e inicio de sesión** contra **ms-usuarios** (la contraseña se guarda en el servidor con BCrypt). Validación por campo con íconos y mensajes, medidor de seguridad y sesión persistente con token.
- **Recuperar contraseña**: código de 6 dígitos que vence en 10 minutos (máx. 5 intentos) y cambio de contraseña en dos pasos animados.
- **"Usar mi ubicación"** (GPS): detecta la comuna más cercana con productos y filtra el catálogo.
- **Catálogo en tiempo real** desde **ms-productos**: se descarga al abrir y se actualiza solo cada 20 s (si un proveedor publica, pausa o se vende stock, el cliente lo ve). Buscador, carrusel de destacados, filtros por categoría/comuna, poco stock y orden por precio. Aviso con botón **Reintentar** si no hay conexión.
- **Indicadores del día** (API externa **mindicador.cl**): tarjeta con dólar, UF y euro en el catálogo; el detalle del producto y el total del carrito muestran su equivalencia en **US$ y UF**.
- **Carrito** (guardado localmente en SQLite) con cantidades limitadas por el stock real, desglose Neto / IVA 19% / Total.
- **Confirmar compra**: se envía a **ms-pedidos**, que valida y descuenta el stock en **ms-productos** (comunicación entre microservicios). Si falta stock, se informa qué productos.
- **Mis pedidos**: historial desde ms-pedidos, **cancelar** una compra (el stock vuelve al proveedor) y **eliminar** pedidos cancelados.
- **Perfil**: foto con la cámara, **editar nombre** (PUT en ms-usuarios), **eliminar cuenta** (DELETE) y cerrar sesión.

### App de Proveedores
- **Registro** (nombre, RUT con dígito verificador, celular, correo, contraseña), **inicio de sesión** y **recuperación de contraseña** contra **ms-usuarios**.
- **Mis publicaciones** desde **ms-productos**: resumen (activas, pausadas, unidades y valor del inventario, también en **US$ y UF** con la API externa), búsqueda, filtros y acciones **editar, pausar/activar y eliminar**. Se actualiza sola para ver bajar el stock cuando los clientes compran.
- **Asistente de publicación en 4 pasos** (producto, fotos, precio, revisar) que **crea (POST) o edita (PUT)** en ms-productos. En el paso de precio se muestra el desglose de IVA y el equivalente en US$/UF, y el botón **"Usar mi ubicación"** completa la comuna con el GPS.
- **Perfil**: foto, datos de contacto y **edición de nombre/teléfono** (PUT en ms-usuarios).

### Panel web (Soporte y Administrador)
Inicio de sesión del equipo, resumen en vivo (ventas, gráficos, alertas) y pestañas de **Publicaciones**, **Pedidos** y
**Usuarios** con acciones según el rol (ver sección *Panel de administración*).

### CRUD cubierto desde la interfaz

| Recurso | Create | Read | Update | Delete |
|---|---|---|---|---|
| Clientes (ms-usuarios) | Registro | Login / perfil | Editar nombre · Recuperar contraseña | Eliminar cuenta · Panel (Admin) |
| Proveedores (ms-usuarios) | Registro | Login / perfil | Editar datos · Recuperar contraseña | Panel (Admin) |
| Equipo interno (ms-usuarios) | Panel (Admin) | Panel | — | Panel (Admin) |
| Productos (ms-productos) | Publicar | Catálogo / Mis publicaciones | Editar · Pausar/Activar | Eliminar publicación |
| Pedidos (ms-pedidos) | Finalizar compra | Mis pedidos | Cancelar pedido | Eliminar pedido cancelado |

### Recursos nativos (3)

| Recurso | Uso | Permiso / seguridad |
|---|---|---|
| **Cámara** | Foto de perfil (ambas apps) y fotos de productos (proveedores) | `CAMERA` pedido en tiempo de ejecución; fotos en almacenamiento privado vía `FileProvider` |
| **Ubicación (GPS / red)** | Detectar la comuna: filtro "cerca de mí" (clientes) y comuna del producto (proveedores) | `ACCESS_FINE/COARSE_LOCATION` en tiempo de ejecución; se acepta ubicación aproximada; las coordenadas **no se guardan ni se envían**, solo se usa el nombre de la comuna |
| **Vibración** | Retroalimentación háptica en errores de formulario y acciones exitosas | `VIBRATE` (permiso normal) |

> En el emulador, define una ubicación en *Extended controls → Location* (por ejemplo Plaza de Armas: -33.4378, -70.6504).

### Animaciones
Transiciones entre pantallas, pasos animados en el asistente y en "recuperar contraseña", total animado, sacudida ante
errores, barras de progreso, aparición de tarjetas y contadores animados (también en el panel web).

### Almacenamiento local vs externo

| Dónde | Qué se guarda | Por qué ahí |
|---|---|---|
| **SQLite** (teléfono) | Carrito (clientes), copia local del perfil y **foto de perfil** | Son datos del dispositivo: el carrito es un borrador personal y la foto no necesita salir del teléfono |
| **SharedPreferences** | id de sesión y **token** firmado | Mantener la sesión abierta sin guardar la contraseña |
| **Bases de datos de los microservicios** (H2 / MySQL) | Usuarios, equipo, códigos de recuperación, publicaciones, stock y pedidos | Son datos compartidos entre apps y usuarios: deben ser la única fuente de verdad |

## 4. Endpoints utilizados

### Microservicios propios (Spring Boot)

Columna **Acceso**: 🌐 público · 🔑 requiere token · *Dueño* = solo sobre sus propios datos.

**ms-usuarios — `http://localhost:8081`**

| Método | Endpoint | Acceso | Uso |
|---|---|---|---|
| POST | `/api/clientes/registro` | 🌐 | Crear cliente → devuelve token (409 si el correo existe) |
| POST | `/api/clientes/login` | 🌐 | Iniciar sesión → devuelve token y rol (401 si no coincide) |
| GET | `/api/clientes` | 🔑 Soporte, Admin | Listar clientes |
| GET | `/api/clientes/{id}` | 🔑 Dueño, Soporte, Admin | Obtener cliente |
| PUT | `/api/clientes/{id}` | 🔑 Dueño, Admin | Editar nombre |
| DELETE | `/api/clientes/{id}` | 🔑 Dueño, Admin | Eliminar cuenta |
| POST | `/api/proveedores/registro` · `/login` | 🌐 | Crear proveedor (valida RUT) / iniciar sesión |
| GET | `/api/proveedores` · `/{id}` | 🔑 Soporte, Admin (o Dueño) | Listar / obtener |
| PUT · DELETE | `/api/proveedores/{id}` | 🔑 Dueño, Admin | Editar / eliminar |
| POST | `/api/recuperacion/solicitar` | 🌐 | Genera un código de 6 dígitos (simulación de correo en la consola) |
| POST | `/api/recuperacion/restablecer` | 🌐 | Cambia la contraseña con el código (vence en 10 min, 5 intentos) |
| POST | `/api/equipo/login` | 🌐 | Login de Soporte / Admin (panel web) |
| GET | `/api/equipo` | 🔑 Soporte, Admin | Listar el equipo |
| POST · DELETE | `/api/equipo` · `/api/equipo/{id}` | 🔑 Admin | Crear / eliminar cuentas del equipo |

**ms-productos — `http://localhost:8082`**

| Método | Endpoint | Acceso | Uso |
|---|---|---|---|
| GET | `/api/productos?proveedorId=&categoria=&comuna=&soloDisponibles=` | 🌐 | Catálogo y publicaciones de un proveedor |
| GET | `/api/productos/{id}` | 🌐 | Obtener publicación |
| POST | `/api/productos` | 🔑 Proveedor | Crear publicación (el dueño se toma del token) |
| PUT | `/api/productos/{id}` | 🔑 Dueño, Admin | Editar publicación |
| PATCH | `/api/productos/{id}/estado` | 🔑 Dueño, Soporte, Admin | Pausar / activar (`{"estado":"PAUSED"}`) |
| DELETE | `/api/productos/{id}` | 🔑 Dueño, Admin | Eliminar publicación |
| POST | `/api/productos/stock/descontar` · `/reponer` | 🔑 Sistema (ms-pedidos) | Descontar (409 si falta) / reponer stock |

**ms-pedidos — `http://localhost:8083`**

| Método | Endpoint | Acceso | Uso |
|---|---|---|---|
| POST | `/api/pedidos` | 🔑 Cliente (su cuenta) | Confirmar compra (llama a ms-productos con un token de Sistema) |
| GET | `/api/pedidos?clienteId=` | 🔑 Dueño, Soporte, Admin | Historial del cliente (sin `clienteId`: todos, solo Soporte/Admin) |
| GET | `/api/pedidos/{id}` | 🔑 Dueño, Soporte, Admin | Obtener pedido |
| PATCH | `/api/pedidos/{id}/cancelar` | 🔑 Dueño, Soporte, Admin | Cancelar (repone stock en ms-productos) |
| DELETE | `/api/pedidos/{id}` | 🔑 Dueño, Admin | Eliminar pedido cancelado |

Cada microservicio tiene documentación interactiva en **`/swagger-ui.html`** y una colección Postman en
`backend/CostoStock.postman_collection.json` (los requests de login guardan el token automáticamente).

### API externa

| Método | Endpoint | Uso en la app |
|---|---|---|
| GET | `https://mindicador.cl/api` | Dólar, UF, euro y UTM del día. Se consume con **Retrofit** (`IndicadoresApi`) y se muestra en el catálogo, el detalle de producto, el carrito, el asistente de publicación y el resumen del proveedor. Es independiente de los microservicios: si no responde, la app sigue funcionando sin conversiones. |

## 5. Pasos para ejecutar

### Requisitos
- **JDK 17** o superior y **Maven** (o IntelliJ IDEA, que trae Maven integrado).
- **Android Studio** (con el SDK que pide el proyecto) y un emulador o teléfono.
- Opcional: **MySQL 8** (por defecto los microservicios usan H2 y no requieren instalar nada).

### a) Levantar los microservicios
**Windows, sin instalar Maven (recomendado):** doble clic en `backend/iniciar-microservicios.bat`, o desde la terminal de Android Studio:
```powershell
cd backend
.\iniciar-microservicios.bat
```
La primera vez descarga Maven solo (≈10 MB) y usa el Java que trae Android Studio. Abre una ventana por microservicio.

**Con Maven instalado**, en tres terminales (en PowerShell 5 usa `;` en vez de `&&`):

```bash
cd backend/ms-usuarios  && mvn spring-boot:run
cd backend/ms-productos && mvn spring-boot:run
cd backend/ms-pedidos   && mvn spring-boot:run
```

En IntelliJ también se puede abrir cada carpeta y ejecutar la clase `Ms…Application`.

- La base de datos **H2** se guarda en `backend/<servicio>/data/` (persiste entre reinicios). Consola web: `http://localhost:808X/h2-console` (JDBC URL `jdbc:h2:file:./data/<servicio>`, usuario `sa`, sin contraseña).
- ms-productos carga un **catálogo inicial de 14 productos** la primera vez.
- ms-usuarios crea las **cuentas del panel**: `admin@costostock.cl` / `Admin1234` (Administrador) y `soporte@costostock.cl` / `Soporte1234` (Soporte).
- **Recuperar contraseña**: el código de 6 dígitos aparece en la **consola de ms-usuarios** bajo `CORREO SIMULADO`.
- Los tres servicios comparten el secreto de firma de tokens (`costostock.seguridad.secreto`, variable `COSTOSTOCK_SECRETO`); si lo cambian, cámbienlo en los tres.
- **Con MySQL**: `docker compose -f backend/docker-compose.yml up -d` (o un MySQL local) y luego ejecutar cada servicio con `mvn spring-boot:run -Dspring-boot.run.profiles=mysql`. Usuario/clave por variables `DB_USER` / `DB_PASSWORD` (por defecto `root` / vacío).
- Pruebas del backend: `mvn test` dentro de cada microservicio.

### b) Ejecutar las apps
1. Abrir `CostoStock/` (clientes) y/o `CostoStockProveedor/CostoStockProveedor/` (proveedores) en Android Studio y esperar el *Gradle Sync*.
2. **Emulador**: no hay que cambiar nada (la app usa `10.0.2.2`, que es el PC visto desde el emulador).
   **Teléfono físico**: en `gradle.properties` cambiar `costostock.apiHost` por la IP local del PC (ej: `192.168.1.50`) y conectar el teléfono a la misma red Wi-Fi.
3. Ejecutar ▶. Flujo sugerido para la demo:
   1. En **Proveedores**: crear cuenta (RUT de prueba válido `12.345.678-5`) y publicar un artículo.
   2. En **Clientes**: crear cuenta, ver el artículo en el catálogo, agregarlo al carrito y finalizar la compra.
   3. Volver a **Proveedores** y ver que el stock bajó; en **Clientes → Perfil → Mis pedidos**, cancelar la compra y ver que el stock vuelve.

### c) Pruebas unitarias y cobertura (apps)
```bash
./gradlew testDebugUnitTest      # pruebas (JUnit 4 + MockK + kotlinx-coroutines-test)
./gradlew jacocoTestReport       # reporte de cobertura
```
Reporte HTML: `app/build/reports/jacoco/jacocoTestReport/html/index.html`.
Las pruebas cubren **ViewModels, repositorios, mapeos y dominio** (las pantallas Compose, el tema y SQLite se excluyen del cálculo porque no son lógica de negocio). Si el reporte de JaCoCo aparece vacío, usar el de Android Studio: `./gradlew createDebugUnitTestCoverageReport`.

### d) Generar el APK firmado
La firma ya está configurada en `app/build.gradle.kts` (`signingConfigs.release`), con los datos en `keystore.properties` y la llave en `keystore/*.jks`.

```bash
./gradlew assembleRelease
```
APK firmado: `app/build/outputs/apk/release/app-release.apk`. Copiarlo a `entregables/` (ver `entregables/README.md`).
También se puede hacer desde **Build → Generate Signed App Bundle / APK** usando el `.jks` de la carpeta `keystore/` (contraseña y alias en `keystore.properties`).

| App | Llave | Alias | Contraseña |
|---|---|---|---|
| Clientes | `CostoStock/keystore/costostock-clientes.jks` | `costostock-clientes` | `CostoStock2026` |
| Proveedores | `CostoStockProveedor/CostoStockProveedor/keystore/costostock-proveedores.jks` | `costostock-proveedores` | `CostoStock2026` |

> La llave y su contraseña se suben al repositorio solo porque la evaluación lo exige. En una app real nunca se publican.

Verificar la firma: `apksigner verify --print-certs app-release.apk`.

## 6. Captura del APK firmado y .jks

> Reemplazar por las capturas reales después de compilar (ver `entregables/README.md`).

| APK firmado generado | Llave .jks en el proyecto |
|---|---|
| ![APK firmado](entregables/capturas/apk-firmado.png) | ![Llave jks](entregables/capturas/jks.png) |

## Panel de administración (web)

`CostoStock · Panel de administración.html` (raíz del repositorio) es un panel web que lee **en vivo**
los tres microservicios y se actualiza cada 20 s. Muestra ventas, órdenes, ticket promedio, unidades,
clientes y proveedores registrados, publicaciones activas/pausadas/sin stock, ventas por día,
altas de usuarios, ventas por categoría y comuna, usados vs nuevos, productos y proveedores con más
ventas y alertas del catálogo. Filtros por período (7/30/90 días) y categoría.

- Con los microservicios encendidos, basta con **abrir el archivo en el navegador** (doble clic) e **iniciar sesión** con una cuenta del equipo (`admin@costostock.cl` / `Admin1234` o `soporte@costostock.cl` / `Soporte1234`).
- Pestañas: **Resumen** (métricas), **Publicaciones** (pausar/activar; eliminar solo Admin), **Pedidos** (cancelar; eliminar cancelados solo Admin) y **Usuarios** (clientes, proveedores y equipo; eliminar y crear cuentas solo Admin). Las acciones que el rol no permite aparecen deshabilitadas y, además, el backend las rechaza con 403.
- Si el backend corre en otro PC: agregar `?host=IP_DEL_PC` al final de la URL del archivo.
- Usa los endpoints de la sección 4 con el token de la sesión.
  Solo los pedidos **confirmados** cuentan como ventas.
- Si un microservicio no responde, el panel indica cuál y conserva los últimos datos recibidos.

## Arquitectura

```
App (Compose) ── ViewModel (StateFlow) ── Repository ──┬── Retrofit ── ms-usuarios  :8081 ─┐
                                                        ├── Retrofit ── ms-productos :8082 ─┼─ H2 / MySQL
                                                        ├── Retrofit ── ms-pedidos   :8083 ─┘   (ms-pedidos → ms-productos)
                                                        ├── Retrofit ── mindicador.cl (API externa)
                                                        └── SQLite (carrito, sesión y foto de perfil)
```

- **MVVM** con inyección de dependencias manual (`AppContainer`). Las pantallas solo dibujan el estado.
- Cada repositorio traduce los errores HTTP (400, 401, 404, 409, 503) y de red a mensajes claros para el usuario.
- Los microservicios siguen capas **controller → service → repository (JPA)**, validan con Bean Validation y responden errores con un formato JSON común.

## Programación orientada a objetos
Resumen (detalle y ubicación del código en [`docs/defensa.md`](docs/defensa.md)):
- **Herencia y polimorfismo**: `PerfilAcceso` (abstracta) → `PerfilCliente`, `PerfilProveedor`, `PerfilSoporte` → `PerfilAdmin` (herencia multinivel con `super.permisos()`); `NotificadorCorreo` (interfaz) → `LogNotificadorCorreo`; en las apps, jerarquías selladas (`AuthResult`, `CheckoutResult`, `SyncState`, `Outcome`) tratadas con `when`.
- **Excepciones**: jerarquía propia en cada microservicio (`NotFoundException`, `ConflictException`, `AccesoDenegadoException`, `NoAutenticadoException`, `StockInsuficienteException`…) traducidas a HTTP por `@RestControllerAdvice`; en las apps, `ApiException` y manejo de `IOException`/`HttpException` en los repositorios.
- **Arquitectura**: MVVM en las apps (Screen → ViewModel → Repository → Retrofit/SQLite) y capas controller → service → repository en el backend.

## GitHub
- Trabajo colaborativo evidenciado en el historial de commits de ambos integrantes (ver sección 2).
- Convención de ramas y mensajes de commit sugerida en [`docs/trello-planificacion.md`](docs/trello-planificacion.md).

## Tecnologías
Kotlin · Jetpack Compose · Material 3 · Navigation Compose · ViewModel/StateFlow · Retrofit · OkHttp · Gson · SQLite · JUnit · MockK · JaCoCo · Java 17 · Spring Boot 3 · Spring Data JPA · H2 · MySQL · Swagger (springdoc)
