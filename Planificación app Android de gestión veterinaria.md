# Planificación: app Android de gestión veterinaria

*Documento de planificación · Parte 1: análisis, decisiones de arquitectura y MVP · Parte 2: especificación de la etapa 4 · 24 de octubre de 2026*

## Resumen ejecutivo

- **Recomendación central:** app nativa en Kotlin con Jetpack Compose y base local Room (SQLite) como fuente de verdad. Arranca en un solo dispositivo, sin servidor y sin costo recurrente, pero se diseña local-first: identificadores, versionado y operaciones quedan preparados para sumar sincronización más adelante sin reescribir.
- **MVP:** clientes, mascotas, historia clínica básica, vacunaciones con recordatorios (prioridad del proyecto), turnos, catálogo con stock, ventas con caja y un dashboard con facturación por área. Incluye backup/restauración y bloqueo de la app.
- **Cambios al plan original:** backup y seguridad base se adelantan (antes de cargar datos reales), turnos pasan antes del catálogo y el dashboard se construye de forma incremental en lugar de al final.
- **Antes de programar** hay que confirmar siete decisiones (sección 1.4) y responder cinco preguntas (sección 1.6).
- Esta Parte 1 cubre análisis, arquitectura y MVP. La Parte 2, iniciada con la etapa 4, desarrolla la especificación funcional por módulo, el mapa de navegación, el diseño UX/UI lila, las reglas de negocio, el modelo de datos, el plan detallado por etapa y los criterios de aceptación.

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
| 1 | Los recordatorios llegan al personal, no al dueño | La notificación local avisa al dispositivo de la veterinaria, pero quien tiene que venir a vacunar es el propietario | Mensaje de WhatsApp prearmado (enlace wa.me, sin costo, un toque) desde la etapa 4 del MVP; la API de WhatsApp Business solo si hay volumen, porque tiene costo y requiere aprobación |
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
| D6 | ¿Avisos al propietario? | Solo al personal / WhatsApp prearmado / API de WhatsApp Business | Mensaje prearmado desde la etapa 4 del MVP; la API solo si el volumen lo justifica |
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
| Turnos | Calendario mensual, semanal y diario, estados, detección de superposiciones con confirmación, horario del local configurable, vínculo con la historia clínica, recordatorio por WhatsApp prearmado al propietario | Reglas de disponibilidad por profesional, notificaciones automáticas de turno y reprogramación masiva |
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
| 4. Turnos y calendario | Calendario, estados, superposiciones, horario del local, vínculo con la consulta y WhatsApp prearmado | M | Pasa antes del catálogo: depende de clientes y mascotas, no de ventas |
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
- Turnos con un solo profesional, horario del local configurable y recordatorio por WhatsApp prearmado desde la etapa 4.

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

**Estado 24/10/2026 (etapa 3):** etapa 3 completada salvo la validación en el S24 FE físico (pospuesta). Incluye historia clínica por mascota con consultas cerradas y addenda con autor, fecha y motivo; vacunaciones con próxima dosis a 6 o 12 meses, fecha manual o sin aviso; estados vencida, por vencer, próxima y programada; recordatorios locales diarios con WorkManager a 30 y 7 días y al vencer, deduplicados por dosis y umbral; dashboard de alertas en Inicio con acceso a la mascota; aplicar dosis y reprogramar con auditoría; sincronización idempotente por `apply_consultation_operation`, `apply_addendum_operation` y `apply_vaccination_operation`; migración Room 3→4; y backup cifrado con historia, vacunaciones y avisos emitidos. Las RPC de Supabase se validaron contra el entorno local. Pasan lint, tests unitarios y 17 tests instrumentados en emulador API 37.

## 8. Parte 2 — Especificación detallada de la etapa 4: turnos y calendario

### 8.1 Objetivo y alcance

La etapa 4 incorpora la agenda de la veterinaria para ordenar la atención diaria, evitar superposiciones y conectar cada turno con la historia clínica. Es la primera etapa que no depende del catálogo ni de ventas: se apoya en clientes, mascotas y consultas ya existentes.

**Entra en la etapa 4:**

- Pestaña **Turnos** con vistas mensual, semanal y diaria.
- Alta, edición y reprogramación de turnos con mascota, fecha, hora, duración, motivo y notas.
- Estados: programado, confirmado, atendido, cancelado y ausente.
- Detección de superposiciones y de turnos fuera del horario del local, con confirmación explícita.
- Horario de atención configurable por día y duración predeterminada configurable.
- Vínculo con la historia clínica: al atender un turno se registra la consulta y el turno guarda su identificador.
- Recordatorio por WhatsApp prearmado al propietario, con enlace `wa.me`.
- Turnos del día en Inicio y acciones desde el detalle del turno.
- Sincronización idempotente con Supabase, backup cifrado y auditoría.

**Queda fuera de la etapa 4:**

