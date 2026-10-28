CostoStock

Plataforma móvil Android para comprar y vender insumos con control de costos y stock. Está formada por dos aplicaciones que comparten identidad visual, arquitectura y reglas de negocio:

Aplicación	Para quién	Carpeta	Paquete
CostoStock (Clientes)	Quien compra	CostoStock/	cl.costoStock.myapplication
CostoStock Proveedores	Quien vende	CostoStockProveedor/CostoStockProveedor/	cl.costoStock.proveedor

Evaluación Parcial 2 · DSY1105 Desarrollo de Aplicaciones Móviles · Duoc UC

Integrantes
Nicolas Rojas
Benjamin Perez

Equipo N° 4

¿Cómo funciona?

El proveedor publica artículos con fotos, precio, unidades y comuna. El cliente explora el catálogo, arma su carrito y confirma la compra. Todos los precios incluyen IVA (19%) y el carrito muestra el desglose Neto / IVA / Total. Al confirmar una compra, el stock se descuenta.

Funcionalidades
App de Clientes
Registro con validación por campo (nombre, correo, contraseña, confirmación y términos), medidor de seguridad de la contraseña y detección de correo duplicado.
Inicio de sesión con mensajes de error visuales y sesión persistente: al reabrir la app se entra directo al catálogo.
Catálogo con encabezado de marca, buscador, carrusel de destacados, accesos rápidos por categoría, filtros por categoría y comuna, filtro de poco stock, orden por precio y ficha de detalle del producto.
Carrito con cantidades limitadas por el stock, eliminación de líneas, vaciado con confirmación, desglose Neto / IVA 19% incluido / Total y confirmación de compra transaccional (descuenta stock y vacía el carrito en una sola operación).
Perfil con foto tomada con la cámara y cierre de sesión.
App de Proveedores
Registro con nombre del comercio, RUT (con dígito verificador), celular chileno, correo, contraseña con medidor de seguridad y términos.
Inicio de sesión con sesión persistente y cierre de sesión.
Mis publicaciones con resumen (activas, pausadas, unidades y valor del inventario), búsqueda, filtros por estado (todas, activas, pausadas, sin stock) y acciones para editar, pausar o activar y eliminar.
Asistente de publicación en 4 pasos, con validación en cada uno:
Producto: título, categoría, condición (nuevo o usado) y descripción.
Fotos: de 1 a 3 con la cámara; la primera es la portada.
Precio: precio con IVA incluido (con desglose), unidades y comuna.
Revisar: vista previa antes de publicar.
Perfil con foto, datos de contacto y edición de datos.
Recursos nativos
Recurso	Uso	Detalle
Cámara	Foto de perfil (ambas apps) y fotos de productos (Proveedores)	Permiso CAMERA solicitado en tiempo de ejecución, con manejo de rechazo y de dispositivos sin cámara. Las fotos se guardan en el almacenamiento interno privado.
Vibración	Retroalimentación háptica	Al agregar productos, ante errores de formulario y al confirmar una compra.
Animaciones

Transiciones entre pantallas, cambio animado de botón y cantidad, total animado del carrito, sacudida del formulario ante errores, barra de seguridad de contraseña, estado vacío, diálogo de éxito y, en Proveedores, progreso del asistente y contadores del resumen.

Arquitectura (MVVM)

Ambas apps siguen la misma estructura de carpetas:

data/
├── local        # SQLiteOpenHelper + DAOs
├── model        # Modelos de datos
├── repository   # Repositorios (acceso a datos desde los ViewModel)
├── session      # SessionManager (sesión activa)
└── storage      # PhotoStorage (fotos en almacenamiento privado)
domain/
├── validation   # Validators (reglas de formularios, Kotlin puro)
└── cart | pricing | catalog   # Cálculo de IVA y listas cerradas
security/        # PasswordHasher (PBKDF2 + sal)
nativeapi/       # Haptics y, en Proveedores, CameraAction
ui/
├── auth · home · cart · profile · products · publish   # Screen + ViewModel por módulo
├── components   # Campos, banners y elementos reutilizables
├── navigation   # Rutas
└── theme        # Tema Material 3 (claro y oscuro)
Las validaciones viven en Validators, sin dependencias de Android ni de la interfaz. Los ViewModel exponen el estado con StateFlow y las pantallas solo lo dibujan.
La persistencia es SQLite. Los repositorios ejecutan las consultas fuera del hilo principal y los Flow se vuelven a emitir cuando cambia la base, para que la interfaz se mantenga sincronizada.
Las contraseñas nunca se guardan en texto plano: se almacena un hash PBKDF2 con sal aleatoria por usuario.
La inyección de dependencias es manual (contenedor en la clase Application y AppViewModelProvider).
Tecnologías

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · ViewModel y StateFlow · SQLite (SQLiteOpenHelper) · JUnit.

Cómo ejecutar

Requisitos: Android Studio actualizado (compatible con Android SDK 37) y un emulador o dispositivo con Android 7.0 (API 24) o superior.

Abrir en Android Studio la carpeta de la app que se quiere probar:
Clientes: CostoStock/
Proveedores: CostoStockProveedor/CostoStockProveedor/
Esperar la sincronización de Gradle.
Elegir un emulador o dispositivo y presionar Run ▶.
Crear una cuenta desde "Regístrate" e ingresar.

Datos de prueba:

Clientes: el catálogo viene con 14 productos de ejemplo en 5 categorías.
Proveedores: RUT de prueba válido 12.345.678-5 y celular con formato +56 9 1234 5678.

Para probar la cámara se recomienda un dispositivo físico o un emulador con cámara virtual.

Pruebas unitarias

Cubren validaciones, cálculo de IVA y totales del carrito, y hash de contraseñas. Desde la carpeta de cada app:

./gradlew test
Panel de administración (prototipo)

El repositorio incluye una página web en HTML con una vista de administración (resumen de ventas, productos, pedidos, clientes y proveedores). Usa el catálogo real de la app, pero clientes, proveedores y pedidos son datos de ejemplo y no está conectada a las bases de datos de las apps. Se abre con doble clic en cualquier navegador.

Integración entre las apps

Cada app guarda sus datos en su propia base SQLite en el dispositivo, por lo que todavía no comparten información. La tabla products del proveedor incluye las mismas columnas que la del cliente, más datos propios (proveedor, condición, fotos, estado y fechas). Los cambios pendientes en la app de Clientes están descritos en CostoStockProveedor/CostoStockProveedor/docs/contrato-datos.md.

Convención de commits

Prefijo del tipo de cambio seguido de una descripción técnica:

feat: nueva funcionalidad · fix: corrección · refactor: reestructuración · test: pruebas · docs: documentación

Ejemplo: feat(auth): validar correo duplicado en registro
