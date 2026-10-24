# Planificación: app Android de gestión veterinaria

*Documento de planificación · Parte 1 de 2: análisis, decisiones de arquitectura y MVP · 23 de octubre de 2026*

## Resumen ejecutivo

- **Recomendación central:** app nativa en Kotlin con Jetpack Compose y base local Room (SQLite) como fuente de verdad. Arranca en un solo dispositivo, sin servidor y sin costo recurrente, pero se diseña local-first: identificadores, versionado y operaciones quedan preparados para sumar sincronización más adelante sin reescribir.
- **MVP:** clientes, mascotas, historia clínica básica, vacunaciones con recordatorios (prioridad del proyecto), turnos, catálogo con stock, ventas con caja y un dashboard con facturación por área. Incluye backup/restauración y bloqueo de la app.
- **Cambios al plan original:** backup y seguridad base se adelantan (antes de cargar datos reales), turnos pasan antes del catálogo y el dashboard se construye de forma incremental en lugar de al final.
- **Antes de programar** hay que confirmar siete decisiones (sección 1.4) y responder cinco preguntas (sección 1.6).
- Esta Parte 1 cubre análisis, arquitectura y MVP. La Parte 2 desarrollará la especificación funcional por módulo, el mapa de navegación, el diseño UX/UI lila, las reglas de negocio completas, el modelo de datos con diagrama E-R, el plan detallado por etapa y los criterios de aceptación.

> **Actualización 23/10/2026 — Plan v2 aprobado.** Reemplaza D1/D2, la recomendación de 2.1, el stack de 2.2 y el alcance de la sección 3 en lo relativo a sincronización. Resumen: una device, una cuenta, Room local-first con sync propio a Supabase Free en São Paulo, subida inmediata sin Realtime, backup local cifrado semanal sin fotos y fotos online-only comprimidas en el bucket público `mascotas`. El detalle está al final del documento.

## 1. Análisis inicial

### 1.1 Objetivo y problema que resuelve

Una veterinaria suele repartir su información entre cuadernos, planillas, mensajes y la memoria del personal: se pierden vacunas por vencer, no se sabe cuánto stock queda, la caja se cuadra a mano y nadie ve con claridad qué área rinde más. El objetivo es centralizar clientes, mascotas, historias clínicas, vacunaciones, turnos, inventario, ventas y resultados en una app Android moderna, confiable y rápida de usar, que reduzca errores manuales y muestre el estado del negocio en un dashboard.

### 1.2 Necesidades y módulos

| Necesidad del negocio | Cómo la resuelve la app | Módulo |
| --- | --- | --- |
| No perder el seguimiento de vacunas y controles | Calcula próximas dosis, genera avisos y los muestra en el dashboard | Vacunaciones y recordatorios |
| Tener la historia de cada mascota a mano | Ficha cronológica, trazable y vinculada al propietario | Clientes, mascotas, historia clínica |
| Ordenar la agenda | Calendario con estados, superposiciones y disponibilidad | Turnos |
| Saber qué hay y qué falta | Stock calculado por movimientos, con alertas de mínimo | Catálogo e inventario |
| Cobrar sin errores | Venta con totales automáticos, pagos divididos y cierre de caja | Ventas y caja |
| Saber cómo viene el negocio | Facturación y resultado estimado por área comercial | Dashboard y reportes |

### 1.3 Huecos y problemas detectados en los requisitos

