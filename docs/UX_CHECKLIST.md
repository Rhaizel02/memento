# Checklist UX/UI

Lista manual breve para regresiones de las rutas críticas. Probar al menos una vez con tema claro y oscuro, tamaño de fuente grande y teclado visible.

## Añadir obra

- Abrir Añadir, escribir búsqueda y pulsar **Borrar búsqueda**: queda vacía, desaparecen resultados/loading y el campo conserva el foco.
- Seleccionar rápidamente dos resultados: el detalle tardío del primero no reemplaza el segundo.
- Pasar de búsqueda a alta manual y volver: se mantiene el contexto de esa operación.
- Cancelar con Volver y abrir Añadir otra vez: tipo Libro, modo Búsqueda y campos vacíos.
- Completar un alta y abrir otra: no reaparecen título, autoría, descripción, portada ni estado de guardado.
- Rotar durante búsqueda, formulario manual y confirmación: se conserva solo la operación actual.
- Pulsar Guardar dos veces rápidamente: se crea una única obra/consumo y los botones quedan bloqueados mientras guarda.
- Con teclado abierto, desplazarse hasta todas las acciones de guardado sin que queden tapadas.

## Creadores y metadata

- Película TMDB: solo aparecen nombres con trabajo `Director`; nunca reparto.
- Serie TMDB: solo aparecen nombres de `createdBy`; nunca reparto.
- Libro Open Library: aparece autoría. Juego RAWG: aparece desarrollo.
- Confirmar que las etiquetas del formulario son Autoría, Dirección, Creación o Desarrollo según el tipo.
- Simular detalle remoto fallido: se muestra el aviso de ficha parcial y los datos básicos siguen editables/guardables.

## Biblioteca y Descubrir

- Biblioteca: **Borrar búsqueda** limpia el texto y restaura la lista sin cerrar el teclado.
- Combinar texto, tipo y filtros sin coincidencias: aparece “sin resultados” y **Limpiar** restablece todos los criterios.
- Biblioteca realmente vacía: aparece CTA **Añadir primera obra**.
- Descubrir no contiene un campo de búsqueda editable; por ello no requiere acción X.
- Actualizar recomendaciones varias veces rápido: solo se ejecuta un refresh; los títulos largos ofrecen **Ver más**.

## Detalle, Remember y operaciones

- Sinopsis corta: no aparece control adicional. Sinopsis larga: **Ver más/Ver menos** muestra todo el contenido.
- Notas y reflexiones se ven completas; editar no modifica el texto anterior hasta confirmar Guardar.
- Valores de progreso inválidos producen un mensaje comprensible en pantalla y no cierran la app.
- Acciones de detalle quedan bloqueadas mientras se escribe; Eliminar obra vuelve atrás solo después de completar el borrado.
- Remember permite escribir y alcanzar Guardar con el teclado visible; las acciones de IA y guardado de insight no admiten doble envío.
- Backup: exportar/importar bloquea nuevos toques, una restauración fallida conserva los datos locales y un restore válido exige confirmación explícita.

## Accesibilidad y layout

- Botones de icono Volver, Favorito, Editar, Eliminar y Borrar búsqueda anuncian su finalidad con TalkBack.
- Objetivos táctiles de iconos mantienen al menos 48 dp; comprobar que no se solapan en ancho compacto.
- Revisar escalas de fuente 1,0× y máxima: títulos, CTAs y diálogos no se recortan ni salen de pantalla.
- Recorrer Home, Biblioteca, Descubrir, Detalle, Remember, Stats, Wrapped y Ajustes buscando estados vacíos accionables, jerarquía clara y contraste suficiente.
