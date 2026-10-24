# VeterApp

App Android de gestión veterinaria, local-first con Room y sincronización a Supabase.

## Estado actual: etapa 2 (clientes y mascotas)

Incluye:

- Proyecto Android con Kotlin, Compose, Material 3 y tema lila.
- Room local con clientes, mascotas, historial de propietario, outbox de sincronización y auditoría en la versión 3 del esquema.
- Migraciones automáticas de la base local de la versión 1 a la 2 y de la 2 a la 3.
- Login con Supabase Auth (email/contraseña).
- `bootstrap_clinic` para crear la clínica y la membresía del usuario.
- Push idempotente por RPC `apply_client_operation` y `apply_pet_operation`, con pull de clientes y mascotas.
- Sincronización inmediata al guardar, con WorkManager y reintentos.
- Clientes con ficha, teléfono, documento, email, dirección y notas; búsqueda y detección de duplicados por teléfono o documento.
- Mascotas vinculadas a un cliente, con estado activa, inactiva o fallecida.
- Cambio de propietario con historial, motivo y auditoría.
- Alta de mascota con foto desde galería o cámara.
- Compresión local a máximo 800x800 y 100 KB, en WebP o JPEG.
- Subida de foto solo con conexión al bucket público `mascotas`.
- Backup local cifrado manual (AES-256-GCM con frase) de la base, el outbox, clientes, historial y auditoría; no incluye fotos.
- Recordatorio de backup semanal en la pantalla principal.
- Bloqueo de la app con PIN de 4 a 8 dígitos y biometría opcional.
- Auditoría local de eventos críticos, visible desde Ajustes y restaurable desde un backup.
- Pantalla de Ajustes con seguridad, backup y auditoría.
- Pestañas Inicio, Clientes y Mascotas, con indicadores básicos de clientes y mascotas por estado.
- Modo local sin Supabase para probar la UI y Room.

Todavía no incluye: historias clínicas, vacunaciones, turnos, catálogo, ventas, caja, reportes avanzados, multiusuario ni resolución de conflictos de sincronización.

## Requisitos

- JDK 17 o superior.
- Android SDK con API 37 y build-tools 37.
- Una cuenta de Supabase (el plan Free alcanza para desarrollo).
- Un usuario de Auth creado en Supabase.

## Configurar Supabase

1. Crear un proyecto en la región **South America (São Paulo)**.
2. Abrir el SQL Editor y ejecutar, en orden, las migraciones:
   - `supabase/migrations/202610230001_stage0.sql`: crea `clinics`, `clinic_members`, `pets`, `sync_operations` y `audit_log`; crea `bootstrap_clinic` y `apply_pet_operation`; crea el bucket público `mascotas` y sus políticas.
   - `supabase/migrations/202610240001_stage2.sql`: crea `clients`, agrega `client_id` y `status` a `pets`, crea `apply_client_operation` y actualiza `apply_pet_operation`.
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
3. Al guardar un cliente o una mascota, se escriben la fila, su outbox y el evento de auditoría en la misma transacción de Room.
4. Se intenta el push inmediato; los clientes se envían antes que las mascotas para respetar el vínculo. Si no hay red, WorkManager lo reintenta al reconectar.
5. El pull trae clientes y mascotas de la clínica al abrir, al reconectar y manualmente.
6. La foto se comprime antes de subir y solo se permite con conexión. Se guarda únicamente la URL pública en `foto_url`.

## Clientes y mascotas

- **Clientes**: alta y edición con nombre, teléfono, documento, email, dirección y notas. La búsqueda filtra por nombre, teléfono, documento o email.
- **Duplicados**: al guardar un cliente se comparan teléfono y documento normalizados. Si hay coincidencias, la app las muestra y permite revisar o guardar de todos modos.
- **Archivar clientes**: un cliente con mascotas registradas no se puede archivar; primero hay que cambiar el propietario de sus mascotas.
- **Mascotas**: al crear una mascota se elige un cliente existente. El estado puede ser activa, inactiva o fallecida, y se filtra desde la pestaña Mascotas.
- **Cambio de propietario**: se hace desde la ficha de la mascota. Queda registrado en el historial con fecha, propietario anterior, propietario nuevo y motivo opcional.
- **Inicio**: muestra clientes activos, mascotas activas, inactivas y fallecidas, además del estado de sincronización y el recordatorio de backup.

## Bloqueo de la app

En **Ajustes > Seguridad**:

1. **Crear PIN**: entre 4 y 8 dígitos. Se guarda solo un hash PBKDF2-HMAC-SHA256 con sal aleatoria y 150.000 iteraciones, nunca el PIN en texto plano.
2. **Biometría**: se puede activar después de crear el PIN. Requiere autenticación exitosa para habilitarse.
3. **Bloquear ahora**: lleva al bloqueo sin esperar el segundo plano.
4. **Cambiar PIN** o **Quitar bloqueo**: piden el PIN actual.

