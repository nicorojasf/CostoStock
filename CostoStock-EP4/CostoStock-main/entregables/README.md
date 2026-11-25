# Entregables

Esta carpeta reúne lo que pide la pauta para la entrega en GitHub y AVA.

| Archivo | Cómo obtenerlo |
|---|---|
| `CostoStock-Clientes-release.apk` | En `CostoStock/`: `./gradlew assembleRelease` y copiar `app/build/outputs/apk/release/app-release.apk` aquí con este nombre. |
| `CostoStock-Proveedores-release.apk` | Igual, en `CostoStockProveedor/CostoStockProveedor/`. |
| Llaves `.jks` | Ya están en `CostoStock/keystore/` y `CostoStockProveedor/CostoStockProveedor/keystore/` (configuración en `keystore.properties` y `app/build.gradle.kts`). |
| `capturas/apk-firmado.png` | Captura de la carpeta con el APK generado o del resultado de `apksigner verify --print-certs`. |
| `capturas/jks.png` | Captura de la carpeta `keystore/` con el archivo `.jks` (o del diálogo *Generate Signed APK* usándolo). |
| `capturas/trello.png` | Captura del tablero de Trello con las tareas repartidas. |
| `capturas/cobertura.png` | Captura del reporte `app/build/reports/jacoco/jacocoTestReport/html/index.html`. |

Comandos útiles (desde la carpeta de cada app):

```bash
./gradlew testDebugUnitTest jacocoTestReport   # pruebas + cobertura
./gradlew assembleRelease                      # APK firmado
```

`apksigner` está en `<Android SDK>/build-tools/<versión>/`.
