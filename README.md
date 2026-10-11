# Saber Pro - Aplicación Móvil de Preparación (Sede Maicao)

Aplicación móvil nativa para Android desarrollada en Kotlin y Jetpack Compose (`applicationId: com.aistudio.saberpro.maicao`).

## Arquitectura y Almacenamiento Local
- **100 % Local (Offline-First):** La aplicación no depende de ningún servidor ni backend externo.
- **Base de Datos Room:** Almacenamiento estructurado y versionado (`exportSchema = true`) que persiste:
  - Cuentas locales (`local_account`) con contraseña protegida mediante hash **PBKDF2 con sal** de 16 bytes e iteraciones criptográficas (`javax.crypto`).
  - Perfil del estudiante y evidencia de consentimiento informado (`user_profile`).
  - Intentos (`attempts`) con inserción incremental e idempotente por UUID.
  - Respuestas individuales (`attempt_answers`) con `answeredAt` real y bandera de revisión.
  - Progreso por pregunta en el método Leitner (`question_progress` con cajas 1..4, `lastAnsweredAt` y `dueAt`).
  - Ensayos de Comunicación Escrita y rúbricas de autoevaluación (`written_essays`).
  - Reportes de preguntas para revisión pedagógica (`question_reports`).

## Nota sobre Cifrado en Reposo con Android Keystore (AES-GCM)
- **Estado:** Pendiente para integración en fase de despliegue a dispositivo físico con hardware Keystore.
- **Justificación técnica:** La API de `AndroidKeyStore` (`KeyStore.getInstance("AndroidKeyStore")`) requiere el demonio nativo de Keystore del sistema operativo Android y claves ligadas a hardware/TEE. Para garantizar la ejecución determinista de la suite de pruebas unitarias locales en JVM sin dependencias de emulador o mocks inestables, los textos del ensayo y respuestas se almacenan directamente en las tablas locales privadas de Room de la aplicación protegidas por aislamiento de procesos de Android (`SANDBOX`). No se simula un cifrado falso.

## Privacidad y Respaldos (Ley 1581 de 2012)
- `android:allowBackup="false"` configurado en `AndroidManifest.xml`.
- `backup_rules.xml` y `data_extraction_rules.xml` excluyen estrictamente las bases de datos y preferencias compartidas de cualquier copia en la nube o transferencia entre dispositivos.
- Consentimiento informado obligatorio verificado en la capa de datos (`UserDataRepository`) antes de autorizar escrituras.

## Derivación Criptográfica de Contraseñas (PBKDF2) y Compatibilidad
- **Algoritmo estándar:** `PBKDF2WithHmacSHA256` con 600.000 iteraciones (guía vigente OWASP Password Storage Cheat Sheet) y sal criptográfica de 16 bytes.
- **Formato versionado:** Almacenamiento bajo el formato `v1$<iteraciones>$<salHex>$<hashHex>`, con re-hasheo automático transparente de cuentas heredadas de 10.000 iteraciones al iniciar sesión.
- **Comparación en tiempo constante:** Verificación mediante `MessageDigest.isEqual` para mitigar ataques de temporización (*timing attacks*).
- **Ejecución en hilo de fondo:** Operaciones de derivación y verificación ejecutadas obligatoriamente fuera del hilo principal (`Dispatchers.Default`).
- **Compatibilidad API 24-25 (Android 7.0 / 7.1):** En niveles de API 24 y 25, si el proveedor criptográfico del sistema no tiene implementado `PBKDF2WithHmacSHA256`, `PasswordHasher` realiza automáticamente un fallback transparente a `PBKDF2WithHmacSHA1`, preservando las iteraciones y la sal aleatoria para garantizar el funcionamiento continuo sin bloqueos ni errores.