| # | Tema | Por qué importa | Propuesta |
| --- | --- | --- | --- |
| 1 | Los recordatorios llegan al personal, no al dueño | La notificación local avisa al dispositivo de la veterinaria, pero quien tiene que venir a vacunar es el propietario | Mensaje de WhatsApp prearmado (enlace wa.me, sin costo, un toque) en la v1.1; la API de WhatsApp Business solo si hay volumen, porque tiene costo y requiere aprobación |
| 2 | Vacunar no es solo 6 o 12 meses | Cachorros y gatitos llevan una serie de dosis a intervalos cortos; el criterio es del veterinario | Protocolos configurables por especie (serie inicial + refuerzo) y fecha siempre editable |
| 3 | Mascotas fallecidas o dadas de baja | Seguir enviando avisos es un error delicado | Estado de la mascota (activa, inactiva, fallecida) que suprime todos los recordatorios |
| 4 | Antiparasitarios y controles periódicos | Usan el mismo motor que las vacunas | Recordatorio genérico con tipo: vacuna, desparasitación, control |
| 5 | Caja | Se pidió ventas y caja, pero no apertura ni cierre | Apertura, cierre y arqueo diarios, con retiros y gastos menores |
| 6 | Cuenta corriente, señas y pagos parciales | Son habituales en cirugías y clientes de confianza | Fuera del MVP, pero el modelo de pagos ya admite varios pagos por venta |
| 7 | Inflación y precios | El costo de reposición cambia seguido y desactualiza márgenes | Actualización masiva de precios por porcentaje o categoría; el costo se congela en cada línea de venta |
| 8 | Facturación fiscal | La app registra operaciones internas, no emite comprobantes | Fuera del MVP; evaluar después la integración con facturación electrónica de ARCA (ex AFIP) |
| 9 | Datos personales e historias clínicas | Hay datos personales de clientes (Ley 25.326) y posibles requisitos de conservación de historias clínicas | Validar con el colegio veterinario y el contador; diseñar retención y evitar el borrado físico |
| 10 | Copropietarios | Un único propietario vigente puede chocar con familias | Propietario principal más contactos secundarios (solo contacto, sin titularidad) |
| 11 | Vacuna aplicada vs. venta | Una vacuna es un acto clínico y también un producto o servicio cobrado | La vacunación es independiente del catálogo; vínculo opcional con el producto (lote, descuento de stock) y con el cobro |
| 12 | Datos previos | Cuadernos o planillas existentes | Importación CSV de clientes, mascotas y productos en la v1.1 |

### 1.4 Decisiones que hay que tomar antes de empezar

| # | Decisión | Opciones | Recomendación inicial |
| --- | --- | --- | --- |
| D1 | ¿Uno o varios dispositivos? | Un dispositivo / varios con sincronización | Un dispositivo en el MVP, con arquitectura local-first lista para sincronizar |
| D2 | ¿Un usuario o varios con roles? | Un usuario / varios con permisos | Un usuario con PIN o biometría; el modelo ya incluye usuarios y profesionales |
| D3 | ¿Para quién se construye? | Uso propio / una veterinaria concreta / producto para varias | Se asume una veterinaria concreta; define el nivel de soporte y de pulido |
| D4 | ¿Facturación fiscal desde la app? | Sí / no por ahora | No por ahora |
| D5 | ¿Cómo se distribuye? | APK firmado directo / Play Store con prueba interna o cerrada / publicación abierta | APK firmado o prueba interna de Play para el MVP |
| D6 | ¿Avisos al propietario? | Solo al personal / WhatsApp prearmado / API de WhatsApp Business | Mensaje prearmado en la v1.1; la API solo si el volumen lo justifica |
| D7 | ¿Cómo se calculan costos y ganancias? | Solo porcentaje / solo costos reales / híbrido | Híbrido (sección 2.7) |
|  |  |  |  |

### 1.5 Supuestos de trabajo

Valen hasta que se confirmen o corrijan:

- Una veterinaria, un local, entre 1 y 3 personas usando la app, en un teléfono o tablet Android 8.0 o superior.
- Moneda: pesos argentinos, sin multimoneda. Precios finales al consumidor, sin discriminar IVA.
- Zona horaria America/Argentina/Buenos\_Aires.
- Volumen esperable: miles de clientes y decenas de ventas por día. SQLite sobra con holgura.
- La conexión a internet no es permanente: la app debe funcionar completa sin conexión.
- El criterio clínico siempre es del veterinario: la app no sugiere diagnósticos ni dosis, solo registra y recuerda.

### 1.6 Preguntas imprescindibles

