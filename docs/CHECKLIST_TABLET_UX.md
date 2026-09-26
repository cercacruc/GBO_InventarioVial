# Checklist de aceptación en tablet

Completar con la ingeniera responsable usando registros de prueba. No borrar datos históricos para probar la migración. Este checklist no presupone que se hayan ensayado cámara, GNSS o Drive reales.

## Instalación e históricos

- [ ] Abrir registros antiguos tras actualizar desde Room 6: misma cantidad, UUID, fotografías y nombres de Drive.
- [ ] Reabrir un SCAP antiguo con junta: tipo/material aparecen en Junta 1.
- [ ] Girar la tablet y verificar legibilidad, controles táctiles y desplazamiento.
- [ ] Confirmar tema azul/blanco y logo; cada estado tiene texto/símbolo, no solo color.

Checklist ampliado de 26 pasos: [Revisión de ingeniería de puentes 2](REVISION_INGENIERIA_PUENTES_2.md).

## SCAP

- [ ] A: elegir ruta de la lista compartida con SIC; guardar, salir y reabrir.
- [ ] Cambiar catálogo del tramo y comprobar nuevas opciones en SIC y SCAP; conservar selección histórica.
- [ ] C.5: comprobar A Barandas, B Veredas y Sardineles, C Apoyos, D Juntas, E Drenaje.
- [ ] Abrir tipo de baranda: opciones originales + No aplica + Sin completar.
- [ ] Seleccionar tipo real, completar material; pasar a No aplica y cancelar confirmación: datos intactos.
- [ ] Confirmar No aplica: dependientes deshabilitados; volver a tipo real: recuperar material anterior.
- [ ] Repetir para material de veredas, apoyos, juntas y drenaje.
- [ ] Añadir, editar y eliminar con confirmación apoyos y juntas; reabrir y comprobar persistencia.
- [ ] Con más de dos apoyos o una junta: verificar todos los valores en las continuaciones XLSX, sin bloqueo por esos cupos originales.
- [ ] E: completar las once medidas de croquis con decimales y unidades m; cerrar y reabrir.
- [ ] Divisorio: ingresar ancho y altura, elegir No aplica, confirmar; ambos quedan deshabilitados sin borrarse.
- [ ] Volver a Sin completar en el estado del grupo y comprobar que se recuperan las medidas.
- [ ] F.2: título Condición Global del Puente y cinco grupos I–V con código/elemento/descripción.
- [ ] Panel fotográfico: cámara y selector, miniatura, fecha, número, descripción, categoría y elemento.
- [ ] Mover foto arriba/abajo, reabrir; eliminar foto asociada a defecto y comprobar defecto sin enlace roto.
- [ ] Exportar SCAP: hojas físicas originales, estilo, logos, geometría y fórmulas; panel y descripciones correctos.
- [ ] No aplica no exporta códigos inventados ni datos dependientes inactivos. Medidas auxiliares no aparecen.

## SIC-18 y SIC-18A

- [ ] Criterios del 30%/20% bajo Condición Estructural; obstrucción bajo Funcional, sin inputs porcentuales.
- [ ] Panorámica, entrada, salida: cámara, selección de imagen, contador, miniatura y eliminación/reemplazo.
- [ ] Guardar y abrir historial: cada foto conserva su categoría explícita.
- [ ] Fotos antiguas sin categoría siguen visibles; clasificar manualmente, sin deducción por nombre.
- [ ] Seleccionar condición mala estructural o funcional, guardar SIC-18: aparece Completar SIC-18A.
- [ ] Volver sin completar: no se crea una ficha SIC-18A automáticamente.
- [ ] Abrir SIC-18A: cabecera/clase/tipo/ojos heredados; completar sus cinco catálogos de falla/función.
- [ ] Intentar guardar con campos vacíos/códigos inválidos: validación clara.
- [ ] Guardar, revisar resumen en historial, reabrir, editar y guardar.
- [ ] Editar clase/tipo/ojos en SIC-18: se reflejan al abrir SIC-18A vinculada.
- [ ] Exportar SIC-18A a XLSX: 13 columnas solicitadas, cabecera y fecha del padre, sin columnas internas.

## SIC-19

- [ ] Tierra: Regular = erosión; Malo = erosión grave.
- [ ] Concreto y Mampostería: criterios de longitud menor/mayor al 30%.
- [ ] Otro: no se asume criterio; seleccionar Pavimentado o Tierra.
- [ ] Guardar/reabrir/editar y comprobar criterio y condición.
- [ ] Excel conserva únicamente código final 1/2/3 y no añade columna del criterio auxiliar.

## SIC-20

- [ ] Badén: estructural y funcional presentes, guardadas, editables y exportadas.
- [ ] Túnel: estructural y funcional presentes, guardadas, editables y exportadas.
- [ ] Muro: estructural y funcional presentes, guardadas, editables y exportadas.
- [ ] Muro: dimensión 1 es altura promedio del cuerpo; dimensión 2 vacía en Excel.
- [ ] Longitud del muro decimal: persistencia y resumen interno; no aparece en SIC ni afecta condición.
- [ ] Histórico con funcional nula: pendiente visible; editar permite completarla sin inventar un valor.

## Campo / sincronización

- [ ] Probar cámara real, selector de galería y permisos con la tablet del proyecto.
- [ ] Probar captura sin conexión, salida/reapertura y posterior sincronización de fotos SIC.
- [ ] Comprobar en Drive ruta/SIB/nombre y ausencia de duplicados después de reintentar.
- [ ] Recordar que XLSX es exportación local, no envío automático a Apps Script.
- [ ] Confirmar que SCAP sigue local y no entra en la cola fotográfica SIC.
