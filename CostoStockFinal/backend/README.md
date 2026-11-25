# Backend CostoStock (microservicios Spring Boot)

| Servicio | Puerto | Responsabilidad | Base de datos |
|---|---|---|---|
| `ms-usuarios` | 8081 | Clientes y proveedores (registro, login con BCrypt, CRUD) | `usuarios` |
| `ms-productos` | 8082 | Publicaciones, estado y stock | `productos` |
| `ms-pedidos` | 8083 | Compras; se comunica con ms-productos para precio y stock | `pedidos` |

Cada servicio es un proyecto Maven independiente (Java 17, Spring Boot 3.4) con capas
`controller → service → repository` y su propia base de datos (patrón *database per service*).

## Ejecutar
```bash
cd ms-usuarios  && mvn spring-boot:run
cd ms-productos && mvn spring-boot:run
cd ms-pedidos   && mvn spring-boot:run
```
O `iniciar-microservicios.bat` (Windows) / `./iniciar-microservicios.sh` (Mac/Linux).
Desde IntelliJ: abrir la carpeta del servicio y ejecutar la clase `Ms…Application`.

- **H2 (por defecto)**: archivo en `./data/`, consola en `http://localhost:808X/h2-console`.
- **MySQL**: `docker compose up -d` en esta carpeta (o un MySQL local) y ejecutar con
  `mvn spring-boot:run -Dspring-boot.run.profiles=mysql`. Las bases se crean solas.
- **Swagger**: `http://localhost:8081/swagger-ui.html` (y 8082, 8083).
- **Postman**: importar `CostoStock.postman_collection.json`.

## Seguridad y roles
- Login/registro devuelven un **token firmado (HMAC-SHA256)** con id, rol y vencimiento (`security/TokenService`).
- Los endpoints protegidos leen `Authorization: Bearer <token>` y validan el permiso con `PerfilAcceso`
  (subclases `PerfilCliente`, `PerfilProveedor`, `PerfilSoporte`, `PerfilAdmin` y el rol interno `PerfilSistema`).
- `401` sin token o token inválido · `403` rol sin permiso.
- Cuentas iniciales del panel: `admin@costostock.cl` / `Admin1234` y `soporte@costostock.cl` / `Soporte1234`.
- Secreto compartido: `costostock.seguridad.secreto` (variable `COSTOSTOCK_SECRETO`), igual en los 3 servicios.

## Recuperación de contraseña
`POST /api/recuperacion/solicitar` → el código aparece en la consola de ms-usuarios (`CORREO SIMULADO`).
`POST /api/recuperacion/restablecer` con el código (vence en 10 minutos, máximo 5 intentos).

## Pruebas
```bash
mvn test
```
Cada servicio tiene pruebas unitarias de su capa de servicio (JUnit 5 + Mockito) y una prueba de
integración que levanta el servicio con H2 en memoria y recorre el CRUD con MockMvc.

## Errores
Todos responden con el mismo formato:
```json
{ "status": 409, "error": "Conflict", "mensaje": "Stock insuficiente",
  "campos": {}, "detalles": ["Lámpara LED"], "timestamp": 1763750000000 }
```
`400` datos inválidos (con `campos`), `401` credenciales, `404` no existe, `409` duplicado o sin stock,
`503` ms-productos no disponible (desde ms-pedidos).
