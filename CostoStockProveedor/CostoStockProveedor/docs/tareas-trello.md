# Tablero Trello sugerido
Columnas: **Por hacer · En progreso · En revisión · Hecho**.
Crea una tarjeta por ítem, asígnala a una persona y nombra la tarjeta en el commit/PR (ej: `feat: valida RUT (#12)`).

## App de Proveedores
- [x] Crear repositorio, ramas y tablero
- [x] Base de datos SQLite (sellers, products) y DAOs
- [x] Validadores (RUT, teléfono, contraseña, precio, stock) + tests unitarios
- [x] Registro y login de proveedores
- [x] Perfil: datos, edición y cierre de sesión
- [x] Recurso nativo: cámara (foto de perfil y de productos, con permisos)
- [x] Recurso nativo: vibración (feedback háptico)
- [x] Mis publicaciones: lista, filtros, búsqueda, pausar y eliminar
- [x] Asistente de publicación (4 pasos) con validación por paso
- [x] Edición de publicaciones
- [x] Animaciones (transiciones, progreso, errores, contadores)
- [x] Documentar contrato de datos con la app de cliente
- [x] README final
- [ ] Entrega en AVA

## App de Clientes (compañero)
- [ ] Migración de `products` según `docs/contrato-datos.md`
- [ ] Mostrar solo publicaciones activas y con stock
- [ ] Mostrar foto del producto (con respaldo de emoji)

## Conexión entre las dos apps
- [ ] Elegir backend (API REST o Firebase) y definir quién lo implementa
- [ ] Reemplazar `ProductRepository` (proveedor) y el catálogo (cliente) por llamadas al backend
