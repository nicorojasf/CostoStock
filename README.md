<div align="center">

# 🛒 CostoStock

**Compra y venta de insumos con control de costos y stock**

*Nada se pierde, todos ganamos*

![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3-6DB33F?logo=springboot&logoColor=white)
![Android](https://img.shields.io/badge/Android-APK%20firmado-3DDC84?logo=android&logoColor=white)
![Tests](https://img.shields.io/badge/Pruebas-JUnit%20%2B%20MockK-EB6A1D)

Evaluación Final Transversal · DSY1105 Desarrollo de Aplicaciones Móviles · Duoc UC · **Equipo N° 4**

</div>

---

> 📁 **La versión final del proyecto está en la carpeta [`CostoStockFinal/`](CostoStockFinal/).**
> Las demás carpetas del repositorio corresponden a entregas anteriores.

## 📱 ¿Qué es CostoStock?

CostoStock conecta a **personas que venden** insumos (nuevos o usados) con **personas que compran**, mostrando siempre el precio con IVA, el stock real y la comuna de cada producto.

El proyecto está formado por:

| Parte | Para quién | Carpeta |
|---|---|---|
| 🛍️ **App CostoStock** | Clientes que compran | [`CostoStockFinal/CostoStock`](CostoStockFinal/CostoStock) |
| 🏪 **App CostoStock Proveedores** | Vendedores que publican | [`CostoStockFinal/CostoStockProveedor/CostoStockProveedor`](CostoStockFinal/CostoStockProveedor/CostoStockProveedor) |
| ⚙️ **3 microservicios** (usuarios, productos, pedidos) | Backend propio con base de datos | [`CostoStockFinal/backend`](CostoStockFinal/backend) |
| 🖥️ **Panel de administración web** | Equipo de Soporte y Administración | [`CostoStockFinal/CostoStock · Panel de administración.html`](CostoStockFinal/) |

## 👥 Integrantes

| Integrante | Responsabilidad principal |
|---|---|
| **Nicolas Rojas** | App de Proveedores · microservicios de usuarios y productos |
| **Benjamin Perez** | App de Clientes · microservicio de pedidos · API externa |

### Trabajo colaborativo
- Historial de commits: [ver commits](../../commits/main)
- Aportes por integrante: [ver contribuidores](../../graphs/contributors)

## ✨ Funcionalidades

### 🔐 Cuentas y seguridad
- Registro, inicio de sesión y **edición de perfil** en ambas apps.
- **Recuperación de contraseña** con un código de 6 dígitos que vence en 10 minutos.
- Contraseñas guardadas de forma segura (cifradas) en el servidor.
- Cada sesión usa un **token** que el servidor revisa antes de permitir una acción.

### 👤 4 roles de usuario

| Rol | Dónde trabaja | Qué puede hacer |
|---|---|---|
| **Cliente** | App CostoStock | Comprar, ver y cancelar sus pedidos, editar o eliminar su cuenta |
| **Proveedor** | App Proveedores | Publicar, editar, pausar y eliminar sus productos |
| **Soporte** | Panel web | Ver todo, pausar o activar publicaciones y cancelar pedidos (no puede eliminar) |
| **Administrador** | Panel web | Todo lo de Soporte, además de eliminar registros y crear cuentas del equipo |

### 🛍️ App de Clientes
- Catálogo que **se actualiza solo** con buscador, filtros por categoría y comuna, y orden por precio.
- Botón **"Usar mi ubicación"** que muestra los productos de la comuna más cercana.
- Carrito con desglose de **Neto, IVA y Total**, limitado al stock disponible.
- Confirmación de compra e historial en **"Mis pedidos"**, con opción de cancelar.
- Precios convertidos a **dólares y UF** con los indicadores del día.

### 🏪 App de Proveedores
- Resumen del inventario: publicaciones activas, pausadas y valor total (también en US$ y UF).
- **Asistente de publicación en 4 pasos**: producto, fotos, precio y revisión.
- Fotos con la **cámara** y comuna detectada con el **GPS**.
- El stock baja automáticamente cuando un cliente compra.

### 🖥️ Panel de administración
- Inicio de sesión para Soporte y Administrador.
- Resumen en vivo: ventas, pedidos, gráficos por día, categoría y comuna, y alertas de stock.
- Gestión de publicaciones, pedidos y usuarios según el rol.

### 📲 Recursos del teléfono
| Recurso | Uso |
|---|---|
| 📷 **Cámara** | Foto de perfil y fotos de productos |
| 📍 **Ubicación (GPS)** | Detectar la comuna del usuario o del producto |
| 📳 **Vibración** | Aviso al haber errores en formularios o al completar una acción |

Todos piden permiso al usuario cuando corresponde. La ubicación no se guarda ni se envía al servidor.

### 💾 Dónde se guardan los datos
| Lugar | Qué guarda | Por qué |
|---|---|---|
| **Teléfono** (SQLite y preferencias) | Carrito, sesión y foto de perfil | Son datos personales del dispositivo |
| **Microservicios** (base de datos) | Usuarios, productos, stock y pedidos | Son datos compartidos entre todos los usuarios |

## 🔗 Endpoints

### Microservicios propios
| Servicio | Dirección | Principales endpoints |
|---|---|---|
| **ms-usuarios** | `http://localhost:8081` | `/api/clientes` · `/api/proveedores` · `/api/equipo` · `/api/recuperacion` |
| **ms-productos** | `http://localhost:8082` | `/api/productos` (catálogo, publicar, editar, pausar, eliminar) |
| **ms-pedidos** | `http://localhost:8083` | `/api/pedidos` (comprar, historial, cancelar, eliminar) |

Cada servicio tiene documentación interactiva en `/swagger-ui.html`. La lista completa con los permisos de cada endpoint está en el [README del proyecto](CostoStockFinal/README.md#4-endpoints-utilizados). También hay una colección de Postman en [`CostoStockFinal/backend`](CostoStockFinal/backend).

### API externa
| API | Uso |
|---|---|
| [mindicador.cl](https://mindicador.cl/api) | Dólar, UF y euro del día para mostrar precios equivalentes |

## 🚀 Cómo ejecutarlo

**Requisitos:** Android Studio con un emulador. Para el backend no hay que instalar nada más (en Windows el script descarga Maven solo).

1. **Encender el backend:** abrir `CostoStockFinal/backend` y hacer doble clic en `iniciar-microservicios.bat`. Esperar a que las 3 ventanas digan `Started`.
2. **Abrir las apps:** en Android Studio, abrir `CostoStockFinal/CostoStock` (clientes) y `CostoStockFinal/CostoStockProveedor/CostoStockProveedor` (proveedores), y presionar ▶.
3. **Abrir el panel:** doble clic en `CostoStock · Panel de administración.html`.

**Cuentas del panel:**
| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@costostock.cl` | `Admin1234` |
| Soporte | `soporte@costostock.cl` | `Soporte1234` |

💡 Para recuperar una contraseña, el código de 6 dígitos aparece en la ventana de **ms-usuarios** (simula el correo).
💡 En el emulador se puede fijar una ubicación en *Extended controls → Location* (por ejemplo `-33.4378, -70.6504`).

El paso a paso completo, incluido el uso con teléfono físico, está en el [README del proyecto](CostoStockFinal/README.md#5-pasos-para-ejecutar).

## 📦 APK firmado y llaves

| Archivo | Ubicación |
|---|---|
| APK App Clientes | [`CostoStockFinal/entregables/CostoStock-Clientes-release.apk`](CostoStockFinal/entregables) |
| APK App Proveedores | [`CostoStockFinal/entregables/CostoStock-Proveedores-release.apk`](CostoStockFinal/entregables) |
| Llave Clientes (.jks) | `CostoStockFinal/CostoStock/keystore/costostock-clientes.jks` |
| Llave Proveedores (.jks) | `CostoStockFinal/CostoStockProveedor/CostoStockProveedor/keystore/costostock-proveedores.jks` |
| Configuración de firma | `keystore.properties` y `app/build.gradle.kts` de cada app |

## 🧪 Pruebas
- **Apps:** pruebas unitarias de ViewModels y repositorios con JUnit y MockK, más el reporte de cobertura con JaCoCo.
- **Microservicios:** pruebas de servicios y pruebas de integración de cada API.

## 🛠️ Tecnologías
Kotlin · Jetpack Compose · Material 3 · Retrofit · SQLite · Java 17 · Spring Boot · Spring Data JPA · H2 / MySQL · JUnit · MockK · JaCoCo

## 📚 Más información
- [README completo del proyecto](CostoStockFinal/README.md)
- [Guía de la defensa](CostoStockFinal/docs/defensa.md)
- [Checklist de la evaluación](CostoStockFinal/docs/EFT-checklist.md)
