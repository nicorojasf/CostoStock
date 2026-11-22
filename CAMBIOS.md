# Cambios respecto a las versiones anteriores

Base: la versión `CostoStock-main` (estructura que coincide con el README), más lo mejor de `CostoStock-master`.

## Recuperado de la versión anterior
- Ícono personalizado de la app de cliente (se había perdido) y `ic_launcher-playstore.png`.
- Nombres de integrantes en el README del proveedor (estaban como "Nombre Apellido").
- `.gitignore` del proveedor y de su módulo `app`.

## Limpieza
- Eliminados `local.properties` y la carpeta `build/` (no deben subirse). Si siguen en tu repo: `git rm -r --cached build` y `git rm --cached local.properties`.
- `.gitignore` raíz ahora ignora `hs_err_pid*.log`, `replay_pid*.log` y `*.hprof`.
- Panel de administración movido a `docs/panel-administracion.html` (nombre sin caracteres raros) y descrito en el README.
- Quitados `ExampleUnitTest` y `ExampleInstrumentedTest` (plantillas vacías) de la app de cliente.

## Mejoras de código (ambas apps salvo que se indique)
- `minSdk` 24 → 26. Con eso el ícono adaptativo funciona en todos los dispositivos soportados y se puede usar SHA-256.
- `PasswordHasher`: PBKDF2-HMAC-SHA256 con 210.000 iteraciones. Las cuentas antiguas (SHA-1) siguen pudiendo iniciar sesión.
- Tests nuevos del hasher: compatibilidad con hashes antiguos.
- `LoginViewModel`: tras 5 intentos fallidos seguidos, bloqueo de 30 s (límite básico a nivel de UI).
- Proveedor: las fotos temporales de la cámara se borran al quitarlas o al abandonar el asistente sin publicar.

## Documentación
- README raíz: requisitos, enlaces a docs y limitaciones conocidas.
- `contrato-datos.md`: aclara que dos apps no pueden compartir un SQLite y propone backend.
- `tareas-trello.md`: marcado lo hecho y agregadas las tareas pendientes de conexión.

## No incluido (a propósito)
- No actualicé versiones de dependencias (`core-ktx`, `activity-compose`): no pude compilar aquí y prefiero no tocarlas a ciegas.
- No renombré el paquete `myapplication` del cliente: es un cambio grande que conviene hacer con el refactor de Android Studio.
- **No pude compilar ni ejecutar los tests** (sin acceso a Gradle). La lógica del hash la comprobé aparte; el resto de los cambios Kotlin son pequeños, pero haz `Build > Rebuild` y `./gradlew test` antes de entregar.
