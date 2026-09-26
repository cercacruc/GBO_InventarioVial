package com.tuempresa.inventariovial.scap.domain

import com.tuempresa.inventariovial.catalog.SicCatalogRepository
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot

/** SIC-only values live in the existing key/value table, outside the SCAP field map. */
object ScapSicSupplement {
    const val PREFIX = "sic17."
    val labels = linkedMapOf(
        "roadbedCode" to "Calzada", "classCode" to "Clase", "typeCode" to "Tipo",
        "inventoriedCode" to "Inventariado", "structuralConditionCode" to "Condición estructural",
        "functionalConditionCode" to "Condición funcional", "serviceTypeCode" to "Tipo de servicio",
        "singularityCode" to "Singularidad salvada", "singularityName" to "Nombre de singularidad")

    fun options(key: String, values: Map<String, String>): List<String> = when (key) {
        "roadbedCode" -> SicCatalogRepository.roadbedCodes
        "typeCode" -> SicCatalogRepository.options("sic17.type." + when (values[PREFIX + "classCode"]) {
            "01" -> 0; "02" -> 1; "03" -> 2; "04" -> 3; else -> 4
        })
        "classCode" -> SicCatalogRepository.options("sic17.class.0")
        "inventoriedCode" -> SicCatalogRepository.options("sic17.inventoried.0")
        "structuralConditionCode" -> SicCatalogRepository.options("sic17.structural.0")
        "functionalConditionCode" -> SicCatalogRepository.options("sic17.functional.0")
        "serviceTypeCode" -> SicCatalogRepository.options("sic17.service.0")
        "singularityCode" -> SicCatalogRepository.options("sic17.singularity.0")
        else -> emptyList()
    }

    fun fields(s: ScapInspectionSnapshot): List<ScapMappedField> {
        val values = s.values()
        return labels.keys.mapNotNull { key ->
            val raw = values[PREFIX + key].orEmpty().trim()
            val valid = key in setOf("roadbedCode", "singularityName") || options(key, values).any { it.substringBefore(" - ") == raw }
            if (raw.isBlank() || !valid || (key == "singularityName" && !values["singularityName"].isNullOrBlank())) null
            else ScapMappedField(PREFIX + key, key, raw, ScapMappingStatus.DIRECT, "Confirmado por el ingeniero; exclusivo de SIC-17.")
        }
    }
}
