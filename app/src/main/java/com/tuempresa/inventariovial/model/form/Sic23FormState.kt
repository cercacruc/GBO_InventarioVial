package com.tuempresa.inventariovial.model.form

import java.text.Normalizer
import java.util.Locale

/** Manual IV-2014/2015, tabla III.10 y formato SIC-23 (páginas 155-157). */
data class Sic23FormState(
    val classCode: String = "21",
    val typeCode: String = "1",
    val widthM: String = "",
    val description: String = ""
) {
    val typeOptions: List<String>
        get() = when (classCode) {
            "21" -> listOf("1 - Ancho total", "2 - Berma central", "3 - Obstrucción",
                "4 - Instalación de servicio público", "5 - Vereda")
            "23" -> listOf("1 - Cruce importante", "2 - Otro")
            else -> emptyList()
        }

    val usesWidth: Boolean get() = classCode == "21" && typeCode in listOf("1", "2")

    val assetName: String get() = when (classCode) {
        "22" -> "ZONA_URBANA"
        "23" -> "PUNTO_ESPECIFICO"
        "24" -> "CANTERA"
        else -> "DERECHO_VIA"
    }

    fun selectClass(code: String): Sic23FormState = copy(
        classCode = code, typeCode = if (code in listOf("21", "23")) "1" else "",
        widthM = "", description = ""
    )

    fun selectType(code: String): Sic23FormState {
        val next = copy(typeCode = code)
        return next.copy(widthM = if (next.usesWidth) widthM else "")
    }

    fun validationError(): String? {
        if (classCode !in listOf("21", "22", "23", "24")) return "Selecciona una clase SIC-23 válida."
        if (typeOptions.isEmpty()) {
            if (typeCode.isNotBlank()) return "Esta clase no tiene tipo."
        } else if (typeOptions.none { it.substringBefore(" - ") == typeCode }) {
            return "Selecciona un tipo válido para la clase."
        }
        if (usesWidth) {
            val value = widthM.trim().replace(',', '.')
            val number = value.toDoubleOrNull()
            if (!value.matches(Regex("[0-9]+(\\.[0-9]{1,2})?")) ||
                number == null || !number.isFinite() || number < 0) {
                return "Ingresa el ancho en metros, positivo o cero y con hasta dos decimales."
            }
        }
        return null
    }

    fun normalizedDescription(): String = Normalizer.normalize(description, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .uppercase(Locale.ROOT)
        .replace(Regex("[^A-Z0-9\\s]"), " ")
        .trim().replace(Regex("\\s+"), " ")

    companion object {
        val classOptions = listOf("21 - Derecho de vía", "22 - Zona urbana",
            "23 - Punto específico", "24 - Cantera")

        fun locationError(route: String, roadbed: String, startPr: String, startDistance: String,
                          endPr: String?, endDistance: String?, side: String?, checkEstimatedOrder: Boolean = true): String? {
            if (route.isBlank() || roadbed.isBlank()) return "Ruta y calzada son obligatorias."
            if (!roadbed.trim().matches(Regex("[A-Za-z0-9]+"))) return "La calzada debe ser alfanumérica."
            if (!startPr.trim().matches(Regex("[0-9]{1,4}")) ||
                !endPr.orEmpty().trim().matches(Regex("[0-9]{1,4}"))) return "Completa los PR de inicio y fin con hasta cuatro dígitos."
            val start = startDistance.trim().replace(',', '.').toDoubleOrNull()
            val end = endDistance?.trim()?.replace(',', '.')?.toDoubleOrNull()
            if (start == null || !start.isFinite() || start < 0 ||
                end == null || !end.isFinite() || end < 0) return "Las distancias de inicio y fin deben ser números positivos o cero."
            if (checkEstimatedOrder && endPr!!.trim().toInt() * 1000.0 + end < startPr.trim().toInt() * 1000.0 + start)
                return "La ubicación final no puede ser anterior a la inicial."
            if (side?.trim()?.uppercase(Locale.ROOT) !in listOf("D", "I", "S")) return "Selecciona el lado D, I o S."
            return null
        }
    }
}
