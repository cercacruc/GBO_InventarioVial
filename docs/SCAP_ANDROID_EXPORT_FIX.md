# Exportación SCAP compatible con Android

Se corrigió el fallo `http://xml.org/sax/features/external-general-entities` al abrir las partes XML de la plantilla. La fábrica DOM de Android no admite los indicadores SAX que se configuraban previamente.

La lectura comprueba primero los tokens XML con el lector de Android y rechaza declaraciones DTD, independientemente de su codificación. Después utiliza DOM con espacios de nombres y un resolvedor que rechaza entidades externas. Se mantienen los formatos, fórmulas y contenido de la plantilla.

Validación realizada:

- Compilación debug y pruebas `ScapExportTest` y `ScapFullExportTest` correctas.
- Tres pruebas instrumentadas aprobadas en Samsung SM-T733: exportar y reabrir la plantilla real con una ficha sintética; rechazar DTD internas y externas en UTF-8/UTF-16; conservar espacios de nombres, Unicode y texto escapado.
- APK instalado como actualización, sin borrar datos. Las pruebas no acceden a la base de datos del usuario.

Para exportar una ficha existente: abrir SCAP, seleccionar la ficha, ir a G y pulsar «Guardar Excel SCAP»; seleccionar el destino en el selector de Android.
