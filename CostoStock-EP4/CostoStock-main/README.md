# CostoStock

Plataforma móvil Android para **comprar y vender insumos con control de costos y stock**.
Está formada por **dos apps Android** (Kotlin + Jetpack Compose) conectadas a **tres microservicios
Spring Boot** propios y a una **API externa** de indicadores económicos.

> Evaluación Parcial 4 · DSY1105 Desarrollo de Aplicaciones Móviles · Duoc UC · **Equipo N° 4**

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

## 2. Integrantes

| Integrante | Responsabilidad principal |
|---|---|
| **Nicolas Rojas** | App de Proveedores · ms-usuarios · ms-productos |
| **Benjamin Perez** | App de Clientes · ms-pedidos · API externa |

> Ajusten la tabla si se repartieron distinto: debe coincidir con los commits y las tarjetas de Trello.

## 3. Funcionalidades

### App de Clientes
- **Registro e inicio de sesión** contra **ms-usuarios** (la contraseña se guarda en el servidor con BCrypt). Validación por campo, medidor de seguridad y sesión persistente.
- **Catálogo en tiempo real** desde **ms-productos**: se descarga al abrir y se actualiza solo cada 20 s (si un proveedor publica, pausa o se vende stock, el cliente lo ve). Buscador, carrusel de destacados, filtros por categoría/comuna, poco stock y orden por precio. Aviso con botón **Reintentar** si no hay conexión.
- **Indicadores del día** (API externa **mindicador.cl**): tarjeta con dólar, UF y euro en el catálogo; el detalle del producto y el total del carrito muestran su equivalencia en **US$ y UF**.
- **Carrito** (guardado localmente en SQLite) con cantidades limitadas por el stock real, desglose Neto / IVA 19% / Total.
- **Confirmar compra**: se envía a **ms-pedidos**, que valida y descuenta el stock en **ms-productos** (comunicación entre microservicios). Si falta stock, se informa qué productos.
- **Mis pedidos**: historial desde ms-pedidos, **cancelar** una compra (el stock vuelve al proveedor) y **eliminar** pedidos cancelados.
- **Perfil**: foto con la cámara, **editar nombre** (PUT en ms-usuarios), **eliminar cuenta** (DELETE) y cerrar sesión.

### App de Proveedores
- **Registro** (nombre, RUT con dígito verificador, celular, correo, contraseña) e **inicio de sesión** contra **ms-usuarios**.
- **Mis publicaciones** desde **ms-productos**: resumen (activas, pausadas, unidades y valor del inventario, también en **US$ y UF** con la API externa), búsqueda, filtros y acciones **editar, pausar/activar y eliminar**. Se actualiza sola para ver bajar el stock cuando los clientes compran.
- **Asistente de publicación en 4 pasos** (producto, fotos, precio, revisar) que **crea (POST) o edita (PUT)** en ms-productos. En el paso de precio se muestra el desglose de IVA y el equivalente en US$/UF.
- **Perfil**: foto, datos de contacto y **edición de nombre/teléfono** (PUT en ms-usuarios).

### CRUD cubierto desde la interfaz

| Recurso | Create | Read | Update | Delete |
|---|---|---|---|---|
| Clientes (ms-usuarios) | Registro | Login / perfil | Editar nombre | Eliminar cuenta |
| Proveedores (ms-usuarios) | Registro | Login / perfil | Editar datos | — |
| Productos (ms-productos) | Publicar | Catálogo / Mis publicaciones | Editar · Pausar/Activar | Eliminar publicación |
| Pedidos (ms-pedidos) | Finalizar compra | Mis pedidos | Cancelar pedido | Eliminar pedido cancelado |

### Recursos nativos y animaciones
Cámara (fotos de perfil y de productos, con permiso en tiempo de ejecución) y vibración. Transiciones entre pantallas, total animado, sacudida ante errores, progreso del asistente y contadores animados.

## 4. Endpoints utilizados

### Microservicios propios (Spring Boot)

**ms-usuarios — `http://localhost:8081`**