- Agenda por profesional y disponibilidad por profesional.
- Notificaciones automáticas de turno al propietario (solo WhatsApp manual en esta etapa).
- Turnos recurrentes, lista de espera, sobreturnos automáticos y reprogramación masiva.
- Cobro o seña desde el turno; se resuelve en la etapa 6.

### 8.2 Decisiones cerradas

| Tema | Decisión |
| --- | --- |
| Profesionales | Un solo profesional y una sola agenda. El modelo no agrega `professional_id`; se podrá sumar después con una migración compatible. |
| Disponibilidad | Horario por día configurable en Ajustes. Un día cerrado o un turno fuera de horario genera una advertencia, no un bloqueo absoluto. |
| Duración | Duración predeterminada configurable (30 minutos por defecto) y duración ajustable en cada turno. |
| Superposiciones | Se avisan y se permiten con confirmación. El aviso muestra los turnos en conflicto. |
| Recordatorio | WhatsApp prearmado desde el detalle del turno; no se envía automáticamente. |
| Zona horaria | America/Argentina/Buenos_Aires para fecha, hora y día comercial. |
| Identidad | UUID generado en el dispositivo; el turno es sincronizable e idempotente. |

### 8.3 Modelo de datos

**Tabla local `appointments` (Room v5):**

| Campo | Tipo | Regla |
| --- | --- | --- |
| `id` | TEXT | UUID, clave primaria |
| `clinicId` | TEXT | Clínica del dispositivo |
| `petId` | TEXT | Mascota existente y no archivada |
| `startsAt` | INTEGER | Instante UTC en milisegundos |
| `endsAt` | INTEGER | Instante UTC, siempre mayor que `startsAt` |
| `status` | TEXT | `scheduled`, `confirmed`, `attended`, `cancelled`, `no_show` |
| `reason` | TEXT | Motivo obligatorio |
| `notes` | TEXT | Notas opcionales |
| `consultationId` | TEXT | Consulta vinculada, opcional hasta atender |
| `cancelReason` | TEXT | Motivo de cancelación opcional |
| `createdAt`, `updatedAt` | INTEGER | Auditoría temporal |
| `revision` | INTEGER | Versión para sincronización |
| `deletedAt` | INTEGER | Baja lógica |
| `originDeviceId` | TEXT | Dispositivo de origen |
| `pendingSync` | INTEGER | Cambio pendiente de subir |

**Tabla remota `public.appointments`:** mismos campos en `snake_case`, `starts_at` y `ends_at` como `timestamptz`, `consultation_id` con clave foránea a `consultations`, restricciones `ends_at > starts_at` y estados válidos. RLS de lectura por membresía de clínica; escritura solo por RPC.

**Configuración local:** `defaultDurationMinutes` y `businessHours` (siete días con cerrado, apertura y cierre) se guardan en `AppSettings`. No se sincronizan en esta etapa.

### 8.4 Reglas de negocio

1. **Validación:** motivo obligatorio, mascota existente, fin posterior al inicio y duración entre 5 minutos y 8 horas.
2. **Mascota:** no se crean turnos para mascotas fallecidas o archivadas. Una mascota inactiva puede conservar turnos existentes.
3. **Estados y transiciones:**
   - `scheduled` → `confirmed`, `attended`, `cancelled`, `no_show`.
   - `confirmed` → `attended`, `cancelled`, `no_show`.
   - `attended`, `cancelled` y `no_show` son finales.
   - Solo se editan turnos `scheduled` o `confirmed`.
4. **Superposición:** dos turnos se superponen si sus intervalos `[inicio, fin)` se cruzan. Se comparan solo turnos vigentes en estado `scheduled` o `confirmed`; el turno editado se excluye a sí mismo. Si hay conflicto, la app muestra los turnos afectados y exige confirmación.
5. **Horario:** se compara el día local del inicio y el fin. Si el día está cerrado, o el inicio es anterior a la apertura, o el fin es posterior al cierre, se muestra una advertencia y se exige confirmación. Un turno que cruza la medianoche se considera fuera de horario.
6. **Atender:** abre el formulario de consulta con fecha y motivo precargados. Al guardar la consulta, el turno pasa a `attended` y guarda `consultationId`. Si la consulta no se puede guardar, el turno no cambia.
7. **Cancelar:** guarda el motivo opcional, no borra el turno y deja de contar para superposiciones. **Ausente** registra que el propietario no asistió.
8. **WhatsApp:** solo se ofrece si el propietario tiene teléfono. Se normaliza el número argentino (54 9 + área + número) y se abre `wa.me` con un mensaje que incluye mascota, fecha, hora y motivo. No se envía nada sin acción del usuario.
9. **Inicio:** muestra la cantidad de turnos de hoy y los próximos, con acceso a la agenda.
10. **Auditoría:** se registran alta, edición, confirmación, cancelación, ausencia, atención y cambios de horario. No se registra la apertura de WhatsApp.

### 8.5 UX y navegación