1. ¿La app es para una veterinaria real? ¿Cuántas personas la usarían y en qué dispositivos (celular, tablet o ambos)?
2. ¿Cómo llevan hoy clientes y vacunas (papel, planilla, otro sistema)? ¿Hay datos para migrar?
3. ¿Necesitan emitir facturas desde la app o usan otro sistema para eso?
4. ¿Hay más de un veterinario con agenda propia?
5. ¿Con qué dispositivo Android se va a probar (marca y versión)? Importa para validar las notificaciones.

## 2. Decisiones arquitectónicas

### 2.1 Un dispositivo o varios

| Aspecto | A. Solo local | B. Local-first con sincronización | C. Online (cliente-servidor) |
| --- | --- | --- | --- |
| Cómo funciona | Todo en la base del dispositivo | Cada dispositivo tiene su base y sincroniza cuando hay conexión | Los datos viven en un servidor y la app los consulta |
| Ventajas | Sin costo recurrente, funciona sin internet, simple, los datos no salen del local | Offline, varios dispositivos y respaldo en la nube | Una única fuente de verdad, multi-dispositivo directo |
| Desventajas | Si el equipo se pierde o rompe se pierden los datos sin backup; un solo puesto; sin acceso remoto | Complejidad (conflictos, autenticación, reglas de seguridad) y datos personales en la nube | Sin internet no se puede cobrar ni atender; requiere servidor 24/7 y mantenimiento |
| Costo | Cero | Planes gratuitos con límites en servicios gestionados (Firebase, Supabase) y luego pago por uso, o backend propio con hosting | Hosting permanente y mantenimiento |
| Encaja cuando | Un puesto de trabajo | Varios puestos con conexión intermitente | Conexión estable garantizada |

**Recomendación:** empezar con A construido como B. Es decir, MVP en un dispositivo, pero con los principios de la sección 2.6 desde el primer día, de modo que sumar sincronización sea agregar una capa y no rehacer el modelo. La elección del backend (servicio gestionado o uno propio, por ejemplo en FastAPI y MySQL) se difiere hasta que exista una necesidad real de varios dispositivos. Antes de comprometerse con un servicio gestionado, verificar sus condiciones y límites vigentes.

### 2.2 Comparación de tecnologías por capa

| Capa | Alternativas | Recomendación | Motivo y limitaciones |
| --- | --- | --- | --- |
| Interfaz y lenguaje | Kotlin con Compose (nativo) / Flutter / React Native / Kotlin Multiplatform | **Kotlin + Jetpack Compose + Material 3** | Acceso directo a notificaciones, trabajos en segundo plano, permisos, archivos y cámara; buen soporte para tablets con clases de tamaño de ventana; Kotlin resulta cercano a Java. Limitación: solo Android. Flutter permite sumar iOS pero agrega Dart y depende de plugins para segundo plano; React Native reutiliza JavaScript pero es más frágil en alarmas y offline |
| Arquitectura interna | MVVM con flujo unidireccional / MVI / todo en la UI | **MVVM por capas (ui, domain, data)**, Hilt, Coroutines y Flow, Navigation Compose | Las reglas de negocio quedan en un dominio puro y se prueban sin Android |
| Base local | Room (SQLite) / SQLDelight / Realm / ObjectBox | **Room** | Estándar de Android, migraciones versionadas, consultas verificadas en compilación. Realm queda descartada porque MongoDB anunció su discontinuación |
| Notificaciones | Locales con WorkManager / alarmas exactas / FCM | **Locales con WorkManager** ahora; FCM solo con backend | Funcionan sin servidor ni conexión. Detalle en la sección 2.4 |
| Autenticación | Bloqueo local (PIN, biometría) / Firebase Auth / backend propio con JWT | **Bloqueo local con BiometricPrompt** en el MVP | Multiusuario real llega con la sincronización |
| Gráficos | Vico / MPAndroidChart / dibujo propio | **Vico** | Pensada para Compose y suficiente para barras y líneas |
| Reportes y exportación | CSV / PDF / Excel | **CSV en el MVP; PDF para cierres de caja**; Excel solo si lo piden | CSV abre en Excel sin dependencias; las librerías de Excel son pesadas |
| Copias de seguridad | Exportar la base cifrada / Auto Backup de Android / API de Drive | **Exportación cifrada manual y programada** a una ubicación elegida, con opción de compartir a Drive | Auto Backup tiene tope de tamaño y poco control; la API de Drive suma complejidad de autorización |
| Pruebas | JUnit con MockK y Turbine / Room en memoria / pruebas de UI de Compose / pruebas manuales | **Todas, en capas** | Dominio con pruebas unitarias; notificaciones siempre en dispositivos reales |
| Distribución | APK firmado / Play Store (prueba interna o cerrada) / publicación abierta | Según D5 | Publicar en Play implica cuenta de desarrollador (pago único de US$25) y política de privacidad |

