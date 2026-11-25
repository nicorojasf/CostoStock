# Guía para la defensa individual (15 minutos)

La defensa es **individual**: cada uno debe poder ejecutar el proyecto, explicar cualquier parte y
**modificar código en vivo**. Si el proyecto no se ejecuta, los ítems siguientes quedan en 0, así que
practiquen el arranque completo varias veces.

## 1. Arranque (hacerlo antes de entrar)
1. Levantar los 3 microservicios (`backend/iniciar-microservicios`). Verificar `http://localhost:8081/swagger-ui.html`.
2. Abrir el emulador y fijar ubicación (Extended controls → Location: -33.4378, -70.6504).
3. Instalar ambas apps desde Android Studio.
4. Abrir el panel web e iniciar sesión como Admin.

## 2. Guion de demostración sugerido (≈ 6 min)
1. **Proveedor**: registro (RUT `12.345.678-5`), publicar con foto (cámara) y "Usar mi ubicación" para la comuna (GPS). Mostrar US$/UF en el precio (API externa).
2. **Cliente**: registro, catálogo con indicadores, "Usar mi ubicación", agregar al carrito, confirmar compra.
3. **Proveedor**: ver bajar el stock (se actualiza solo).
4. **Cliente**: Mis pedidos → cancelar (el stock vuelve). Perfil → editar nombre.
5. **Recuperar contraseña**: "¿Olvidaste tu contraseña?" → leer el código en la consola de ms-usuarios → cambiarla → iniciar sesión.
6. **Panel**: entrar como Soporte (no puede eliminar, botones deshabilitados) y como Admin (elimina una publicación, crea una cuenta de Soporte).
7. **Seguridad**: en Postman, eliminar un producto con token de Soporte → **403**; sin token → **401**.

## 3. Conceptos que deben poder explicar

### Arquitectura
- **MVVM**: `Screen` (Compose, solo dibuja) → `ViewModel` (estado en `StateFlow`, validaciones con `Validators`) → `Repository` (decide si va a Retrofit o a SQLite) → `Api`/`Dao`.
- **Inyección de dependencias manual**: `AppContainer` crea repositorios y se los pasa a los ViewModels mediante `AppViewModelProvider`.
- **Backend**: `Controller` (HTTP + autorización) → `Service` (reglas de negocio, `@Transactional`) → `Repository` (Spring Data JPA) → BD.
- **Comunicación entre microservicios**: ms-pedidos llama a ms-productos con `RestClient` usando un token del rol interno SISTEMA. Si guardar el pedido falla, repone el stock (compensación).

### POO (IE1.3.1)

| Concepto | Ejemplo en el código |
|---|---|
| Clase abstracta | `PerfilAcceso` (backend, carpeta `security/`) |
| Herencia | `PerfilCliente`, `PerfilProveedor`, `PerfilSoporte` extienden `PerfilAcceso`; **`PerfilAdmin` extiende `PerfilSoporte`** (multinivel) |
| Polimorfismo | Cada subclase redefine `permisos()` y `descripcion()`; el controlador solo llama `perfil.exigir(Permiso.X)` sin saber qué rol es. `PerfilAdmin.permisos()` usa `super.permisos()` |
| Interfaz | `NotificadorCorreo` con la implementación `LogNotificadorCorreo` (se podría cambiar por SMTP sin tocar `RecuperacionService`) |
| Encapsulamiento | Entidades con atributos privados y getters/setters; `SessionManager.token` con `private set` |
| Jerarquías selladas (Kotlin) | `AuthResult`, `CheckoutResult`, `SyncState`, `Outcome`: el `when` obliga a tratar cada caso |
| Excepciones | Backend: `NotFoundException` (404), `ConflictException` (409), `NoAutenticadoException` (401), `AccesoDenegadoException` (403), `StockInsuficienteException` (409) → `GlobalExceptionHandler`. Apps: `try/catch` de `IOException` (sin red) y errores HTTP en los repositorios, `ApiException` |

