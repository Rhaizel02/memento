# Modelo de datos

```text
MediaItem 1 ── N Consumption 1 ── N ProgressEntry
    │                │
    │                └── N Reflection 1 ── N AiInsight
    ├── N:M Creator (rol)
    ├── N:M Genre
    └── 1:N ExternalMediaRef

Consumption 1 ── N RememberExposure
External candidate ── RecommendationFeedback
```

## Núcleo personal

- **MediaItem:** obra estable, independiente del proveedor. Guarda tipo, títulos, descripción, fechas, imágenes, favorito y metadata estructural mínima.
- **Consumption:** una lectura, visionado o partida concreta, con estado, fechas y rating propios. Un reconsumo crea otra fila.
- **ProgressEntry:** historial de páginas, episodio, horas, porcentaje o minutos.
- **Reflection:** texto UTF-8 de tipo nota, reflexión final o reflexión posterior. Ninguna función inteligente sustituye contenido existente.
- **AiInsight:** resultado on-device aceptado expresamente, ligado a la reflexión que lo originó y eliminado en cascada con ella.

## Relaciones descriptivas

`Creator` y `Genre` se normalizan y se relacionan N:M. `ExternalMediaRef` mantiene el vínculo con TMDB, Open Library o RAWG y su restricción única evita duplicados por proveedor, id externo y tipo.

## Sistemas derivados

- **RememberExposure:** hecho histórico no derivable que evita mostrar repetidamente un consumo/reflexión.
- **RecommendationFeedback:** señal personal persistente (interesa, no interesa, ya conocida).
- **RecommendationCandidate:** cache remota temporal y prescindible; puede borrarse sin afectar la biblioteca y no se exporta.

Timeline, estadísticas, Wrapped, perfiles de gusto y puntuaciones de recomendación se calculan; no duplican hechos en tablas genéricas.

## Integridad y evolución

- Borrar una obra elimina en cascada sus consumos y dependencias, tras confirmación en UI.
- Borrar un consumo no elimina la obra ni otros consumos.
- IDs internos son UUID String, nunca IDs del proveedor.
- Las fechas personales son `LocalDate`; timestamps técnicos, `Instant` UTC.
- El schema actual es versión 3 y se exporta a `app/schemas`.
- `1→2` añade recomendación/cache/feedback; `2→3` añade insights de IA.
- El backup JSON v1 contiene todas las tablas personales y valida claves foráneas antes de restaurar.
