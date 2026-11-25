# Planificación del equipo (Trello + GitHub)

La rúbrica (IE 3.3.2, 15%) evalúa **evidencia real** de trabajo colaborativo: tarjetas distribuidas
en Trello y commits técnicos de cada integrante en GitHub. Este documento deja la planificación
lista para copiar al tablero. **Los commits y las tarjetas los tienen que crear ustedes**: no sirve
subir todo en un solo commit al final.

## Tablero
Columnas: **Backlog · Por hacer · En progreso · En revisión · Hecho**.
Etiquetas sugeridas: `Backend` (azul), `App Clientes` (verde), `App Proveedores` (naranjo),
`Pruebas` (morado), `Entrega` (rojo).

Cada tarjeta debe tener: responsable, fecha límite, checklist y el enlace al commit o Pull Request.

## Tarjetas (EP4)

| # | Tarjeta | Etiqueta | Responsable |
|---|---|---|---|
| 1 | Crear estructura `backend/` y repositorio con ramas `main` / `develop` | Backend | Nicolas |
| 2 | ms-usuarios: entidades Cliente y Proveedor, BCrypt, registro/login | Backend | Nicolas |
| 3 | ms-usuarios: CRUD (GET, PUT, DELETE), validaciones y manejo de errores | Backend | Nicolas |
| 4 | ms-productos: entidad Producto, CRUD y cambio de estado | Backend | Nicolas |
| 5 | ms-productos: descontar/reponer stock transaccional + catálogo inicial | Backend | Nicolas |
| 6 | ms-pedidos: entidades Pedido/PedidoItem y cálculo de IVA | Backend | Benjamin |
| 7 | ms-pedidos: comunicación con ms-productos (RestClient) y cancelación | Backend | Benjamin |
| 8 | Pruebas del backend (servicios con Mockito + integración con MockMvc) | Pruebas | Ambos |
| 9 | Perfil MySQL, docker-compose, Swagger y colección Postman | Backend | Benjamin |
| 10 | App Clientes: Retrofit, DTOs y repositorios remotos (usuarios, productos) | App Clientes | Benjamin |
| 11 | App Clientes: checkout contra ms-pedidos y pantalla "Mis pedidos" | App Clientes | Benjamin |
| 12 | App Clientes: editar nombre y eliminar cuenta | App Clientes | Benjamin |
| 13 | API externa mindicador.cl: Retrofit, repositorio y tarjeta de indicadores | App Clientes | Benjamin |
| 14 | App Proveedores: Retrofit, login/registro contra ms-usuarios | App Proveedores | Nicolas |
| 15 | App Proveedores: CRUD de publicaciones contra ms-productos | App Proveedores | Nicolas |
| 16 | App Proveedores: equivalencia US$/UF en resumen y asistente | App Proveedores | Nicolas |
| 17 | Pruebas unitarias app Clientes (ViewModel + Repository con MockK) y JaCoCo ≥ 80% | Pruebas | Benjamin |
| 18 | Pruebas unitarias app Proveedores (ViewModel + Repository con MockK) y JaCoCo ≥ 80% | Pruebas | Nicolas |
| 19 | Generar llaves .jks, firma en build.gradle y APK firmados | Entrega | Ambos |
| 20 | README final, capturas del APK/.jks y entrega en AVA | Entrega | Ambos |

## Flujo de trabajo en GitHub
1. Cada tarjeta = una rama: `feature/12-editar-nombre-cliente`.
2. Commits pequeños y descriptivos que nombran la tarjeta:
   - `feat(ms-productos): descontar stock transaccional (#5)`
   - `feat(app-clientes): pantalla Mis pedidos con cancelar y eliminar (#11)`
   - `test(app-proveedores): ProductRepository con MockK (#18)`
   - `fix(ms-pedidos): reponer stock si falla el guardado (#7)`
3. Pull Request a `develop`, revisión del compañero y merge. Al final, merge de `develop` a `main`.
4. Mover la tarjeta a **Hecho** con el enlace al PR.

> Importante: la rúbrica penaliza la "participación desigual". Que cada uno haga commits desde su
> propia cuenta de GitHub, repartidos en los días de trabajo, y que ambos aparezcan en *Insights → Contributors*.
> No modifiquen el repositorio después de la fecha límite (la pauta asigna 1.0 automáticamente).

## Evidencia para la entrega
- Captura del tablero con las columnas y tarjetas asignadas (guardar en `entregables/capturas/trello.png`).
- Captura de *Insights → Contributors* o del historial de commits por integrante.

## Tarjetas EFT (agregar al tablero)

| # | Tarjeta | Etiqueta | Responsable |
|---|---|---|---|
| 21 | Roles: `PerfilAcceso` con subclases y tokens firmados en los 3 microservicios | Backend | Nicolas |
| 22 | Cuentas del equipo (Soporte/Admin) y endpoints `/api/equipo` | Backend | Nicolas |
| 23 | Recuperación de contraseña (`/api/recuperacion`) con código de 6 dígitos | Backend | Benjamin |
| 24 | Apps: token en `SessionManager` + interceptor de OkHttp | App Clientes / Proveedores | Ambos |
| 25 | Pantalla "¿Olvidaste tu contraseña?" en ambas apps | App Clientes / Proveedores | Benjamin |
| 26 | GPS: `LocationHelper` + `ComunaLocator` (clientes: cerca de mí; proveedores: comuna del producto) | App Clientes / Proveedores | Nicolas |
| 27 | Panel web: login y gestión por rol | Backend | Benjamin |
| 28 | Pruebas de roles, recuperación y GPS | Pruebas | Ambos |
| 29 | README EFT, capturas y ensayo de defensa | Entrega | Ambos |
