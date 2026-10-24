# Biblioteca + branding — polish UI/UX

## Alcance

Iteración sobre Warm Archive, sin cambios en Room, entidades, backup/import, motores, proveedores ni dependencias de la app. Se aplican los problemas descritos en el bloque del usuario; no había nuevas capturas adjuntas en este mensaje para comparar visualmente.

## Biblioteca

- Cabecera compacta con subtítulo breve; añadir obra pasa del FAB a un botón tonal en la cabecera. Acceso a búsqueda global y Ajustes conservado.
- Buscador local con placeholder y nombre accesible, sin etiqueta superior duplicada. Borrar conserva el foco. No hace búsquedas externas.
- Tipo de medio en chips principales; estado en pestañas de texto con subrayado y semántica de selección. Todas las opciones siguen disponibles por desplazamiento horizontal.
- Toolbar de colección: filtros avanzados con contador, menú directo de ordenación y cambio lista/cuadrícula. El contador excluye el estado visible y la ordenación, que no son filtros avanzados ocultos.
- Toolbar dividida en dos filas cuando falta espacio o se amplía la fuente; evita comprimir el selector de ordenación.
- Controles y resultados comparten una única colección lazy con estado guardable. Solo la cabecera permanece fija. Márgenes inferiores sin la antigua reserva de FAB y adaptación al teclado.
- Lista y cuadrícula comparten metadatos: título, creador, tipo, año, estado, rating, favorito y tags disponibles. Sin consultas nuevas ni cálculos estadísticos.
- Portadas decorativas sin duplicar el nombre anunciado. Fallback existente cuando falta imagen. Títulos de dos líneas, creadores/tags con elipsis y metadatos flexibles.
- Con fuente ampliada se ensancha la celda de cuadrícula, pudiendo pasar a una columna para conservar legibilidad. La selección de vista se mantiene.
- Loading, biblioteca vacía, resultados sin coincidencias y limpiar filtros conservan sus acciones. Una ordenación diferente por sí sola no se confunde con una búsqueda sin resultados.

## Branding

Se usan vectores del kit aprobado, con geometría fuente en `design/branding/android-logo-kit/source/memento-symbol.svg`:

- Launcher adaptativo: M marfil sobre terracota; variantes `ic_launcher` / `ic_launcher_round` conservando las referencias del manifiesto.
- API 33+: máscara monocroma independiente para iconos temáticos. API 26–32 mantiene capas foreground/background.
- Android 12+: splash estático sin fondo de icono, con el símbolo y márgenes del kit. Base de tema compartida conserva atributos claros/oscuros. Sin nueva Activity, dependencia, animación ni espera artificial.
- Fondo de ventana Warm Archive también en versiones anteriores; sin pantalla de splash adicional.
- Onboarding usa la M en lugar de las antiguas tarjetas abstractas. Inicio y el apartado Acerca de muestran el símbolo con discreción; no invade las pantallas de contenido ni sustituye los iconos de tipos de medio.
- En Compose el tint usa `MaterialTheme`, de modo que el logo respeta Claro/Oscuro elegido en la app aunque no coincida con Android. Se mantiene el nombre como texto Newsreader accesible.
- No se incluyen el ZIP, PNG de Play Store, fuentes duplicadas, previews ni herramientas de exportación en la APK.

## Pantallas relacionadas

Descubrir e Historia usan cabecera compacta, un único título/subtítulo y conservan filtros, calendario, recomendaciones y navegación. Ajustes elimina el doble margen horizontal y reduce separación entre secciones; todas sus opciones permanecen. No se han rediseñado de nuevo sus motores ni sus contenidos.

## Validación y límites

- Tests selectivos: `ViewModelsTest`, `ColorPalettesTest`, `ThemePreferencesTest`, `OnboardingPreferencesTest`, `LibraryUiPolicyTest`: **45 tests, sin fallos**.
- `assembleDebug` y `assembleDebugAndroidTest`: **correctos** tras el último ajuste responsive de toolbar.
- Se añaden dos escenarios de interacción Android: controles/lista/cuadrícula/navegación y limpieza de un empty state a 320 dp con fuente 1,5×. Se compilan; no se declara su ejecución.
- Revisión razonada de composables: targets mínimos de 48 dp, filtros horizontales, metadatos flexibles, lista/cuadrícula, teclado, reserva inferior, estados vacíos, controles con fuente ampliada y colores de tema.
- Comprobación de recursos empaquetados: launcher v26/v33, tema v31 y base día/noche presentes en la APK.
- No había dispositivo ADB conectado. Sin revisión visual en ejecución ni nuevas capturas de emulador en este bloque. La inspección de código y la compilación no sustituyen esa prueba.

## Trade-offs / backlog

- Buscar/filtrar requiere volver hacia arriba al desplazarse lejos; a cambio, los controles no ocupan permanentemente gran parte de la pantalla. La cabecera y añadir siguen accesibles.
- Títulos largos se abrevian en la colección; el detalle conserva el título completo.
- El splash nativo sigue Claro/Oscuro del **sistema** antes de que Compose lea las preferencias. La apariencia forzada de la app se aplica al contenido. Sin añadir almacenamiento ni un bloqueo de arranque para resolver esa transición.
- Pendiente comprobar en dispositivo el launcher, iconos temáticos, arranque frío, IME, TalkBack y ajuste fino de densidad a partir de capturas reales del usuario.
