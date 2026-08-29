# Memento

Memento es una aplicación Android local-first para construir una memoria cultural personal. Separa cada obra de las veces que la lees, ves o juegas, y conserva progreso, valoraciones, notas y reflexiones a lo largo del tiempo.

## Capturas

Sección preparada. Las capturas se añadirán tras la validación visual final en un dispositivo compatible.

## Funcionalidad

- biblioteca de libros, películas, series y videojuegos con búsqueda, filtros y orden;
- alta manual o búsqueda en TMDB, Open Library y RAWG; al elegir un resultado se descarga su ficha completa antes de revisar y persistir;
- pendientes, progreso específico por tipo, finalización, abandono, medias estrellas y favoritos;
- múltiples consumos por obra, notas, reflexión final, reflexiones posteriores y timeline derivada;
- edición de metadata y reflexiones, historial visible y eliminación independiente de consumos con confirmación;
- Remember con sorteo ponderado estable por día, historial de exposiciones y comparación entre pensamientos separados en el tiempo;
- recomendaciones locales explicables, feedback y cache remoto prescindible con stale-while-revalidate;
- estadísticas anuales, Wrapped histórico y tarjetas PNG 1080 × 1920 compartibles;
- backup JSON versionado, validado y restaurado en una transacción Room;
- IA opcional en el dispositivo para pulir, resumir, extraer temas, preguntar, comparar y conectar reflexiones de obras distintas. Los insights conservan todas sus fuentes y nunca sustituyen el original.

## Stack

Kotlin, Jetpack Compose, Material 3, Navigation 3, Room, KSP, Hilt, Coroutines/Flow, DataStore, Retrofit, OkHttp, Kotlin Serialization, Coil y ML Kit GenAI. Las versiones están centralizadas en `gradle/libs.versions.toml`.

El proyecto usa un único módulo `app`; consulta [Arquitectura](docs/ARCHITECTURE.md), [Modelo de datos](docs/DATA_MODEL.md), [Producto](docs/PRODUCT.md) y [Decisiones](docs/DECISIONS.md).

## Requisitos

- JDK 17;
- Android SDK Platform 37;
- Android SDK Build Tools compatible con AGP 9.3;
- Android 8.0/API 26 o posterior para instalar la app.

`compileSdk` es 37 y `targetSdk` es 36. Abre el proyecto en una versión compatible de Android Studio y ejecuta la configuración `app`.

## API keys

El núcleo local no necesita claves. Copia `local.properties.example` a `local.properties` y configura solo los proveedores deseados:

```properties
TMDB_API_KEY=
RAWG_API_KEY=
OPEN_LIBRARY_CONTACT=contacto@example.com
```

- **TMDB:** películas y series. Sin clave, estos tipos siguen disponibles mediante alta manual.
- **RAWG:** videojuegos. Sin clave, el alta manual sigue funcionando.
- **Open Library:** libros; no requiere clave. El contacto se incorpora al `User-Agent` recomendado.

No subas `local.properties`, tokens ni keystores. Una clave incorporada a un APK personal puede extraerse; una distribución pública debe replantear la autenticación.

## Offline y privacidad

Room es la fuente de verdad. Biblioteca, consumos, progreso, reflexiones, Remember, estadísticas y backups funcionan sin Internet. Las APIs externas solo buscan candidatos y completan la ficha seleccionada; si falla el detalle se conserva el resultado parcial con un aviso antes de copiarlo a Room.

No hay login, backend, analytics, anuncios ni telemetría propia. Memento no hace backups remotos. Las reflexiones no se registran en logs y ninguna automatización las sobrescribe.

## IA local

La integración usa la API Prompt de ML Kit GenAI y AICore/Gemini Nano. Es opcional, comprueba compatibilidad en tiempo de ejecución y solo aparece en Remember cuando el modelo está disponible. Algunos dispositivos compatibles requieren descargar primero el modelo desde Ajustes.

La dependencia de ML Kit es beta porque no existe una API estable equivalente para esta capacidad experimental. La app sigue plenamente operativa cuando el dispositivo no es compatible. El resultado se puede copiar, descartar o guardar como `AiInsight`; el texto original permanece inmutable.

Documentación oficial: [ML Kit GenAI Prompt API](https://developers.google.com/ml-kit/genai/prompt/android/get-started).

## Backup

Desde Ajustes se exporta un JSON UTF-8 con `schemaVersion = 2`. Incluye obras, referencias externas, creadores, géneros, consumos, progreso, reflexiones, exposiciones de Remember, feedback, insights de IA y sus múltiples fuentes; excluye la cache descargable de recomendaciones. Los backups v1 siguen siendo importables.

La importación limita el archivo a 10 MB, valida estructura, claves foráneas, enums y fechas, muestra un resumen y solo entonces reemplaza los datos en una única transacción. Si falla, Room revierte la operación.

## Compilar APK

```bash
./gradlew assembleDebug
```

En Windows:

```powershell
.\gradlew.bat assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. `assembleRelease` también compila; el firmado debe configurarse externamente y no se incluye ningún keystore.

## Tests

```bash
./gradlew testDebugUnitTest
./gradlew compileDebugAndroidTestKotlin
./gradlew lintDebug
./gradlew assembleDebug
```

Las pruebas unitarias cubren Remember ponderado/determinista, cache de recomendaciones, conexiones entre reflexiones, progreso, estadísticas y Wrapped; timeline, ratings, backup, mappers y estados principales de ViewModels, incluida una IA falsa. Las pruebas instrumentadas validan la migración `3→4`, fuentes N:M y cascadas, consumo activo único, exposiciones diarias, round trip Room, la tarjeta compartible 1080 × 1920 y flujos Compose críticos. Para ejecutarlas en un dispositivo conectado:

```bash
./gradlew connectedDebugAndroidTest
```

## Atribuciones

Memento no está afiliada oficialmente a TMDB, Open Library ni RAWG. Los textos, enlaces y estados de cada fuente están disponibles en Ajustes. Al distribuir una build se deben revisar de nuevo los términos vigentes de cada proveedor.
