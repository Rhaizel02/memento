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

Room v4 es la fuente de verdad de los datos personales. Se usan UUID internos, foreign keys con cascada, índices, schema exportado y migraciones explícitas `1→2→3→4`; no existe `fallbackToDestructiveMigration`.

Las fechas elegidas por el usuario son `LocalDate`; los timestamps técnicos son `Instant`. La timeline se deriva de consumos, progreso y reflexiones para evitar duplicidad. DataStore solo conserva tema y onboarding.

Las consultas de Biblioteca aplican texto, tipo, estado, rating, favorito, año y orden en SQLite. Home usa proyecciones agregadas. Cuando Stats, Wrapped, recomendaciones o conexiones necesitan toda la historia, `observeAllDetails` ejecuta seis consultas bulk constantes —obras, creadores, géneros, consumos, progreso y reflexiones— y monta relaciones mediante mapas lineales. Se eliminó el patrón de cinco consultas adicionales por obra.

## Fuentes remotas

Retrofit/Kotlin Serialization encapsula TMDB, Open Library y RAWG detrás de `MetadataRepository`. Los DTOs no llegan a UI. El flujo es `buscar → seleccionar → pedir detalle → mapear → revisar → Room`: TMDB obtiene película/serie con créditos anexados, Open Library obtiene Work y una Edition para enriquecer páginas/portada, y RAWG obtiene `/games/{id}`. Si el detalle falla, la UI mantiene el resultado parcial, lo indica discretamente y permite revisarlo. Las claves ausentes degradan a alta manual.

Las recomendaciones combinan un perfil local puro con candidatos de proveedores. `recommendation_candidates` es cache borrable; no forma parte del backup. Una cache de menos de 24 horas evita red; después se revalida y una respuesta fallida conserva los datos stale. Tras una actualización válida se retienen siete días. El feedback sí es dato personal y se conserva.

## Remember, compartir e IA

Remember separa elegibilidad, scoring y sorteo ponderado. La semilla derivada del día y del conjunto de candidatos mantiene una elección estable durante la jornada sin convertir el máximo en ganador automático. Home registra la exposición al mostrar el héroe y Room inserta como máximo una exposición lógica por consumo y día. Sus tarjetas compartibles se renderizan localmente con Canvas a PNG 1080 × 1920 y se entregan al Sharesheet mediante `FileProvider`.

`AiProcessor` aísla ML Kit GenAI. La implementación comprueba estado, descarga opcional y generación on-device; tests y ViewModels dependen de la interfaz. `AiInsight` se relaciona N:M con sus reflexiones fuente. Evolución usa reflexiones de una misma obra; Conexión elige localmente otra obra mediante solapamiento de palabras significativas. Los resultados solo se persisten cuando la persona lo decide y nunca actualizan una reflexión.

## Backup

El SAF elige destino/origen sin permisos generales de almacenamiento. `BackupCodec` valida tamaño, versión, estructura, referencias, tipos y fechas antes de modificar Room. El reemplazo completo ocurre dentro de `withTransaction`; la cache remota queda fuera deliberadamente.

## Navegación e inyección

Navigation 3 mantiene una pila de claves serializables con cuatro destinos principales. Hilt crea base de datos, DAOs, APIs, repositorios, procesador IA y ViewModels; KSP procesa Hilt y Room.