### 2.3 Capas de la aplicación

| Capa | Responsabilidad | Regla |
| --- | --- | --- |
| UI (Compose) | Pantallas, estados de carga, error y vacío, componentes reutilizables | No contiene reglas de negocio |
| ViewModel | Estado de pantalla y eventos del usuario | Habla solo con casos de uso |
| Dominio | Cálculo de ventas, estado de vacunas, superposición de turnos, cálculo económico | Kotlin puro, sin dependencias de Android, 100 % probable con pruebas unitarias |
| Datos | Repositorios (interfaces) sobre Room y DataStore | Único punto de acceso a la persistencia |
| Plataforma | WorkManager, notificaciones, archivos, biometría | Aislada detrás de interfaces |
| Sincronización (futura) | Cola de operaciones y resolución de conflictos | Se agrega sin tocar el dominio |

### 2.4 Notificaciones: cómo funcionan y sus límites en Android

- **Mecanismo:** un trabajo diario de WorkManager recalcula los estados de las vacunas y emite los avisos que correspondan (30 días antes, 7 días antes y vencidas, configurables). No necesita servidor ni conexión y sigue funcionando con la app cerrada.
- **Evitar duplicados:** cada aviso tiene una clave única (vacunación + hito). Una nueva aplicación cierra el seguimiento anterior y crea el siguiente sin borrar el historial.
- **Teléfono apagado o ahorro de batería:** WorkManager no es exacto y puede demorarse. El trabajo procesa todo lo pendiente cuando vuelve a ejecutarse, y el dashboard calcula los vencimientos al abrir la app, de modo que el dato nunca depende de que la notificación haya salido.
- **Permisos:** desde Android 13 hace falta el permiso de notificaciones, que se pide en contexto y con una explicación. No se necesitan permisos amplios de almacenamiento (se usa el selector del sistema); la cámara solo si se activan fotos o lectura de códigos de barras.
- **Alarmas exactas:** requieren un permiso especial y Google Play restringe su uso. Para avisos diarios de vacunas no hacen falta.
- **Riesgos reales:** algunos fabricantes aplican ahorro de batería agresivo y, si el usuario fuerza el cierre de la app, los trabajos programados se detienen hasta que se vuelva a abrir. Se mitiga con una pantalla de diagnóstico de permisos y batería, y con una prueba temprana en el dispositivo real.
- **Avisos remotos a varios dispositivos:** requieren FCM y un backend. Las notificaciones locales alcanzan para un dispositivo.

### 2.5 Riesgo de guardar todo solo en el teléfono

| Riesgo | Mitigación |
| --- | --- |
| Pérdida, robo o rotura del equipo | Backup cifrado automático a una ubicación externa (Drive, almacenamiento compartido o PC) |
| Backup que nunca se probó | Prueba de restauración en un dispositivo limpio como criterio de aceptación |
| Acceso no autorizado al equipo | Bloqueo de la app con PIN o biometría, cifrado del dispositivo, sin datos en logs |
| Cambio de equipo | Flujo de exportar, instalar y restaurar con verificación de integridad |
| Corrupción de la base | Chequeo de integridad al iniciar y copias rotativas (diaria y semanal) |

