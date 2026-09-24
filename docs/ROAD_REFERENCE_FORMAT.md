# Catálogo de ejes y PR

La aplicación carga `app/src/main/assets/road_reference.json`. El archivo entregado está vacío; no se han convertido las rutas o registros históricos de los ejemplos en cartografía oficial.

La empresa puede suministrar JSON o un GeoJSON FeatureCollection dentro de ese mismo archivo. Toda geometría debe estar en WGS84 (grados decimales); no se aceptan coordenadas UTM como latitud/longitud. Cada vértice requiere su progresiva conocida: la longitud geométrica no se inventa como progresiva oficial.

JSON (ejemplo sintético de contrato, no datos de campo):

```json
{
  "segments": [{
    "routeCode": "RUTA_CLIENTE", "roadbedCode": "CD", "segmentId": "TRAMO_1",
    "points": [
      {"sequence":0,"latitude":-12.0,"longitude":-77.0,"chainageM":1000.0},
      {"sequence":1,"latitude":-12.001,"longitude":-77.001,"chainageM":1150.0}
    ]
  }],
  "prs": [{"routeCode":"RUTA_CLIENTE","roadbedCode":"CD","prCode":"0010",
    "latitude":-12.0,"longitude":-77.0,"chainageM":1000.0}]
}
```

GeoJSON: cada `LineString` contiene `properties.routeCode`, `roadbedCode`, `segmentId` y un arreglo `chainageM` de la misma longitud que `geometry.coordinates`. Coordenadas en orden `[longitud, latitud]`. Cada `Point` de PR contiene `routeCode`, `roadbedCode`, `prCode`, `chainageM`. `roadbedCode` vacío significa cartografía sin distinción de calzadas; no permite aceptar una calzada inventada.

El importador rechaza identificadores/PR duplicados, coordenadas fuera de rango, menos de dos vértices, secuencias repetidas y progresivas no monótonas. Puede aceptar vértices crecientes o decrecientes y orienta el lado según progresiva creciente. Dividir ramales y discontinuidades en segmentos distintos. `RoadReferenceImporter` permite incorporar más adelante lectores KML/KMZ, sin alterar el cálculo. SHP/GDB necesitan una conversión acordada con el área GIS; no están implementados.

El cálculo usa PR oficial + distancia. Si no hay catálogo para esa ruta y calzada usa código PR numérico × 1000 + distancia y marca `ESTIMATED_FROM_PR_CODE`. Si existe catálogo pero falta el PR solicitado, muestra posición desconocida y avisa, sin suponer que ese PR es un kilómetro.

El matcher hace proyección local sobre segmentos cercanos e interpola sus progresivas. Usa umbral de distancia al eje y precisión, y rechaza calzadas/rutas con coincidencia ambigua. Sin PR anterior no inventa uno. La UI requiere aceptar la ubicación y confirmar el lado por separado. Los límites son configurables desde Inicio; la geometría de producción debe densificarse adecuadamente y probarse en cruces, curvas y calzadas paralelas antes de campo. El algoritmo actual recorre los segmentos en memoria; catálogos regionales muy grandes pueden requerir un índice espacial en una siguiente iteración.
