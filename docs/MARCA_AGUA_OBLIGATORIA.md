# Marca corporativa obligatoria

Las nuevas fotografías SIC y SCAP reciben automáticamente una copia con el logo GBO en la esquina superior derecha. El original permanece intacto. Los textos técnicos son opcionales; quitar textos no permite enviar una foto sin logo.

Antes de cada subida SIC a Drive y de preparar las fotografías del Excel SCAP, `RequiredWatermark` comprueba la copia. Una marca EXIF identifica las copias producidas con logo por la aplicación y permite reutilizarlas sin volver a dibujarlo. Si falta la copia o su marca, se genera otra; si no puede generarse, se detiene la entrega. Una copia antigua con textos conserva esos textos al procesarla. Las copias temporales de entrega se eliminan al finalizar.

Los croquis SCAP siguen su flujo independiente. Los archivos ya subidos a Drive y los Excel previamente exportados no se modifican retroactivamente. El servidor que evita duplicados por nombre tampoco reemplaza automáticamente archivos antiguos.

Verificación realizada: `:app:testDebugUnitTest --tests '*CorporateWatermarkTest' :app:assembleDebug` completó correctamente. Incluye logo visible sin interacción, preservación del original, reutilización de copia, regeneración de copia ausente y error cuando no hay imagen válida. Falta una comprobación visual en la tablet y una subida real a Drive.