Si más adelante se evalúa cifrar la base completa con SQLCipher, se pesa contra su costo de complejidad. Para el MVP alcanza con el cifrado del dispositivo, el bloqueo de la app y el backup cifrado.

### 2.6 Principios de datos que se fijan desde el día 1

| Principio | Qué implica | Qué evita |
| --- | --- | --- |
| Identificadores UUID | Cada registro tiene un UUID generado en el dispositivo, no un autoincremental | Colisiones al sincronizar varios dispositivos |
| Dinero en enteros | Importes en centavos (Long), nunca Double | Errores de redondeo en caja |
| Fechas bien tipadas | Instantes en UTC para eventos; fechas de calendario (sin hora) para próximas dosis; zona horaria fija para el día comercial | Recordatorios corridos y ventas asignadas al día equivocado |
| Stock como libro de movimientos | El stock actual es la suma de movimientos (entrada, venta, ajuste, pérdida, anulación), con saldo cacheado y verificable | Stock que se pisa y no se puede auditar ni sincronizar |
| Ventas inmutables | Anular crea un registro de reversión y movimientos de stock inversos; ambos quedan visibles | Caja que no cierra y trazabilidad perdida |
| Historia clínica solo con agregados | Un registro cerrado no se sobrescribe: las correcciones son versiones o addenda con autor, fecha y motivo | Cambios silenciosos |
| Foto de la venta | Cada línea guarda precio, costo, descuento y área al momento de vender | Que un cambio de precio altere el pasado |
| Configuración con vigencia | Los porcentajes de costos por área tienen fecha desde la que rigen; cambiarlos crea una fila nueva | Reportes históricos que cambian solos |
| Idempotencia | Cada venta o turno lleva un UUID de operación generado al abrir el formulario, con restricción de unicidad | Duplicados por reintentos o doble toque |
| Archivar, no borrar | Baja lógica; el borrado físico solo por política de retención | Pérdida de historial y referencias rotas |
| Auditoría | Registro de eventos críticos: anulaciones, ediciones clínicas, cambios de precio o porcentaje, cambio de propietario, restauración | Operaciones sin responsable |

### 2.7 Modelo económico inicial

| Opción | Ventajas | Desventajas |
| --- | --- | --- |
| 1. Porcentaje de costos sobre la facturación de cada área | Simple, rápido de configurar, no exige cargar gastos | Impreciso: ignora diferencias de margen entre productos y se desactualiza con la inflación |
| 2. Costos reales de productos, servicios y gastos | Preciso y auditable | Exige mantener costos de compra y registrar gastos (alquiler, sueldos); si falta un dato, el resultado engaña |

**Recomendación: híbrido.** El costo de los productos vendidos se toma de datos reales (el producto ya tiene costo unitario, que queda congelado en cada línea de venta); los costos directos de servicios se usan si se cargan; y un porcentaje de gastos estimados por área, sobre la facturación neta, cubre lo que no se registra.

- Facturación neta = ventas brutas − descuentos − devoluciones
- Resultado estimado del área = facturación neta − costo de productos vendidos − costos directos de servicios − (facturación neta × % de gastos del área)

Reglas: el porcentaje nunca modifica el precio cobrado; la pantalla muestra siempre la base de cálculo y la fórmula; los resultados se rotulan como estimados; y cada venta usa el porcentaje vigente en su fecha, de modo que cambiar un valor hoy no reescribe los meses anteriores.

## 3. Definición del MVP

