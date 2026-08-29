# Arquitectura

## Forma del proyecto

Memento usa un único módulo Android `app`. La escala esperada —una biblioteca personal de hasta unas 10.000 obras— no justifica una arquitectura multimódulo ceremonial.

```text
Compose UI
  ↓ estados inmutables y callbacks
ViewModels
  ↓
Motores puros / Repositories
  ↓                         ↓
Room + DataStore       Proveedores HTTP opcionales
```

Los ViewModels exponen `StateFlow`. Los repositorios coordinan transacciones y transforman entidades/DTOs en modelos de dominio. Remember, Recommendation, Stats, Wrapped y Timeline son cálculos de dominio puros y testeables.

## Persistencia local

Room v3 es la fuente de verdad de los datos personales. Se usan UUID internos, foreign keys con cascada, índices, schema exportado y migraciones explícitas `1→2→3`; no existe `fallbackToDestructiveMigration`.

Las fechas elegidas por el usuario son `LocalDate`; los timestamps técnicos son `Instant`. La timeline se deriva de consumos, progreso y reflexiones para evitar duplicidad. DataStore solo conserva tema y onboarding.

Las consultas de Biblioteca aplican texto, tipo, estado, rating, favorito, año y orden en SQLite. Las pantallas usan colecciones lazy y no cargan relaciones de toda la base salvo los agregados que necesitan Stats, Wrapped y recomendaciones.

## Fuentes remotas

Retrofit/Kotlin Serialization encapsula TMDB, Open Library y RAWG detrás de `MetadataRepository`. Los DTOs no llegan a UI. Al confirmar un alta se copian metadata, referencias, creadores y géneros a Room. Los fallos de red o claves ausentes degradan a alta manual.

Las recomendaciones combinan un perfil local con candidatos de proveedores. `recommendation_candidates` es cache borrable; no forma parte del backup. El feedback sí es dato personal y se conserva.

## Remember, compartir e IA

Remember selecciona mediante scoring comprensible y variación estable, y registra exposiciones para no repetir. Sus tarjetas compartibles se renderizan localmente con Canvas a PNG 1080 × 1920 y se entregan al Sharesheet mediante `FileProvider`.

`AiProcessor` aísla ML Kit GenAI. La implementación comprueba estado, descarga opcional y generación on-device; tests y ViewModels dependen de la interfaz. Los resultados solo se persisten como `AiInsight` cuando la persona lo decide y nunca actualizan una reflexión.

## Backup

El SAF elige destino/origen sin permisos generales de almacenamiento. `BackupCodec` valida tamaño, versión, estructura, referencias, tipos y fechas antes de modificar Room. El reemplazo completo ocurre dentro de `withTransaction`; la cache remota queda fuera deliberadamente.

## Navegación e inyección

Navigation 3 mantiene una pila de claves serializables con cuatro destinos principales. Hilt crea base de datos, DAOs, APIs, repositorios, procesador IA y ViewModels; KSP procesa Hilt y Room.