| Método | Endpoint | Uso |
|---|---|---|
| POST | `/api/clientes/registro` | Crear cliente (409 si el correo existe) |
| POST | `/api/clientes/login` | Iniciar sesión (401 si las credenciales no coinciden) |
| GET | `/api/clientes` · `/api/clientes/{id}` | Listar / obtener |
| PUT | `/api/clientes/{id}` | Editar nombre |
| DELETE | `/api/clientes/{id}` | Eliminar cuenta |
| POST | `/api/proveedores/registro` | Crear proveedor (valida RUT, 409 si correo o RUT existen) |
| POST | `/api/proveedores/login` | Iniciar sesión |
| GET | `/api/proveedores` · `/api/proveedores/{id}` | Listar / obtener |
| PUT | `/api/proveedores/{id}` | Editar nombre y teléfono |
| DELETE | `/api/proveedores/{id}` | Eliminar proveedor |

**ms-productos — `http://localhost:8082`**

| Método | Endpoint | Uso |
|---|---|---|
| GET | `/api/productos?proveedorId=&categoria=&comuna=&soloDisponibles=` | Catálogo y publicaciones de un proveedor |
| GET | `/api/productos/{id}` | Obtener publicación |
| POST | `/api/productos` | Crear publicación |
| PUT | `/api/productos/{id}` | Editar publicación |
| PATCH | `/api/productos/{id}/estado` | Pausar / activar (`{"estado":"PAUSED"}`) |
| DELETE | `/api/productos/{id}` | Eliminar publicación |
| POST | `/api/productos/stock/descontar` | Descuenta stock en una transacción (409 si falta) — lo usa ms-pedidos |
| POST | `/api/productos/stock/reponer` | Devuelve stock al cancelar — lo usa ms-pedidos |

**ms-pedidos — `http://localhost:8083`**

| Método | Endpoint | Uso |
|---|---|---|
| POST | `/api/pedidos` | Confirmar compra (llama a ms-productos para precio y stock) |
| GET | `/api/pedidos?clienteId=` | Historial del cliente |
| GET | `/api/pedidos/{id}` | Obtener pedido |
| PATCH | `/api/pedidos/{id}/cancelar` | Cancelar (repone stock en ms-productos) |
| DELETE | `/api/pedidos/{id}` | Eliminar pedido cancelado |

Cada microservicio tiene documentación interactiva en **`/swagger-ui.html`** y una colección Postman en `backend/CostoStock.postman_collection.json`.

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
En tres terminales (o con `backend/iniciar-microservicios.bat` en Windows / `.sh` en Mac-Linux):

```bash
cd backend/ms-usuarios  && mvn spring-boot:run
cd backend/ms-productos && mvn spring-boot:run
cd backend/ms-pedidos   && mvn spring-boot:run
```

En IntelliJ también se puede abrir cada carpeta y ejecutar la clase `Ms…Application`.

- La base de datos **H2** se guarda en `backend/<servicio>/data/` (persiste entre reinicios). Consola web: `http://localhost:808X/h2-console` (JDBC URL `jdbc:h2:file:./data/<servicio>`, usuario `sa`, sin contraseña).
- ms-productos carga un **catálogo inicial de 14 productos** la primera vez.
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

- Con los microservicios encendidos, basta con **abrir el archivo en el navegador** (doble clic).
- Si el backend corre en otro PC: agregar `?host=IP_DEL_PC` al final de la URL del archivo.
- Usa `GET /api/clientes`, `GET /api/proveedores` (ms-usuarios), `GET /api/productos` y `GET /api/pedidos`.
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

## GitHub y Trello
- Planificación de tareas y reparto: [`docs/trello-planificacion.md`](docs/trello-planificacion.md) (tarjetas listas para copiar al tablero).
- Convención de commits y ramas descrita en el mismo documento.

## Tecnologías
Kotlin · Jetpack Compose · Material 3 · Navigation Compose · ViewModel/StateFlow · Retrofit · OkHttp · Gson · SQLite · JUnit · MockK · JaCoCo · Java 17 · Spring Boot 3 · Spring Data JPA · H2 · MySQL · Swagger (springdoc)
