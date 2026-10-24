# VeterApp

App Android de gestión veterinaria, local-first con Room y sincronización a Supabase.

## Estado actual: etapa 7 (dashboard económico y reportes)

Incluye las etapas 0 a 7 del plan, salvo las validaciones en el S24 FE físico.

- Proyecto Android con Kotlin, Compose, Material 3 y tema lila.
- Room local en la **versión 8** con clientes, mascotas, pesos, historial de propietario, consultas, addenda, vacunaciones, avisos, turnos, áreas, categorías, catálogo, movimientos de stock, medios de pago, ventas, líneas, pagos, cajas, movimientos de caja, anulaciones, numeración, porcentajes por área, outbox y auditoría.
- Migraciones automáticas de la base local de la versión 1 a la 8.
- Login con Supabase Auth (email/contraseña) y `bootstrap_clinic`.
- Push idempotente por RPC para clientes, mascotas, pesos, consultas, addenda, vacunaciones, turnos, áreas, categorías, catálogo, stock, medios de pago, ventas, anulaciones, cajas y movimientos de caja.
- Pull incremental al abrir, al reconectar y manualmente.
- Sincronización inmediata al guardar, WorkManager y reintentos con backoff.
- Clientes con ficha, búsqueda y detección de duplicados por teléfono o documento.
- Mascotas con sexo, nacimiento o edad aproximada, color, señas, microchip, estado reproductivo, alergias y antecedentes.
- Evolución de peso en gramos con gráfico simple, edición y borrado lógico.
- Cambio de propietario con historial, motivo y auditoría.
- Historia clínica con consultas cerradas y addenda con autor, fecha y motivo.
- Vacunaciones con próxima dosis a 6 o 12 meses, fecha manual o sin aviso.
- Recordatorios locales configurables (anticipación mensual, semanal y aviso de vencidas), deduplicados por dosis y umbral.
- Turnos con vistas mensual, semanal y diaria, estados, superposiciones, horario del local y WhatsApp prearmado.
- Turnos visibles en las fichas de mascota y cliente.
- Catálogo unificado de productos y servicios con áreas, categorías, subcategorías, códigos, costo, precio, stock mínimo, lote y vencimiento.
- Inventario por movimientos inmutables (entrada, ajuste y pérdida), saldo cacheado verificable, stock bajo y alertas de vencimiento.
- Ventas con cliente opcional, líneas por área, descuentos de línea y global prorrateado, pagos divididos, efectivo y vuelto.
- Comprobante interno no fiscal, compartible desde el dispositivo.
- Anulación total con reversión de stock y efectivo.
- Caja con apertura, retiros, gastos, ajustes, cierre y arqueo. Solo una caja abierta por clínica.
- Numeración legible `V-AAAAMMDD-NNNN`, única por clínica y por día, generada sin conexión.
- Porcentajes de gastos por área con vigencia y congelados en cada línea de venta.
- Dashboard de Inicio con resumen del día, indicadores económicos, accesos rápidos, turnos, vacunas, stock y caja.
- Reportes de facturación, ventas por área, costos y resultado estimado, medios de pago, más vendidos, inventario, turnos, vacunas, clientes y mascotas, caja y evolución.
- Drill-down desde ventas, vacunas e inventario.
- Exportación CSV con BOM UTF-8 y separador punto y coma; PDF de cierre de caja.
- Navegación principal de cinco destinos: Inicio, Agenda, Clientes, Ventas y Más, con barra inferior en teléfono y riel lateral en tablet.
- Backup local cifrado (AES-256-GCM) versión 6 con todas las tablas, outbox y auditoría; no incluye fotos.
- Bloqueo con PIN de 4 a 8 dígitos y biometría opcional.
- Auditoría local de eventos críticos, visible desde Ajustes.
- Alta de mascota con foto desde galería o cámara, compresión local y subida solo con conexión.
- Modo local sin Supabase para probar la UI y Room.

Todavía no incluye: protocolos por especie, agenda por profesional, devolución parcial por ítem, stock por lote, compras a proveedores, facturación fiscal, cuenta corriente, señas, adjuntos clínicos, roles, multiusuario ni resolución de conflictos de sincronización.

## Requisitos

