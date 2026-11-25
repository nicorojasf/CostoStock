# Cambios EP4 y dónde se cumple cada punto de la pauta

| Indicador | Qué se agregó | Dónde mirar |
|---|---|---|
| **IE 3.1.1** App con pantallas y flujos completos (15%) | Pantalla nueva **Mis pedidos** (listar, cancelar, eliminar). Perfil del cliente con **editar nombre** y **eliminar cuenta**. Avisos de "sin conexión" con botón **Reintentar** y estados de carga. | `CostoStock/.../ui/orders/`, `ui/profile/ProfileScreen.kt`, `ui/components/NetworkComponents.kt` |
| **IE 3.1.2** Microservicios con BD y endpoints (15%) | 3 microservicios Spring Boot: **ms-usuarios**, **ms-productos**, **ms-pedidos**. JPA + H2 (o MySQL con perfil), validaciones, manejo de errores, Swagger, pruebas. | `backend/` |
| **IE 3.1.3** Integración app ↔ microservicios, CRUD (15%) | Retrofit en ambas apps. Registro/login, catálogo, publicaciones (POST/PUT/PATCH/DELETE), compras y pedidos. Actualización automática cada 20 s. ms-pedidos se comunica con ms-productos. | `data/remote/`, `data/repository/` de cada app |
| Panel web de administración | Conectado en vivo a los 3 microservicios (antes mostraba datos de ejemplo). | `CostoStock · Panel de administración.html` |
| **IE 3.1.4** API externa (15%) | **mindicador.cl** con Retrofit: tarjeta de indicadores en el catálogo y equivalencias US$/UF en detalle, carrito, asistente de publicación y resumen del proveedor. Si falla, no afecta al resto. | `IndicadoresApi`, `IndicatorsRepository`, `CurrencyConverter` |
| **IE 3.2.1** Pruebas unitarias ≥ 80% (15%) | JUnit 4 + **MockK** + coroutines-test sobre **ViewModels y repositorios** de ambas apps, más mapeos y dominio. Tarea `jacocoTestReport` que mide solo la lógica. Backend con JUnit 5 + Mockito + MockMvc. | `app/src/test/` de cada app, `backend/*/src/test/` |
| **IE 3.3.1** APK firmado + build.gradle + .jks (10%) | Llaves `.jks`, `keystore.properties` y `signingConfigs.release` en `app/build.gradle.kts`. Reglas R8 para Retrofit/Gson. | `keystore/`, `keystore.properties`, `app/build.gradle.kts`, `src/main/keepRules/rules.keep` |
| **IE 3.3.2** GitHub y Trello (15%) | Tablero con 20 tarjetas repartidas y convención de ramas/commits. **Requiere que ustedes creen el tablero y hagan los commits.** | `docs/trello-planificacion.md` |

## Cambios de arquitectura que deben poder explicar en la defensa
- Antes todo vivía en SQLite dentro de cada teléfono. Ahora **los datos compartidos están en los microservicios**: lo que publica el proveedor lo ve el cliente y las compras descuentan stock real.
- En SQLite quedó solo lo que es del dispositivo: el **carrito** (cliente), la **sesión** y la **foto de perfil**.
- Las contraseñas ya no se guardan en el teléfono: **ms-usuarios** las guarda con **BCrypt**.
- Al actualizar, la base local se recrea (versión 3 en clientes, 2 en proveedores) y hay que volver a iniciar sesión con una cuenta del servidor.
- Confirmar una compra es una operación distribuida: ms-pedidos pide a ms-productos que descuente el stock (todo o nada). Si después falla el guardado del pedido, lo devuelve (compensación).