La app se bloquea al abrirse y después de 60 segundos en segundo plano. Si la biometría está activa, la pantalla de bloqueo la ofrece automáticamente; el PIN siempre queda como respaldo. Tras 5 intentos incorrectos hay una pausa de 30 segundos.

Si se olvida el PIN no hay recuperación dentro de la app: hay que borrar los datos de la aplicación y restaurar desde el backup cifrado o desde Supabase. El PIN no se incluye en el backup. El bloqueo protege el acceso a la app, pero no cifra la base local; para el MVP se apoya en el cifrado del dispositivo y en el backup cifrado.

## Auditoría local

Se registran eventos críticos en la tabla `audit_log` del dispositivo:

- Alta, edición y baja de clientes.
- Alta, edición, cambio de estado y baja de mascotas.
- Cambios de propietario, con propietario anterior y nuevo.
- Exportación y restauración de backups.
- Configuración, cambio y baja del PIN.
- Activación y desactivación de la biometría.

Cuando hay sesión iniciada se guarda también el `actor_id` de Supabase. Los últimos 30 eventos se ven en **Ajustes > Auditoría** y todos se incluyen en el backup.

## Backup local cifrado

En **Ajustes > Backup**:

1. Escribir una frase de al menos 8 caracteres y repetirla.
2. **Guardar backup** abre el selector de archivos y escribe `veterapp-AAAAMMDD-HHmm.vbackup`.
3. **Restaurar** pide la misma frase y reemplaza los datos locales con el contenido del backup.
4. La app muestra un recordatorio si pasaron más de 7 días desde el último backup.

El backup incluye clientes, mascotas, historial de propietario, outbox y auditoría, no fotos. La frase no se guarda en el dispositivo: si se pierde, el backup no se puede recuperar. El archivo se verifica con cabecera `VBK1` y cifrado AES-256-GCM; una frase incorrecta falla sin exponer datos.

Restaurar exige iniciar sesión con la misma cuenta y clínica: el backup se rechaza si pertenece a otra clínica. No restaura el PIN ni la configuración del dispositivo. En un equipo nuevo, primero hay que iniciar sesión y después restaurar.

## Supabase Free

- El proyecto Free se pausa tras 7 días de baja actividad. La app sigue funcionando offline; hay que reanudar el proyecto desde el dashboard.
- Free no tiene backups automáticos: el backup local cifrado semanal es obligatorio antes de cargar datos reales.
- Límites: 500 MB de base, 1 GB de Storage y 5 GB de egress.
- Las fotos son públicas con URL aleatoria y no se incluyen en el backup local.

## Dispositivo objetivo: Samsung Galaxy S24 FE

La app está pensada para usarse principalmente en un Galaxy S24 FE (Android 14 o superior, One UI). Antes de usarla con datos reales, probar en el equipo:

- Iniciar sesión con el usuario real y verificar `bootstrap_clinic`.
- Crear clientes con teléfono y documento, y comprobar la detección de duplicados.
- Crear mascotas vinculadas, cambiar su estado y cambiar de propietario verificando el historial.
- Alta de mascota con foto desde **Galería** y desde **Cámara**.
- Confirmar que la foto sube solo con conexión y que `foto_url` llega a Supabase.
- Crear un PIN, habilitar biometría, bloquear la app y desbloquear con huella y con PIN.
- Comprobar el bloqueo automático tras un minuto en segundo plano y la reanudación de la sesión.
- Exportar un backup con frase, borrar los datos y restaurarlo en el equipo limpio.
- Verificar que la auditoría muestra los eventos y que el backup restaurado los conserva.
- Activar el ahorro de energía de One UI y comprobar que el sync se recupera al abrir la app o al reconectar.
- Excluir VeterApp de la optimización de batería si el sync en segundo plano se demora demasiado (Ajustes > Batería > Uso en segundo plano).
- Probar con zoom de fuente, tema claro/oscuro y navegación por gestos.
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

Las pruebas unitarias cubren el hash y la política de PIN, la normalización de teléfonos y documentos, la detección de duplicados, los estados de mascota, el mapeo de DTOs, el cálculo de dimensiones y la compresión de fotos, los mensajes de error y el cifrado del backup. Las pruebas instrumentadas cubren:

- CRUD de clientes, duplicados por teléfono o documento y bloqueo de archivado con mascotas.
- Creación de mascotas vinculadas, cambio de estado y cambio de propietario con historial.
- Backup cifrado de ida y vuelta con clientes, mascotas, historial, outbox y auditoría.
- Migraciones de Room de la versión 1 a la 2 y de la 2 a la 3.
- Creación, verificación, bloqueo, cambio y baja del PIN; límite de intentos y bloqueo por segundo plano.
- Pantalla de bloqueo: PIN correcto desbloquea y PIN incorrecto mantiene el contenido oculto.
- Escritura transaccional de mascota + outbox + auditoría.
- Compresión real a WebP/JPEG.

La suite instrumentada se ejecutó en un emulador API 37 (14 pruebas). La validación en el S24 FE físico queda pendiente.