| Módulo | Entra en el MVP (v1.0) | Queda para después |
| --- | --- | --- |
| Clientes y mascotas | Alta, edición, búsqueda y filtros; detección de duplicados por teléfono y documento; cambio de propietario con historial; estado de la mascota | Contactos secundarios, importación CSV, fotos con recorte |
| Historia clínica | Consulta con motivo, examen, diagnóstico, tratamiento, medicación e indicaciones; correcciones por addendum | Adjuntos de estudios (imágenes y PDF), plantillas por tipo de consulta |
| Vacunaciones y recordatorios | Registro completo, cálculo de próxima fecha (6, 12 meses o fecha manual), estados, avisos a 30 y 7 días y vencidas, notificaciones locales, dashboard de alertas, marcar como atendido o reprogramar | Protocolos por especie con serie de dosis, desparasitación, mensaje de WhatsApp al propietario |
| Turnos | Calendario mensual, semanal y diario, estados, detección de superposiciones, vínculo con la historia clínica | Recordatorio de turno al propietario, reglas de disponibilidad por profesional |
| Catálogo e inventario | Productos y servicios, categorías, áreas, stock por movimientos, stock mínimo, lote y vencimiento en farmacia | Códigos de barras con cámara, actualización masiva de precios, compras a proveedores |
| Ventas y caja | Venta de productos y servicios, descuentos, pagos divididos, anulación total, apertura y cierre de caja | Devolución parcial por ítem, cuenta corriente, señas |
| Dashboard y reportes | Resumen del día, facturación de día, mes y año, ventas por área, stock bajo, vacunas próximas y vencidas, exportación CSV | Comparativas avanzadas, PDF de reportes, resultado económico detallado |
| Configuración | Establecimiento, horarios, áreas, medios de pago, categorías, anticipación de avisos, porcentajes por área con vigencia | Roles y permisos |
| Seguridad y datos | Bloqueo con PIN o biometría, backup y restauración cifrados, auditoría | Multiusuario y sincronización |

**El MVP está terminado cuando** una persona puede atender un día completo solo con la app: registra al cliente y su mascota, agenda un turno, anota la consulta, carga la vacuna y recibe el aviso de la próxima dosis, vende un producto con stock descontado, cierra la caja, y puede restaurar todo en otro dispositivo desde un backup.

## 4. Orden de etapas revisado

| Etapa | Contenido | Esfuerzo relativo | Cambio respecto del plan original |
| --- | --- | --- | --- |
| 0. Definición | Esta planificación, prototipos de baja fidelidad y una prueba técnica de Room más una notificación programada en el dispositivo real | S | Se agrega la prueba técnica para validar temprano el mayor riesgo |
| 1. Base | Proyecto, Room, navegación, tema lila, componentes, configuración, bloqueo con PIN, auditoría, esqueleto de backup y restauración | M | Seguridad y backup se adelantan: deben existir antes de cargar datos reales |
| 2. Clientes y mascotas | Fichas, búsqueda, duplicados, cambio de propietario | M | Sin cambios; el dashboard empieza a mostrar sus primeros indicadores |
| 3. Historia clínica y vacunaciones | Consultas, vacunas, cálculo de fechas, recordatorios, alertas en el dashboard | L | Prioridad máxima; sale primero porque es lo que más valor aporta |
| 4. Turnos y calendario | Calendario, estados, superposiciones, vínculo con la consulta | M | Pasa antes del catálogo: depende de clientes y mascotas, no de ventas |
| 5. Catálogo e inventario | Productos, servicios, áreas, categorías, movimientos de stock | M | Queda pegada a ventas para evitar retrabajo |
| 6. Ventas y caja | Venta, pagos divididos, descuentos, anulación, caja, configuración económica con vigencia | L | Incluye la configuración de porcentajes, que antes estaba al final |
| 7. Dashboard económico y reportes | Indicadores por día, mes y año, ventas por área, exportación CSV | M | El dashboard ya viene creciendo desde la etapa 2 |
| 8. Endurecimiento y puesta en marcha | Pruebas de integridad y rendimiento con datos masivos de prueba, restauración real, notificaciones en varios dispositivos, instalación y capacitación | M | Las pruebas no esperan a esta etapa: se ejecutan en cada una |
| Después del MVP | Sincronización, multiusuario y roles, y los puntos de la columna de pendientes de la sección 3 | L | Preparado por los principios de la sección 2.6 |

En la Parte 2 cada etapa se detallará con objetivo, dependencias, riesgos, pruebas y criterios de cierre.

## 5. Riesgos, costos y mantenimiento (preliminar)

