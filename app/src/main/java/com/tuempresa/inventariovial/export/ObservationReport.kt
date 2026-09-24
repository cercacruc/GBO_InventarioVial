package com.tuempresa.inventariovial.export

import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity

/** Separate editable-in-browser report. Text is escaped, never interpreted as HTML. */
object ObservationReport {
    fun render(records: List<InventoryRecordEntity>): String {
        fun escape(value: String?) = value.orEmpty().replace("&","&amp;").replace("<","&lt;")
            .replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;")
        return buildString {
            append("<!doctype html><html lang=\"es\"><meta charset=\"utf-8\"><title>Observaciones del inventario vial</title>")
            append("<style>body{font:14px Arial;margin:32px;color:#172438}table{border-collapse:collapse;width:100%}td,th{border:1px solid #ccd3da;padding:8px;text-align:left}td{white-space:pre-wrap}h1{font-size:24px}@media print{thead{display:table-header-group}tr{break-inside:avoid}}</style>")
            append("<h1>Observaciones del inventario vial</h1><p>Documento de apoyo al informe. Plantilla contractual pendiente de confirmación.</p>")
            append("<table><thead><tr><th>Fecha / registro</th><th>Ubicación</th><th>Observaciones</th></tr></thead><tbody>")
            records.filter {it.status=="ACTIVE" && !it.observations.isNullOrBlank()}.forEach { r ->
                append("<tr><td>${escape(r.surveyDate)}<br>${escape(r.sicCode)} · ${escape(r.assetType)}<br>${escape(r.id)}</td>")
                append("<td>${escape(r.segment)}<br>Ruta ${escape(r.routeCode)} · ${escape(r.roadbedCode)}<br>${escape(r.startPrCode)} + ${r.startDistanceM} m</td>")
                append("<td>${escape(r.observations)}</td></tr>")
            }
            append("</tbody></table></html>")
        }
    }
}
