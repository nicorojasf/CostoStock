# Contrato de datos entre la App de Proveedores y la App de Clientes

Ambas apps usan **SQLite** y modelan los artículos en una tabla `products`.
Por ahora cada app tiene su propia base en el dispositivo; este documento define cómo se unirán
cuando compartan la base de datos (o un backend).

## Tabla `products`

| Columna | Tipo | Cliente (`costostock.db`) | Proveedor (`costostock_proveedor.db`) | Notas |
|---|---|:---:|:---:|---|
| `id` | INTEGER PK AUTOINCREMENT | ✅ | ✅ | |
| `name` | TEXT NOT NULL | ✅ | ✅ | En el proveedor es el "Título de la publicación" |
| `category` | TEXT NOT NULL | ✅ | ✅ | Las 5 categorías del cliente existen en el proveedor (+ Deportes, Libros y hobbies, Otros) |
| `description` | TEXT NOT NULL | ✅ | ✅ | |
| `price` | INTEGER NOT NULL (≥ 0) | ✅ | ✅ | Pesos chilenos, **IVA incluido** |
| `stock` | INTEGER NOT NULL (≥ 0) | ✅ | ✅ | El cliente lo descuenta al confirmar una compra |
| `comuna` | TEXT NOT NULL | ✅ | ✅ | |
| `emoji` | TEXT NOT NULL | ✅ | ✅ | El proveedor lo completa según la categoría |
| `seller_id` | INTEGER NOT NULL (FK `sellers.id`) | ➕ | ✅ | Quién publica |
| `item_condition` | TEXT (`NEW` / `USED`) | ➕ | ✅ | |
| `image_paths` | TEXT (rutas separadas por `\|`) | ➕ | ✅ | Hoy son rutas locales; con backend pasarán a ser URLs |
| `status` | TEXT (`ACTIVE` / `PAUSED`) | ➕ | ✅ | |
| `created_at` / `updated_at` | INTEGER (epoch ms) | ➕ | ✅ | |

✅ ya existe · ➕ columna que el cliente tendría que agregar.

> Las 8 primeras columnas son **idénticas** a las del cliente, así que lo que publica el proveedor
> ya cabe en la tabla del cliente sin transformar nada.

## Qué cambia en la app de Cliente para conectarse
1. **Migración** de `products` (subir `DB_VERSION` a 3): agregar `seller_id`, `item_condition`, `image_paths`, `status`, `created_at`, `updated_at`.
2. **Consulta del catálogo:** mostrar solo `status = 'ACTIVE' AND stock > 0` (hoy `getAll()` devuelve todo).
3. **Imágenes:** mostrar la primera foto de `image_paths` si existe; si no, el `emoji` (ya lo usan hoy).
4. **Compras:** el descuento de `stock` al confirmar una compra ya funciona sobre esta misma tabla,
   por lo que el proveedor verá bajar su stock automáticamente.

## Usuarios
- Cliente: tabla `users` (compradores).
- Proveedor: tabla `sellers` (vendedores, con RUT y teléfono).
Son cuentas distintas; un mismo correo puede existir en ambas apps.

## Cuando exista un backend
- Los `id` los asignará el servidor.
- `image_paths` pasará a URLs (subida de fotos al servidor).
- Solo cambia la implementación de `ProductRepository`; las pantallas y ViewModels no se tocan.