| Riesgo | Impacto | Mitigación |
| --- | --- | --- |
| Pérdida o rotura del dispositivo | Pérdida total de datos | Backup cifrado automático a ubicación externa y prueba de restauración en las etapas 1 y 8 |
| Los recordatorios no se disparan (ahorro de batería, cierre forzado, permiso denegado) | Se incumple la función prioritaria | Prueba técnica temprana, recálculo al abrir la app, pantalla de diagnóstico, vencimientos siempre visibles en el dashboard |
| Crecimiento del alcance | El MVP nunca sale | Congelar la sección 3 y mover lo nuevo a una lista de espera de la v1.1 |
| Errores de cálculo en caja y stock | Pérdida de confianza en la app | Dinero en enteros, dominio con pruebas unitarias, ventas inmutables, conciliación de stock contra movimientos |
| Pasar a varios dispositivos tarde | Reescritura costosa | Principios de la sección 2.6 desde el inicio |
| Privacidad de datos personales | Exposición de clientes | Bloqueo, backup cifrado, sin datos personales en logs, revisar obligaciones de la Ley 25.326 |
| Actualizaciones de Android | Google Play exige subir el nivel de API objetivo cada año | Dependencias al día y tiempo de mantenimiento reservado cada año |
| Un solo desarrollador | Dependencia de una persona | Código modular, pruebas automáticas y documentación mínima |

| Concepto de costo | MVP | A futuro |
| --- | --- | --- |
| Cuenta de desarrollador de Google Play | US$25 pago único, solo si se publica en Play | Igual |
| Backend y sincronización | Cero | Desde plan gratuito hasta pago mensual según uso; a evaluar al decidir |
| Avisos por WhatsApp | Cero con mensaje prearmado | La API de WhatsApp Business tiene costo por conversación |
| Equipos de prueba | Un dispositivo real, idealmente de la marca que usará la veterinaria | Un segundo dispositivo para sincronización |
| Mantenimiento | Horas anuales para actualizar dependencias y nivel de API | Crece con la sincronización |

## 6. Próximos pasos

1. Responder las cinco preguntas de la sección 1.6 y confirmar o corregir las decisiones D1 a D7.
2. Redactar la Parte 2: especificación funcional, mapa de navegación, UX/UI lila, reglas de negocio completas, modelo de datos con diagrama E-R y plan detallado por etapa con criterios de aceptación.
3. Hacer los prototipos de baja fidelidad del dashboard, la ficha de mascota, el calendario y la pantalla de venta.
4. Ejecutar la prueba técnica de la etapa 0: proyecto base con Room y una notificación programada con WorkManager, probada en el dispositivo real con ahorro de batería activo.
5. Con todo lo anterior validado, abrir el repositorio y comenzar la etapa 1.

## 7. Plan v2 aprobado (23/10/2026)

### 7.1 Decisiones cerradas

- Una sola device, una sola cuenta, una sola veterinaria.
- Kotlin + Jetpack Compose; Room como fuente de verdad.
- Sync propio contra Supabase, sin Realtime: subida inmediata al detectar cambios.
- Supabase Free en producción, región São Paulo (`sa-east-1`).
- Bucket público `mascotas`; la foto se sube solo con conexión.
- Backup local cifrado semanal; no incluye fotos.
- MVP sin resultado económico ni lotes de inventario.

### 7.2 Sincronización

- Room + outbox en la misma transacción. Push inmediato con debounce y WorkManager con `NetworkType.CONNECTED` como respaldo.
- Push idempotente por RPC `apply_pet_operation` (se generalizará por entidad); `sync_operations` evita duplicados.
- Pull incremental al abrir, al reconectar y manualmente; primera sincronización completa.
- Sin pantalla de conflictos: una device, una cuenta. Se conservan `revision`, `updated_at`, `deleted_at` y `origin_device_id`.
- Si Supabase está pausado, la app sigue operando y el outbox espera. Se muestra el estado y se reanuda manualmente.
- Free no tiene backups automáticos: backup local cifrado obligatorio.

