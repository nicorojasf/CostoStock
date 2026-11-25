# 🛒 CostoStock Proveedores

App Android para que los **proveedores** publiquen y administren los artículos que venden en **CostoStock**.
Es la contraparte de la app de clientes: el proveedor sube sus artículos y el cliente los compra.
Proyecto de la asignatura **DSY1105 – Desarrollo de Aplicaciones Móviles** (Duoc UC).

> La documentación completa del proyecto (microservicios, endpoints, API externa, pruebas y APK
> firmado) está en el [README principal](../../README.md).

## 👥 Integrantes
- Nicolas Rojas — App de Proveedores
- Benjamin Perez — App de Clientes

## ✅ Funcionalidades
- **Registro** de proveedor (nombre del comercio, RUT con dígito verificador, teléfono celular, correo, contraseña con medidor de seguridad y términos).
- **Inicio de sesión** con sesión persistente y cierre de sesión.
- **Mis publicaciones** (inspirado en Mercado Libre): resumen de ventas (activas, pausadas, unidades y valor del inventario), búsqueda, filtros por estado, y acciones para editar, pausar/activar y eliminar.
- **Publicar un artículo** en un asistente de 4 pasos con validación en cada uno:
  1. *Producto*: título, categoría, condición (nuevo/usado) y descripción.
  2. *Fotos*: hasta 3 con la cámara; la primera es la portada.
  3. *Precio*: precio con IVA incluido (con desglose neto + IVA), unidades y comuna.
  4. *Revisar*: vista previa de la publicación antes de publicar.
- **Perfil**: foto con la cámara, datos de contacto, estadísticas y edición de datos.
- **Datos en los microservicios**: cuentas en `ms-usuarios` y publicaciones en `ms-productos` (Retrofit). En SQLite solo queda la copia local del perfil y su foto.
- **API externa** mindicador.cl: equivalencia en US$ y UF del precio y del valor del inventario.
- **Recursos nativos**: cámara (con permiso en tiempo de ejecución) y vibración (feedback háptico).
- **Animaciones**: transiciones de pantalla y de pasos, progreso del asistente, sacudida ante errores, mensajes de error animados, contadores del resumen, cambio de color de estados, ítems de la lista y confirmación con rebote.

## 🧱 Arquitectura
MVVM con repositorios, inyección de dependencias manual y la misma estructura que la app de clientes.

```
data/      local (SQLite + DAOs) · model · repository · session · storage
domain/    validation (Validators) · pricing (IVA) · catalog (categorías y comunas)
security/  PasswordHasher (PBKDF2 + salt)
nativeapi/ Haptics · CameraAction
ui/        auth · products · publish · profile · components · navigation · theme
```

- La **validación** vive en `domain/validation` (Kotlin puro, sin Android ni Compose) y se prueba con tests.
- Las contraseñas se guardan con **hash PBKDF2 + salt**; el login no revela si el correo existe.
- Las fotos se toman en la caché y se copian al almacenamiento privado al confirmar.

## 🔗 Integración con la app de clientes
Ambas apps comparten el microservicio `ms-productos`: lo que publica el proveedor aparece en el
catálogo del cliente y las compras descuentan el stock que ve el proveedor.
Ver [`docs/contrato-datos.md`](docs/contrato-datos.md).

## ▶️ Cómo ejecutar
1. Levantar los microservicios (`backend/`, ver README principal).
2. Abrir el proyecto con **Android Studio** y esperar el *Gradle Sync*.
3. Ejecutar en un emulador o dispositivo (API 24+). Para la cámara, usar un emulador con cámara virtual o un teléfono real.
4. Crear una cuenta (RUT de prueba válido: `12.345.678-5`) y publicar un artículo.
5. Tests y cobertura: `./gradlew testDebugUnitTest jacocoTestReport`.
6. APK firmado: `./gradlew assembleRelease`.

## 🧪 Tests
Validaciones, cálculo de IVA, mapeos de red, repositorios (`AuthRepository`, `ProductRepository`, `IndicatorsRepository`) y ViewModels (login, registro, publicaciones, asistente y perfil) con **MockK** y **kotlinx-coroutines-test**.
