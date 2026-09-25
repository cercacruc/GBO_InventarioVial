# Cálculo SCAP del archivo de referencia

Fuente: `reference/SCAP_PUENTE_AGUA_BLANCA.xlsx`, copia idéntica del archivo del usuario. No se modificó ni recalculó el Excel. Se inspeccionaron fórmulas y valores almacenados, y se reprodujeron las operaciones fuera de Excel antes de implementar Kotlin.

## Algoritmo observado en G.- CONDICION ESTADISTICA

1. Filas 14–27, columnas F:K: porcentajes de campo en orden 0,1,2,3,4,5. Se valida cada valor en [0,100] y suma 100 ± 0.01. La UI almacena porcentajes, no fracciones.
2. F48:K48 contiene umbrales [25,25,25,25,25,3]. Tabla 2 (51–64): `a[i] = p[i] / umbral[i] * 100`. **Sin redondeo intermedio.**
3. Tabla 3 (87–100): se usan literalmente las fórmulas asimétricas del archivo. Con índices 0..5:
   - `c5 = a5`
   - `c4 = c5 > 100 ? 0 : c5 + a4`
   - `c3 = c4 > 100 ? 0 : c4 + a3`
   - `c2 = c3 >= 100 ? 0 : c3 + a2`
   - `c1 = c2 >= 100 || c3 >= 100 ? 0 : c2 + a1`
   - `c0 = c1 >= 100 || c2 >= 100 || c3 >= 100 ? 0 : c1 + a0`
4. Tabla 4 (122–135): para i=0..3, si `sum(c[i..5]) >=100`, tomar `max(0,100-sum(c[i+1..5]))`; de lo contrario `max(0,c[i])`. Para i=4 la comparación es estricta `>100`. Para i=5 se conserva c5 directamente. No normalizar silenciosamente esta distribución.
5. Tabla 5 (157–170): `condition = (sum(i^5 * final[i] /100))^(1/5)`. Multiplicar por factor de importancia del catálogo operativo AUXILIAR/F para obtener la contribución. El metrado no pondera esta fórmula.
6. E186:E191: n = número de elementos presentes evaluados, m = contribución máxima, s = suma de contribuciones. `bridge = m + (s-m)/(m*(n-1))`.
7. M12: `>4 → MUY MALO`, `>3 → MALO`, `>2 → REGULAR`, en otro caso `BUENO`. Presentación numérica a tres decimales; no redondear antes de clasificar.

## Reproducción Agua Blanca

14 elementos; contribución máxima 1.6804320641498556; suma 10.192663602363911. Resultado **2.0700860438436695**, presentación **2.070**, clasificación **REGULAR**. La extracción genera un fixture desde G14:K27 con factores contrastados contra AUXILIAR, nunca una constante de retorno del calculador.

## Diferencias y límites que requieren confirmación

- Hoja1 introduce ROUNDDOWN y redondeo a dos decimales; la hoja G operativa no los aplica. Se prioriza G para reproducir el ejemplo solicitado.
- Hoja1 contiene factores históricos diferentes a AUXILIAR/F. El catálogo de la app usa AUXILIAR, tal como solicita el prompt.
- El porcentaje final de condición 5 no está limitado a 100 en la fórmula de G. Para ciertos porcentajes de campo superiores a 3%, la distribución resultante supera 100 y puede generar índices fuera de la escala. El calculador conserva la evidencia de la fórmula y devuelve «Requiere revisión de fórmula» en esos casos, sin inventar una corrección.
- n=1 o m=0 produce división por cero en la fórmula de puente. No se inventa un resultado; la evaluación individual sigue disponible y se indica por qué no hay índice global.
- La tabla de rangos J187:L192 usa intervalos y seis etiquetas, pero M12 utiliza cuatro etiquetas y comparaciones estrictas. La app identifica la clasificación como «según fórmula M12». Acordar la regla definitiva antes de usarla como criterio normativo.
- Una inspección incompleta o porcentajes inválidos no obtiene un resultado global que aparente estar completo.
