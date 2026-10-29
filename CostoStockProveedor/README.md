# 🛒 CostoStock Proveedores

App Android para que los **proveedores** publiquen y administren los artículos que venden en **CostoStock**.
Es la contraparte de la app de clientes: el proveedor sube sus artículos y el cliente los compra.
Proyecto de la asignatura **DSY1105 – Desarrollo de Aplicaciones Móviles** (Duoc UC).

## 👥 Integrantes
- _Nombre Apellido_ — App de Proveedores
- _Nombre Apellido_ — App de Clientes

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
- **Persistencia local** con SQLite (`SQLiteOpenHelper` + DAOs), igual que la app de clientes.
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
La tabla `products` incluye las mismas columnas que la del cliente más algunas propias del proveedor.
Ver [`docs/contrato-datos.md`](docs/contrato-datos.md).

## ▶️ Cómo ejecutar
1. Abrir el proyecto con **Android Studio** y esperar el *Gradle Sync*.
2. Ejecutar en un emulador o dispositivo (API 24+). Para la cámara, usar un emulador con cámara virtual o un teléfono real.
3. Crear una cuenta (RUT de prueba válido: `12.345.678-5`) y publicar un artículo.
4. Tests: `./gradlew test`.

## 🧪 Tests
Validaciones (RUT, teléfono, contraseña, título, precio, stock, fotos…), cálculo de IVA y hash de contraseñas.