- JDK 17 o superior.
- Android SDK con API 37 y build-tools 37.
- Una cuenta de Supabase (el plan Free alcanza para desarrollo).
- Un usuario de Auth creado en Supabase.

## Configurar Supabase

1. Crear un proyecto en la región **South America (São Paulo)**.
2. Abrir el SQL Editor y ejecutar, en orden, las migraciones:
   - `supabase/migrations/202610230001_stage0.sql`: clínicas, membresías, mascotas, `sync_operations`, auditoría, bucket `mascotas`, `bootstrap_clinic` y `apply_pet_operation`.
   - `supabase/migrations/202610240001_stage2.sql`: clientes, `client_id`/`status` en mascotas, historial y `apply_client_operation`.
   - `supabase/migrations/202610240002_stage3.sql`: consultas, addenda, vacunaciones y sus RPC.
   - `supabase/migrations/202610240003_stage4.sql`: turnos y `apply_appointment_operation`.
   - `supabase/migrations/202610240004_stage45.sql`: campos clínicos de mascota, `pet_weights` y `apply_pet_weight_operation`; actualiza `apply_pet_operation`.
   - `supabase/migrations/202610240005_stage5.sql`: áreas, categorías, catálogo, movimientos de stock y sus RPC.
   - `supabase/migrations/202610240006_stage6.sql`: medios de pago, ventas, líneas, pagos, cajas, movimientos, anulaciones, porcentajes por área y sus RPC.
3. En **Authentication > Users**, crear el usuario de la veterinaria con email y contraseña.
4. Copiar la URL del proyecto y la **publishable/anon key** desde **Project Settings > API**.
5. Completar `local.properties`:

```properties
sdk.dir=C\:\\AndroidSdk
SUPABASE_URL=https://TU-PROYECTO.supabase.co
SUPABASE_ANON_KEY=TU_PUBLISHABLE_O_ANON_KEY
```

No commitear `local.properties`. No usar nunca la secret/service_role key en la app.

## Compilar y ejecutar

```powershell
$env:ANDROID_HOME = "C:\AndroidSdk"
$env:ANDROID_SDK_ROOT = "C:\AndroidSdk"
.\gradlew.bat assembleDebug
```

Abrir el proyecto en Android Studio y ejecutar en un dispositivo o emulador con Android 8.0 (API 26) o superior. También se puede instalar el APK de `app/build/outputs/apk/debug/`.

Sin `SUPABASE_URL` y `SUPABASE_ANON_KEY`, la app arranca en **modo local**: Room funciona, la sincronización queda pendiente y las fotos se deshabilitan.

## Flujo de sincronización

1. Primer inicio de sesión online.
2. `bootstrap_clinic` devuelve la clínica del usuario.
3. La app crea localmente las cinco áreas y los tres medios de pago iniciales con identificadores determinísticos y los encola para subir.
4. Al guardar, se escriben la fila, su outbox y el evento de auditoría en la misma transacción de Room.
5. El push respeta el orden de dependencias: clientes, áreas, categorías, medios de pago, porcentajes, mascotas, pesos, consultas, addenda, vacunas, turnos, catálogo, stock, cajas, ventas, anulaciones y movimientos de caja.
6. El pull trae todas las entidades de la clínica al abrir, al reconectar y manualmente.
7. Si Supabase está pausado o sin conexión, la app sigue funcionando; el outbox reintenta con WorkManager.

## Mascotas, historia y turnos

- **Mascotas**: ficha con datos clínicos completos, estado activa/inactiva/fallecida, foto y evolución de peso.
- **Pestañas de la ficha**: Resumen, Historia, Vacunas, Turnos y Peso.
- **Consultas**: fecha, motivo, examen, diagnóstico, tratamiento, medicación e indicaciones. Al guardar quedan cerradas.
- **Addendum**: corrige o amplía una consulta sin modificarla.
- **Vacunas**: aplicación, lote, notas y próxima dosis a 6 o 12 meses, manual o sin aviso. Estados vencida, por vencer, próxima y programada.
- **Recordatorios**: se configuran en **Ajustes > Recordatorios**. Se avisa a los días mensuales, a los días semanales y, opcionalmente, al vencer.
- **Turnos**: alta, edición, estados, superposiciones y horario fuera de atención con confirmación. Desde la ficha del cliente o la mascota se consultan sus turnos.
- **WhatsApp**: mensaje prearmado desde el detalle del turno cuando el cliente tiene teléfono.