- **Pestaña Turnos** en la barra principal, junto a Inicio, Clientes y Mascotas.
- **Vista mensual:** grilla de lunes a domingo, día seleccionado, cantidad de turnos por día y agenda del día debajo.
- **Vista semanal:** siete secciones de lunes a domingo con sus turnos.
- **Vista diaria:** agenda cronológica del día con hora, mascota, propietario y estado.
- **Formulario de turno:** selector de mascota con búsqueda, fecha, hora de inicio, duración, motivo y notas. Muestra la hora de fin calculada.
- **Advertencias:** diálogo con los turnos en conflicto y/o el horario del local, con las opciones **Volver** y **Guardar igual**.
- **Detalle del turno:** datos completos, estado, acciones de confirmar, cancelar, ausente, atender, editar y WhatsApp.
- **Inicio:** tarjeta **Turnos de hoy** con cantidad, próximo turno y botón para abrir la agenda.
- **Ajustes:** tarjeta **Agenda** con duración predeterminada y horario de apertura y cierre por día.
- **Ficha de mascota:** los turnos se abren desde la agenda; el vínculo con la historia clínica se completa al atender.

### 8.6 Sincronización

- El alta o la edición del turno escribe la fila, el outbox y la auditoría en la misma transacción de Room.
- El outbox usa la entidad `appointment` y se envía después de `pet` y de `consultation` para respetar la clave foránea de `consultationId`.
- Push por RPC `apply_appointment_operation(p_operation_id, p_clinic_id, p_device_id, p_appointment)`; `sync_operations` evita duplicados y la función valida clínica, mascota, consulta, fechas y estado.
- Pull de `appointments` al abrir, al reconectar y manualmente, después de consultas y vacunaciones.
- Si Supabase está pausado o sin conexión, el turno queda local y el outbox reintenta con WorkManager.

### 8.7 Backup y auditoría

- El backup cifrado sube a la versión 3 e incluye `appointments`. Los backups anteriores se restauran con la lista vacía.
- Se mantiene la validación de clínica y la restauración transaccional.
- La auditoría local suma los eventos de turnos y de configuración de agenda; se incluyen en el backup y se ven en Ajustes.

### 8.8 Pruebas

**Unitarias:**

- Validación de borrador (motivo, fechas, duración).
- Detección de superposición con intervalos que se tocan y que se cruzan.
- Horario por día: abierto, cerrado, antes de apertura y después del cierre.
- Normalización de teléfono y armado del enlace de WhatsApp.
- Mapeo de DTO y estados.

**Instrumentadas:**

- Alta de turno con outbox y auditoría.
- Conflicto de superposición, reintento con confirmación y guardado.
- Turno fuera de horario con confirmación.
- Confirmar, cancelar, marcar ausente y atender con consulta vinculada.
- Observación por rango con nombre de mascota y teléfono del propietario.
- Migración Room 4 → 5.
- Backup de ida y vuelta con turnos.
- Recorrido de UI de la agenda y del formulario en emulador.

### 8.9 Criterios de aceptación

1. Se puede crear un turno desde la pestaña Turnos y desde el botón de Inicio, con mascota, fecha, hora, duración y motivo.
2. El calendario muestra los turnos en las vistas mensual, semanal y diaria.
3. Un turno superpuesto muestra los conflictos y solo se guarda con confirmación explícita.
4. Un turno fuera del horario configurado muestra la advertencia y solo se guarda con confirmación explícita.
5. Los estados se pueden cambiar según las transiciones permitidas y quedan auditados.
6. Al atender un turno se registra la consulta, se vincula y el turno queda como atendido.
7. El botón de WhatsApp abre la conversación con el mensaje correcto cuando hay teléfono.
8. El horario y la duración predeterminada se configuran en Ajustes y se aplican a los turnos nuevos.
9. Inicio muestra los turnos del día.
10. El turno se sincroniza con Supabase sin duplicados y se restaura desde el backup cifrado.
11. Pasan lint, pruebas unitarias y pruebas instrumentadas en emulador; la validación en el S24 FE físico queda pendiente.

**Estado 24/10/2026 (etapa 4):** etapa 4 completada salvo la validación en el S24 FE físico (pospuesta). Incluye pestaña Turnos con vistas mensual, semanal y diaria; alta y edición con mascota, fecha, hora, duración, motivo y notas; estados programado, confirmado, atendido, cancelado y ausente; detección de superposiciones y de horarios fuera de atención con confirmación explícita; horario por día y duración predeterminada configurables en Ajustes; atención con consulta clínica vinculada; recordatorio por WhatsApp prearmado; turnos del día en Inicio; sincronización idempotente por `apply_appointment_operation`; migración Room 4→5; backup cifrado con turnos; y auditoría de turnos y agenda. Se validó la RPC contra Supabase local, el recorrido de UI en emulador API 37 y el flujo completo hasta el turno atendido sincronizado con su consulta. Pasan lint, 46 tests unitarios y 19 tests instrumentados.