### 7.3 Fotos

- Galería con Photo Picker o cámara del sistema con FileProvider.
- Compresión en el dispositivo: máximo 800x800 sin agrandar, WebP lossy (JPEG fallback), calidad descendente hasta ≤100.000 bytes, EXIF/GPS eliminados y orientación corregida.
- Subida directa con `supabase-kt` Storage a `mascotas/{clinic_id}/{pet_id}/{uuid}.webp`, solo con conexión.
- Se guarda únicamente la URL pública en `foto_url`, actualizada en Room + outbox.
- Si no hay conexión, no se permite subir la foto; la ficha se puede guardar sin foto.
- Reemplazo: subir la nueva, actualizar la URL y borrar la anterior best-effort.

### 7.4 Seguridad y operación

- Auth email/contraseña; primer login online; sesión persistente; PIN/biometría local.
- RLS en todas las tablas; publishable/anon key en la app, nunca secret/service_role.
- Storage público para lectura; escritura y borrado autenticados y limitados por `clinic_id` en el path.
- Límites Free: 500 MB de base, 1 GB de Storage, 5 GB de egress; ~10.000 fotos de 100 KB.
- Pro (US$25/mes) queda como salida si la pausa o la falta de backups se vuelven un problema.

### 7.5 Etapa 0

Proyecto Supabase São Paulo, Auth, RLS, `clinics`/`clinic_members`, `pets`, `sync_operations`, `bootstrap_clinic`, `apply_pet_operation`, bucket `mascotas`; app con Room + outbox, login, alta de mascota, sync inmediato, pull, compresión y subida de foto. Criterio: modo avión, matar la app a mitad de push, sin duplicados, proyecto pausado simulado y restore local.

Equipo objetivo: **Samsung Galaxy S24 FE** (Android 14 o superior, One UI). Probar cámara, Photo Picker, ahorro de energía de One UI, sync con pantalla apagada, zoom de fuente y tema claro/oscuro. El modo HTTP sin TLS queda limitado a builds debug para Supabase local; producción usa HTTPS.

**Estado 24/10/2026:** etapa 0 implementada y verificada contra Supabase local: login, `bootstrap_clinic`, alta de mascota, sync inmediato, pull, modo avión con outbox, reconexión automática y foto comprimida a WebP de 8 KB subida al bucket `mascotas` con `foto_url` persistida. Pasan lint, tests unitarios y 4 tests instrumentados.

**Estado 24/10/2026:** etapa 1 completada salvo la validación en el S24 FE físico (pospuesta). Incluye backup local cifrado (AES-256-GCM, PBKDF2, frase de 8+ caracteres) con exportación/restauración por SAF, recordatorio semanal y pruebas instrumentadas de ida y vuelta; no incluye fotos. Agrega bloqueo con PIN de 4 a 8 dígitos (PBKDF2-HMAC-SHA256 con sal), biometría opcional, bloqueo al iniciar y tras 60 segundos en segundo plano, auditoría local de eventos críticos en Room v2 con migración 1→2, pantalla de Ajustes y auditoría incluida en el backup. Pasan lint, tests unitarios y 12 tests instrumentados en emulador API 37.

**Estado 24/10/2026 (etapa 2):** etapa 2 completada salvo la validación en el S24 FE físico (pospuesta). Incluye clientes con ficha, búsqueda y duplicados por teléfono/documento; vínculo obligatorio mascota-cliente al crear; estados activa/inactiva/fallecida; cambio de propietario con historial, motivo y auditoría; pestañas Inicio/Clientes/Mascotas con indicadores básicos; archivado de clientes bloqueado si tienen mascotas; sincronización idempotente de clientes por `apply_client_operation` y de mascotas con `client_id`/`status`; migración Room 2→3; y backup cifrado con clientes e historial. Las RPC de Supabase se validaron contra el entorno local y la UI se recorrió en modo local (alta de clientes y mascotas, cambio de estado, cambio de propietario e historial). Pasan lint, tests unitarios y 14 tests instrumentados en emulador API 37.
