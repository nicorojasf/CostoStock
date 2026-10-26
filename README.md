# CostoStock

Aplicación móvil Android (Kotlin + Jetpack Compose + Material 3) para **gestionar compras de insumos con control de costos y stock**. Permite registrarse, iniciar sesión, explorar un catálogo, armar un carrito y confirmar la compra descontando stock.

> Evaluación Parcial 2 – DSY1105 Desarrollo de Aplicaciones Móviles (Duoc UC)

## Integrantes
- _Nombre Apellido 1_
- _Nombre Apellido 2_

**Equipo N°:** _X_ · **Planificación:** _enlace al tablero de Trello_

## Funcionalidades
- **Registro** con validación por campo (nombre, correo, contraseña, confirmación, términos), medidor de seguridad de la contraseña y detección de correo duplicado.
- **Inicio de sesión** con validación, mensajes de error visuales y sesión persistente (al reabrir la app se entra directo al catálogo).
- **Catálogo** (identidad CostoStock, distribución inspirada en Mercado Libre): encabezado con logo, comuna y buscador; carrusel de destacados; accesos rápidos; filtros por categoría y comuna; orden por precio; grilla de productos con precio (IVA incluido), comuna y stock; ficha de detalle del producto.
- **Carrito** con cantidades limitadas por stock, eliminación, vaciado con confirmación, desglose Neto / IVA 19% incluido / Total y **confirmación de compra** transaccional (descuenta stock y vacía el carrito).
- **Perfil** con foto tomada con la cámara y cierre de sesión.
- **Animaciones:** transiciones de navegación, cambio animado de botón/cantidad, total animado, "shake" del formulario ante error, barra de seguridad de contraseña, estado vacío y diálogo de éxito.

## Recursos nativos
1. **Cámara** (foto de perfil) con solicitud de permiso `CAMERA` en tiempo de ejecución y manejo de rechazo / dispositivo sin cámara. La foto se guarda en el almacenamiento interno privado.
2. **Vibración** (feedback háptico) al agregar productos, en errores de formulario y al confirmar la compra.

## Arquitectura (MVVM)
```
cl.costoStock.myapplication
├── data
│   ├── local        # SQLiteOpenHelper + DAOs (usuarios, productos, carrito)
│   ├── model        # User, Product, CartLine
│   ├── repository   # AuthRepository, ProductRepository, CartRepository
│   ├── session      # SessionManager (sesión activa)
│   └── storage      # PhotoStorage (fotos de perfil)
├── domain
│   ├── validation   # Validators (lógica pura, sin UI)
│   └── cart         # CartCalculator (neto / IVA / total)
├── security         # PasswordHasher (PBKDF2 + sal)
├── nativeapi        # Haptics (vibración)
└── ui
    ├── auth · home · cart · profile   # Screen + ViewModel por módulo
    ├── components   # Campos, stepper, banners reutilizables
    ├── navigation   # Rutas
    └── theme        # Tema Material 3
```
- La **validación vive en `Validators`** y los `ViewModel` exponen el estado (`StateFlow`); las pantallas solo lo dibujan.
- La **persistencia es SQLite**; los `Flow` de los repositorios se vuelven a emitir cuando cambia la base, así la UI se mantiene sincronizada.
- Las contraseñas **nunca se guardan en texto plano**: se almacena hash PBKDF2 con sal aleatoria por usuario.

## Cómo ejecutar
1. Abrir la carpeta del proyecto en **Android Studio** y esperar la sincronización de Gradle.
2. Elegir un emulador (API 24+) o un dispositivo físico.
3. Presionar **Run ▶**.
4. Crear una cuenta desde "Regístrate" e ingresar. Para probar la cámara se recomienda un dispositivo físico o un emulador con cámara virtual.

Tests unitarios: `./gradlew test`

## Convención de commits
`feat:`, `fix:`, `refactor:`, `test:`, `docs:` seguido de una descripción técnica (ej. `feat(auth): validar correo duplicado en registro`).
