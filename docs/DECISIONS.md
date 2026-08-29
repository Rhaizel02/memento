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