## Catálogo e inventario

Acceso: **Más > Inventario**.

- **Productos**: código interno único, código de barras opcional, nombre, descripción, marca, categoría, área, costo, precio, stock mínimo, lote y vencimiento.
- **Servicios**: área, categoría, precio, duración y costo directo opcional. No manejan stock.
- **Áreas iniciales**: Consultorio, Farmacia, Pet shop, Peluquería y Alimentos. Se administran y renombran en **Ajustes > Catálogo**.
- **Categorías**: máximo dos niveles; una subcategoría depende de una categoría de primer nivel.
- **Stock**: libro de movimientos inmutable. El saldo se actualiza en la misma transacción y se puede verificar contra la suma de movimientos.
- **Stock negativo**: bloqueado por defecto; se habilita en **Ajustes > Inventario** con confirmación explícita y auditoría.
- **Stock bajo**: producto activo con stock menor o igual al mínimo.
- **Vencimientos**: vencidos y próximos a vencer (30 días) se muestran en la lista y en los reportes.
- **Códigos internos**: se generan automáticamente si se dejan vacíos.
- **Baja**: un ítem con historial se desactiva; no se borra.

## Ventas y caja

Acceso: pestaña **Ventas**.

- **Abrir caja**: monto inicial y notas. Solo puede haber una caja abierta.
- **Nueva venta**: cliente opcional, buscador de catálogo, cantidades, descuento por línea, descuento global prorrateado, notas y total en vivo.
- **Cobro**: uno o varios medios de pago; la suma debe igualar el total. En efectivo se registra el efectivo recibido y el vuelto.
- **Comprobante**: interno y no fiscal, con número, fecha, cliente, líneas por área, descuentos, total, pagos y vuelto. Se puede compartir con el selector del sistema.
- **Anulación**: solo total, con motivo obligatorio. Revierte stock y efectivo; conserva la venta y queda auditada.
- **Caja**: retiros, gastos y ajustes con motivo. El cierre muestra efectivo esperado, contado y diferencia.
- **Cierre en PDF**: desde el detalle de una caja cerrada.
- **Numeración**: `V-AAAAMMDD-NNNN`, única por clínica. El contador local se incluye en el backup.
- **Porcentajes de gastos por área**: en **Ajustes > Economía**, con vigencia desde la fecha. Cada línea de venta congela el porcentaje aplicado.

## Dashboard y reportes

- **Inicio**: accesos rápidos, facturación y resultado estimado del día, mes y año, comparación con el período anterior, turnos, vacunas, stock y caja.
- **Más > Reportes**:
  - Facturación por período.
  - Ventas por área con participación.
  - Costos y resultado estimado por área.
  - Medios de pago.
  - Productos y servicios más vendidos.
  - Inventario con stock bajo y vencimientos.
  - Turnos por estado.
  - Vacunaciones vencidas, por vencer y próximas.
  - Clientes nuevos y mascotas registradas.
  - Caja y evolución diaria.
- **Filtros**: área, medio de pago y tipo según el reporte, además del período.\n- **Drill-down**: las ventas abren su detalle; las vacunas abren la ficha de la mascota; los turnos muestran su detalle; la caja abre el arqueo con PDF; el inventario abre el módulo.
- **Exportación**: CSV con BOM UTF-8 y separador `;` para Excel es-AR. El PDF de cierre de caja incluye esperado, contado y diferencia.
- **Resultado estimado**: usa costos y porcentajes congelados en cada línea. No es contabilidad formal.

## Backup local cifrado

En **Ajustes > Backup**:

1. Escribir una frase de al menos 8 caracteres y repetirla.
2. **Guardar backup** abre el selector y escribe `veterapp-AAAAMMDD-HHmm.vbackup`.
3. **Restaurar** pide la misma frase y reemplaza los datos locales.
4. La app recuerda el último backup y avisa si pasaron más de 7 días.

