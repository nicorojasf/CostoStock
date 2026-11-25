# EFT DSY1105 — Dónde se cumple cada punto

## Requisitos mínimos obligatorios

| Requisito | Cómo se cumple | Dónde mirar |
|---|---|---|
| API externa pública | mindicador.cl con Retrofit (dólar, UF, euro) en catálogo, carrito, asistente y resumen | `data/remote/Apis.kt` (`IndicadoresApi`), `IndicatorsRepository` |
| Backend propio con microservicios y CRUD | ms-usuarios, ms-productos, ms-pedidos con JPA (H2/MySQL) | `backend/` |
| Persistencia local y externa | SQLite + SharedPreferences en el teléfono; bases de cada microservicio | README → *Almacenamiento local vs externo* |
| ≥ 2 recursos nativos | **Cámara**, **ubicación GPS** y vibración, con permisos en tiempo de ejecución | `nativeapi/` de cada app |
| Pruebas unitarias | JUnit + MockK sobre ViewModels y repositorios; JaCoCo; backend con JUnit 5 + Mockito + MockMvc | `app/src/test/`, `backend/*/src/test/` |
| APK firmado | `.jks` + `keystore.properties` + `signingConfigs.release` | `keystore/`, `app/build.gradle.kts` |
| **4 roles con privilegios diferenciados** | Cliente, Proveedor, Soporte, Administrador; validados en el backend con token | `backend/*/security/PerfilAcceso.java`, controladores |
| Formularios funcionales | Login, registro, recuperar contraseña, editar perfil, publicar (4 pasos), crear cuenta del equipo (panel) | `ui/auth/`, `ui/profile/`, `ui/publish/` |
| Personalización visual | Colores, logo, nombre e íconos propios de CostoStock | `ui/theme/`, `res/` |
| Login, registro, **recuperación de contraseña** y modificación de perfil | En ambas apps; recuperación con código de 6 dígitos | `ForgotPasswordScreen/ViewModel`, `RecuperacionService` |
| Navegación fluida | Navigation Compose con transiciones | `CostoStockRoot.kt`, `ProveedorRoot.kt` |
| Gestión de estado desacoplada | ViewModel + StateFlow; las pantallas solo dibujan el estado | `ui/**/*ViewModel.kt` |
| Animaciones | Transiciones, pasos animados, sacudida en errores, contadores | `ui/components/`, pantallas |

## Pauta de evaluación

| Indicador | % | Evidencia principal |
|---|---|---|
| IE1.3.1 POO (clases, herencia, polimorfismo) | 10 | `PerfilAcceso` y subclases, `NotificadorCorreo`, jerarquías selladas, excepciones propias |
| IE2.3.1 Modularidad + persistencia local | 10 | MVVM por paquetes (`data`, `domain`, `ui`, `nativeapi`), SQLite/SharedPreferences |
| IE2.3.2 GitHub + Trello | 10 | **Depende de ustedes**: commits de ambos y tablero con tareas repartidas (`docs/trello-planificacion.md`) |
| IE2.4.1 Recursos nativos | 15 | Cámara + GPS (+ vibración) con permisos, integrados en el flujo |
| IE3.1.1 App completa | 15 | Todas las pantallas y formularios con validación visual |
| IE3.1.2 Microservicios | 20 | 3 servicios, BD operativa, endpoints con autorización por rol |
| IE3.1.3 API externa | 5 | mindicador.cl integrada visualmente, independiente del backend |
| IE3.2.1 Pruebas ≥ 80% | 10 | `./gradlew jacocoTestReport` en cada app |
| IE3.3.1 APK firmado | 5 | `./gradlew assembleRelease` → copiar a `entregables/` |

## Antes de entregar
1. `mvn test` en los 3 microservicios y `./gradlew testDebugUnitTest jacocoTestReport` en las 2 apps.
2. Generar los APK firmados y dejarlos en `entregables/` con las capturas.
3. Completar en el README la sección *Evidencia de trabajo colaborativo* (enlace a Trello y commits por persona).
4. Subir a GitHub (público) **antes** de la fecha límite. Después de la fecha no se puede tocar el repositorio.
5. En AVA: un integrante sube el proyecto y **cada uno** completa el formulario individual.
