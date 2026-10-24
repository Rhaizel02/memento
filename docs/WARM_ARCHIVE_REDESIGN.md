# Warm Archive — implementación y validación

## Alcance

El contrato aprobado se conserva íntegro en `DESIGN.md`. No se reconstruye la aplicación: se mantiene Compose, Material 3, Navigation 3, Hilt, StateFlow y la persistencia existente.

### Composición renovada

- **Inicio:** cabecera editorial, recuerdo protagonista con contenido real, obras en curso en filas con progreso y acciones directas, efemérides, recomendación concreta, actividad y entradas secundarias.
- **Biblioteca:** búsqueda local, filtros por medio/estado visibles, filtros avanzados conservados, lista compacta con creador/estado/rating/tags y alternativa de cuadrícula.
- **Detalle:** portada y metadatos, historia personal antes de la sinopsis, progreso, acciones, tags, diario, citas retrospectivas, disponibilidad y consumos históricos.
- **Discover y Recommendation Detail:** selección editorial, tiers y razones reales compartidas con Inicio, incorporación a Biblioteca y enlaces existentes.
- **Quick Capture:** hojas coherentes con teclado y barras del sistema, incrementos adecuados al medio, borradores y guardado sin doble envío.

### Adaptación de pantallas secundarias

Historia/Timeline, Calendario, Remember, Perfil cultural, Estadísticas, Wrapped, Búsqueda global, Añadir obra, Ajustes y Onboarding adoptan los mismos tokens, fuentes y controles. No se han reescrito sus motores ni su cobertura funcional. Perfil y Estadísticas usan secciones y separadores en lugar de apilar paneles grandes. Wrapped mantiene su experiencia paginada, años, exportación y consentimiento para texto privado.

Estas pantallas son adaptaciones coherentes, no réplicas completas de cada composición de Stitch. No se incorporan funciones ficticias que aparezcan en las referencias.

## Sistema visual

- Warm Light, Warm Dark y Sistema; valores exactos del contrato para fondos, texto, terracota, bordes, error y éxito.
- Newsreader para títulos y pasajes personales; Work Sans para controles y metadatos. Recursos incluidos en la APK, sin descarga de fuentes en ejecución.
- Fuentes procedentes de los repositorios oficiales [Newsreader](https://github.com/google/fonts/tree/main/ofl/newsreader) y [Work Sans](https://github.com/google/fonts/tree/main/ofl/worksans). Licencias OFL incluidas en `app/src/main/assets/licenses`.
- Cabeceras, botones de 48 dp mínimos, chips, campos con etiqueta fija, hojas, skeleton estático, artwork/fallbacks, citas y tarjetas de recomendación compartidos.
- Los enums históricos MEMENTO/FOREST/NOBLE/INK y su lectura DataStore siguen siendo compatibles. Todos resuelven a Warm Archive; Sistema/Claro/Oscuro se conserva independientemente. No hay migración destructiva.

## Navegación y datos

La barra inferior contiene Inicio, Biblioteca, Descubrir e Historia. Ajustes está visible en la cabecera de los cuatro destinos. Se conservan entradas a búsqueda global, calendario, estadísticas, perfil, Wrapped y añadir obra, además del retorno desde detalles.

No se modifican entidades, versión/esquemas Room, backup, proveedores ni motores estadísticos/recomendadores. Dos adaptaciones de lectura dan soporte a la presentación:

1. Resumen de Biblioteca: combina flujos existentes y agrupa creadores, consumos y tags una vez; no hace consultas por fila ni carga reflexiones.
2. Detalle: conecta las referencias externas ya persistidas para no ocultar disponibilidad de streaming de obras guardadas.

Los borradores de captura son transitorios; no añaden persistencia. En Detalle se conservan al cerrar la hoja y se limpian tras guardado confirmado o descarte explícito; en Inicio se conservan mientras vive su ViewModel. No se promete recuperación tras muerte del proceso para borradores de Inicio.

## Validación automatizada

- `test`: **141 tests, 0 fallos, 0 errores, 0 omitidos**.
- `assembleDebug`: **correcto**.
- `assembleDebugAndroidTest`: **correcto**.
- Regresiones selectivas: compatibilidad de paletas antiguas, contraste y superficies, borrador de nota cerrar/reabrir/descartar, selecciones de apariencia y formularios.
- Pruebas instrumentadas preparadas: navegación/captura con la actividad real y capturas light/dark; verificación Room de resúmenes y referencias externas. Compilar estas pruebas no equivale a ejecutarlas.

### Bloqueo de emulador

No hay capturas reales de Memento de esta sesión ni se declara pasada la validación visual. El emulador instalado 37.1.11 se cierra en código nativo (`0xC0000005`) antes de poder instalar/ejecutar la app. Se probó también 36.3.10 Stable, descargado del archivo oficial de Android y verificado mediante SHA-256 en un directorio temporal, con renderizado software y con/sin aceleración: tampoco mantuvo un dispositivo operativo. Los intentos utilizaron un AVD temporal separado; no se borró el AVD personal existente.

Quedan pendientes de un dispositivo/emulador funcional: ejecutar `WarmArchiveJourneyTest`, las regresiones instrumentadas y revisar capturas, texto ampliado, teclado y pantallas estrechas. La comprobación de estos puntos por lectura de código no sustituye su validación en ejecución.

### Severidad al cierre

- **P0:** ninguno identificado por la compilación y los tests ejecutados.
- **P1:** pendiente la validación Android/visual por el bloqueo del entorno; no se afirma ausencia de errores de interacción sin esa ejecución.
- **P2:** ajustes finos de composición y densidad que pueda revelar la revisión visual; las pantallas secundarias mantienen parte de su estructura anterior adaptada al sistema común. No bloquean la generación de la APK.

## Desviaciones deliberadas respecto a Stitch

- Home Dark se construye con los tokens espresso y las buenas referencias oscuras, no con la captura marfil mal etiquetada.
- Se preservan todos los filtros, estados, consumos históricos, citas retrospectivas y streaming aunque no aparezcan en un mockup.
- Sin porcentajes inventados, emociones deducidas, nuevas APIs, efectos caros ni funcionalidades sociales.
- Portadas reales cuando existen; placeholders neutros cuando faltan. No se incluyen imágenes de las referencias como datos de producción.