### Roles y seguridad
- Al iniciar sesión, ms-usuarios devuelve un **token firmado con HMAC-SHA256** (`TokenService`): contiene id, rol y vencimiento. Los 3 servicios comparten el secreto y verifican la firma sin consultar la base.
- Las apps guardan el token en `SessionManager` y un `Interceptor` de OkHttp lo agrega en cada llamada. A la API externa **no** se le envía el token.
- "Dueño": `exigirDuenoO(Rol.CLIENTE, id, Permiso.X)` compara **rol e id** (los ids de clientes y proveedores se pueden repetir).
- Las contraseñas y los códigos de recuperación se guardan con **BCrypt**.

### Recuperación de contraseña
`POST /api/recuperacion/solicitar` → código aleatorio (`SecureRandom`) de 6 dígitos, guardado cifrado, vence en 10 min.
`POST /api/recuperacion/restablecer` → máximo 5 intentos; `noRollbackFor` hace que el contador de intentos se guarde aunque la respuesta sea error.
Por seguridad, si el correo no existe se responde igual (no revela qué correos están registrados).

### GPS
`LocationHelper` usa `LocationManager` del sistema (sin Google Play Services): última ubicación conocida o una lectura nueva con tiempo límite. `ComunaLocator` calcula la comuna más cercana con la fórmula de **Haversine** contra el centro de cada comuna (funciona sin internet y está probado con pruebas unitarias).

### Pruebas
- MockK reemplaza las APIs, DAOs y la sesión para probar repositorios y ViewModels sin red ni emulador.
- `MainDispatcherRule` reemplaza `Dispatchers.Main`; `runTest` controla el tiempo virtual.
- JaCoCo mide solo la lógica (se excluyen pantallas, tema y SQLite).

## 4. Modificaciones en vivo que podrían pedir

| Pedido | Dónde se cambia |
|---|---|
| Cambiar el IVA | `domain/pricing/PriceCalculator.kt`, `domain/cart/CartCalculator.kt` y `PedidoService.IVA_PERCENT` |
| Dar a Soporte permiso para eliminar publicaciones | `PerfilSoporte.permisos()` en `security/PerfilAcceso.java` de **ms-productos** (y en el panel `PERMISOS.SOPORTE`) |
| Agregar un rol nuevo (ej. Repartidor) | `Rol.java`, nueva subclase de `PerfilAcceso` y su caso en `PerfilAcceso.de(...)` |
| Cambiar la vigencia del código | `RecuperacionService.MINUTOS_VIGENCIA` |
| Cambiar cada cuánto se actualiza el catálogo | `ProductRepository.AUTO_REFRESH_MS` |
| Agregar una validación a un campo | `domain/validation/Validators.kt` (+ prueba en `ValidatorsTest`) |
| Cambiar el radio de "cerca de mí" | `HomeViewModel.onLocationDetected` (`maxKm = 15.0`) |
| Agregar un campo al producto | Entidad `Producto` + `Dtos` en ms-productos, `ProductoDto` y `Mappers.kt` en las apps |
| Agregar un endpoint | Controlador del microservicio + método en la interfaz Retrofit (`Apis.kt`) + repositorio |

## 5. Preguntas frecuentes
- **¿Por qué SQLite y no solo el backend?** El carrito y la foto son datos del dispositivo; lo compartido vive en los microservicios.
- **¿Qué pasa si se cae ms-productos?** La app muestra el aviso con "Reintentar" y conserva la última lista; ms-pedidos responde 503.
- **¿Qué pasa si falla la API externa?** Simplemente no se muestran las conversiones; nada más se ve afectado.
- **¿Cómo evitan que un cliente borre pedidos ajenos?** El backend compara el id y rol del token con el dueño del pedido (403).
- **¿Por qué H2?** Funciona sin instalar nada; con el perfil `mysql` se usa MySQL sin cambiar código (JPA).
