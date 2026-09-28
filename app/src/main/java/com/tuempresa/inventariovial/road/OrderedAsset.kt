package com.tuempresa.inventariovial.road

import com.tuempresa.inventariovial.DriveFolderRouter
import com.tuempresa.inventariovial.data.entity.InventoryRecordWithPhotos

enum class AssetOrderSource { GNSS_MAP_MATCH, MANUAL_CHAINAGE_FALLBACK, CREATED_AT_FALLBACK }

data class OrderedAsset(
    val item: InventoryRecordWithPhotos,
    val routeCode: String,
    val sibCode: String?,
    val assetFamily: AssetFamily,
    val axisMeasureM: Double?,
    val sequence: Int,
    val orderSource: AssetOrderSource,
    val fallbackPosition: RoadPosition,
    val warnings: List<String>
) {
    val recordId get() = item.record.id
    val shortCode get() = "${assetFamily.abbreviation}-$sequence"
    val folderName get() = "${assetFamily.folderLabel} $sequence"
}

internal fun orderedAssets(records: List<InventoryRecordWithPhotos>, calculator: ChainageCalculator): List<OrderedAsset> {
    val rows = records.filter { it.record.status == "ACTIVE" && it.record.sicCode != "SCAP" }.map { item ->
        val r = item.record
        val family = AssetPresentationCatalog.resolve(item)
        val sib = runCatching { DriveFolderRouter.resolveSib(r.sicCode, r.assetType) }.getOrNull()
        val axis = r.axisMeasureM?.takeIf { it.isFinite() && it >= 0 }
        val fallback = calculator.calculate(r)
        OrderedAsset(item, normalizedRoadCode(r.routeCode), sib, family, axis, 0,
            when { axis != null -> AssetOrderSource.GNSS_MAP_MATCH
                fallback.chainageM?.isFinite() == true -> AssetOrderSource.MANUAL_CHAINAGE_FALLBACK
                else -> AssetOrderSource.CREATED_AT_FALLBACK }, fallback,
            buildList {
                if (family == AssetFamily.OTROS) add("Familia sin abreviatura configurada: ${r.sicCode} / ${r.assetType}; se usa ELM.")
                if (sib == null) add("SIB no configurado: ${r.sicCode} / ${r.assetType}.")
                if (r.routeCode.isBlank()) add("Ruta no seleccionada.")
            })
    }.sortedWith(compareBy<OrderedAsset> { it.routeCode }
        // Geometric and contractual origins differ: unmatched records follow matched records.
        .thenBy { it.orderSource.ordinal }
        .thenBy { it.axisMeasureM ?: it.fallbackPosition.chainageM?.takeIf(Double::isFinite) ?: Double.POSITIVE_INFINITY }
        .thenBy { it.item.record.createdAt }.thenBy { it.recordId })
    val counters = mutableMapOf<Triple<String, String?, AssetFamily>, Int>()
    return rows.map { row ->
        val key = Triple(row.routeCode, row.sibCode, row.assetFamily)
        val sequence = (counters[key] ?: 0) + 1
        counters[key] = sequence
        row.copy(sequence = sequence)
    }
}
