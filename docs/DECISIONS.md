# Decisiones

## ADR-001 — Un único módulo Android

**Contexto.** App personal local-first sin equipos ni despliegues separados.  
**Decisión.** Un módulo `app` organizado por responsabilidad.  
**Motivo.** Reduce configuración y compilación sin mezclar capas.  
**Consecuencias.** Solo se modularizará ante límites reales.

## ADR-002 — Timeline derivada

**Contexto.** Inicio, final, progreso y reflexiones ya son hechos persistidos.  
**Decisión.** Derivar la timeline, sin tabla de eventos duplicada.  
**Motivo.** Evita divergencias. `RememberExposure` sí persiste porque no puede derivarse.  
**Consecuencias.** La presentación se recompone de datos fuente íntegros.

## ADR-003 — compileSdk 37 y targetSdk 36

**Contexto.** El stack AndroidX seleccionado requiere compilar con API 37, mientras el target estable apropiado es 36.  
**Decisión.** `compileSdk 37`, `targetSdk 36`, `minSdk 26`.  
**Motivo.** Compatibilidad mutua del stack sin adoptar prematuramente comportamientos de target futuros.  
**Consecuencias.** Revisar el target antes de publicar.

## ADR-004 — Motores de dominio puros

**Contexto.** Remember, recomendaciones, Stats y Wrapped necesitan reglas comprensibles y pruebas deterministas.  
**Decisión.** Mantener sus cálculos libres de Android/Room y suministrar fechas/variación como entradas.  
**Motivo.** Explicabilidad, tests rápidos y evolución segura.  
**Consecuencias.** Los repositorios preparan datos; los motores no hacen I/O.

## ADR-005 — Backup JSON completo pero sin cache remota

**Contexto.** Los datos deben sobrevivir años y ser portables.  
**Decisión.** JSON versionado con todas las tablas personales, validación previa y restore transaccional; excluir `recommendation_candidates`.  
**Motivo.** La cache se puede reconstruir y no debe inflar ni contaminar el archivo personal.  
**Consecuencias.** Cambios incompatibles exigirán evolución explícita de `schemaVersion`.

## ADR-006 — Metadatos externos opcionales y copiados a Room

**Contexto.** TMDB y RAWG necesitan clave y cualquier proveedor puede fallar.  
**Decisión.** Buscar con Retrofit, revisar y copiar metadata/referencias a Room; alta manual siempre disponible.  
**Motivo.** Mantener el producto local-first.  
**Consecuencias.** Secrets fuera de Git; distribución pública requerirá reevaluar claves incorporadas al APK.

## ADR-007 — ML Kit GenAI beta detrás de una interfaz opcional

**Contexto.** La generación on-device solicitada no dispone de una API estable equivalente y la compatibilidad de dispositivo es limitada.  
**Decisión.** Usar la API Prompt beta oficial detrás de `AiProcessor`, detectar capacidad y no mostrar acciones cuando falta soporte.  
**Motivo.** Permite la función experimental sin acoplar ni bloquear el núcleo.  
**Consecuencias.** La dependencia debe revisarse al estabilizarse; tests usan fake y la release funciona sin IA.

## ADR-008 — Detalle remoto antes de persistir

**Contexto.** Los endpoints de búsqueda ofrecen fichas ligeras y omiten duración, estructura, créditos o descripción completa.
**Decisión.** Después de seleccionar se consulta el detalle del proveedor y sólo después se habilita la persistencia. TMDB anexa créditos; Open Library combina Work con la primera Edition relevante; RAWG consulta el juego por ID.
**Motivo.** Evita convertir un resultado de búsqueda provisional en la fuente persistida.
**Consecuencias.** Si la llamada falla, el resultado parcial sigue editable y aparece un aviso discreto.

## ADR-009 — Relaciones globales en consultas bulk

**Contexto.** Montar Stats, Wrapped y recomendaciones hacía cinco consultas por cada obra.
**Decisión.** Observar seis conjuntos completos y agruparlos en memoria por sus claves. `RecommendationEngine` permanece puro.
**Motivo.** El número de consultas deja de crecer con la biblioteca y el trabajo de ensamblado es lineal.
**Consecuencias.** Las vistas globales cargan las columnas necesarias de la historia; Biblioteca, Home y Detalle conservan consultas específicas.

## ADR-010 — Fuentes N:M para insights

**Contexto.** Comparar o conectar pensamientos requiere más de una reflexión de origen.
**Decisión.** Room v4 separa `ai_insights` de `ai_insight_sources`; `3→4` copia el antiguo `reflectionId`. La última fuente eliminada limpia el insight huérfano.
**Motivo.** La procedencia queda exacta sin duplicar insights ni alterar textos originales.
**Consecuencias.** El backup sube a v2 y conserva compatibilidad de importación con v1.

## ADR-011 — Consumo activo protegido por triggers

**Contexto.** SQLite admite un índice único parcial, pero Room 2.8 no permite declararlo en `@Index`; un índice manual quedaría fuera del contrato de schema generado y de su validación.
**Decisión.** Reutilizar la fila activa en una transacción, rechazar backups inválidos y añadir triggers `BEFORE INSERT/UPDATE` para `PLANNED`/`IN_PROGRESS`.
**Motivo.** Conserva una garantía SQLite robusta y explícita sin introducir una discrepancia silenciosa con Room.
**Consecuencias.** Los triggers se crean en `3→4` y en el callback de creación/apertura.

## ADR-012 — Cache de recomendaciones stale-while-revalidate

**Contexto.** Refrescar al abrir Home/Descubrir desperdiciaba red y una caída podía dejar una pantalla vacía.
**Decisión.** TTL fresco de 24 horas, revalidación tras caducar y retención de siete días después de una actualización válida. Un fallo no borra cache stale.
**Motivo.** Reduce llamadas y mantiene utilidad offline.
**Consecuencias.** Sin cache y sin red, el feed queda vacío sin afectar biblioteca ni feedback.

## ADR-013 — Open Library se identifica por Work

**Contexto.** Una obra puede tener muchas ediciones y `ExternalMediaRef` guarda una referencia por proveedor y obra local.
**Decisión.** Persistir el ID de Work; usar Edition únicamente para enriquecer páginas y portada durante el alta.
**Motivo.** Work es la identidad cultural estable que Memento modela; una edición concreta no es hoy un concepto de dominio.
**Consecuencias.** Se mantiene la clave primaria `(mediaItemId, provider)`. Si el producto modela ediciones en el futuro, deberá evolucionar explícitamente el dominio y esa clave.