El backup versión 6 incluye clientes, mascotas, pesos, historial, consultas, addenda, vacunaciones, avisos, turnos, áreas, categorías, catálogo, movimientos de stock, medios de pago, ventas, líneas, pagos, cajas, movimientos de caja, anulaciones, numeración, porcentajes por área, outbox y auditoría. No incluye fotos. Al restaurar se valida la clínica, se recalcula el stock si no coincide con los movimientos y se auditan las diferencias de ventas o caja. Los backups anteriores se restauran con las tablas nuevas vacías.

## Seguridad y auditoría

- **PIN**: 4 a 8 dígitos, hash PBKDF2-HMAC-SHA256 con sal. La app se bloquea al abrir y tras 60 segundos en segundo plano.
- **Biometría**: opcional, después de crear el PIN.
- **Auditoría**: altas y ediciones, cambios de propietario, consultas y addenda, vacunas, turnos, pesos, catálogo, stock, áreas, categorías, medios de pago, ventas, anulaciones, caja, backups, PIN y exportaciones.
- Los últimos eventos se ven en **Ajustes > Auditoría** y todos se incluyen en el backup.
- El PIN no se incluye en el backup. Si se olvida, hay que borrar los datos y restaurar desde un backup o Supabase.

## Supabase Free

- El proyecto Free se pausa tras 7 días de baja actividad. La app sigue funcionando offline.
- Free no tiene backups automáticos: el backup local cifrado es obligatorio antes de cargar datos reales.
- Límites: 500 MB de base, 1 GB de Storage y 5 GB de egress.
- Las fotos son públicas con URL aleatoria y no se incluyen en el backup local.

## Dispositivo objetivo: Samsung Galaxy S24 FE

Antes de usar la app con datos reales, probar en el equipo:

- Login, `bootstrap_clinic`, sync y restauración de la sesión.
- Clientes con duplicados, mascotas con todos los campos clínicos y cambio de propietario.
- Pesos: alta, evolución, borrado lógico y sync.
- Consultas, addenda y vacunas con recordatorios configurables.
- Turnos: vistas, superposiciones, fuera de horario, atención con consulta y WhatsApp.
- Turnos visibles en las fichas de mascota y cliente.
- Catálogo: producto con lote y vencimiento, servicio, códigos únicos, stock inicial, entrada, ajuste y pérdida.
- Stock bajo y vencimientos visibles en el inventario y en el dashboard.
- Venta con tres áreas, descuento global, pago dividido, vuelto y comprobante.
- Anulación con reversión de stock y efectivo.
- Caja: apertura, retiro, gasto, ajuste, cierre, arqueo y PDF.
- Reportes: conciliación por área, medios de pago, resultado estimado y CSV abierto en Excel es-AR.
- Backup v6 con todas las tablas y restauración en un equipo limpio.
- Notificaciones con la app cerrada y ahorro de energía de One UI.
- Zoom de fuente, tema claro/oscuro y navegación por gestos.
- Verificar que el APK debug no se distribuya: el tráfico HTTP solo está permitido en debug.

Para instalar en el S24 FE por USB:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Pruebas

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat lintDebug
```

Las pruebas unitarias cubren el hash y la política de PIN, teléfonos y documentos, duplicados, estados de mascota, próxima dosis y estados de vacuna, planificador de recordatorios, validación de consultas y addenda, validación de turnos, superposiciones y horarios, WhatsApp, mapeo de DTOs, compresión de fotos, cifrado del backup, validación de catálogo y stock, descuentos y totales de venta, prorrateo, porcentajes por área, períodos y variaciones de reportes.

Las pruebas instrumentadas cubren:

- CRUD de clientes, duplicados y bloqueo de archivado con mascotas.
- Mascotas vinculadas, cambio de estado, cambio de propietario y pesos.
- Consultas, addenda, vacunas, reprogramación y recordatorios.
- Turnos con superposiciones, confirmación, cancelación, ausencia, atención y consulta vinculada.
- Catálogo, movimientos de stock, stock bajo y vencimientos.
- Ventas, pagos divididos, anulación, caja y arqueo (cuando se ejecuten en emulador).
- Backup cifrado de ida y vuelta con todas las tablas.
- Migraciones de Room de la versión 1 a la 8.
- PIN, biometría, pantalla de bloqueo y bloqueo por segundo plano.
- Compresión real a WebP/JPEG.

La suite unitaria y las 22 pruebas instrumentadas se ejecutaron en emulador API 37. La validación en el S24 FE físico queda pendiente.
