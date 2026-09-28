package com.tuempresa.inventariovial.road

import com.tuempresa.inventariovial.data.entity.InventoryRecordWithPhotos
import java.text.Normalizer
import java.util.Locale

enum class AssetFamily(val abbreviation: String, val label: String, val folderLabel: String,
    val mapFolder: String, val rgb: Int) {
    PUENTE("P", "Puente", "PUENTE", "PUENTES", 0x228B22),
    ALCANTARILLA("ALC", "Alcantarilla", "ALCANTARILLA", "ALCANTARILLAS", 0x0066FF),
    CUNETA("CUN", "Cuneta", "CUNETA", "CUNETAS", 0xFF8800),
    BADEN("BAD", "Badén", "BADEN", "BADENES", 0x9933CC),
    TUNEL("TUN", "Túnel", "TUNEL", "OTROS", 0x808080),
    MURO("MUR", "Muro", "MURO", "OTROS", 0x808080),
    SENALIZACION_VERTICAL("SV", "Señalización vertical", "SEÑALIZACION VERTICAL", "SEÑALIZACIÓN", 0xFF0000),
    CANAL("CAN", "Canal", "CANAL", "OTROS", 0x808080),
    BAJADA_AGUA("BAJ", "Bajada de agua", "BAJADA DE AGUA", "OTROS", 0x808080),
    ZANJA_DRENAJE("ZD", "Zanja de drenaje", "ZANJA DE DRENAJE", "OTROS", 0x808080),
    SENALIZACION_HORIZONTAL("SH", "Señalización horizontal", "SEÑALIZACION HORIZONTAL", "SEÑALIZACIÓN", 0x808080),
    DERECHO_DE_VIA("DV", "Derecho de vía", "DERECHO DE VIA", "OTROS", 0x808080),
    OTROS("ELM", "Otros/no clasificados", "ELEMENTO", "OTROS", 0x808080);

    val kmlColor: String get() = String.format(Locale.ROOT, "ff%02x%02x%02x", rgb and 255, (rgb shr 8) and 255, (rgb shr 16) and 255)
}

/** Single presentation catalog for ordering, UI, folder plans and GIS. Detail classes take precedence. */
object AssetPresentationCatalog {
    val legend = listOf(AssetFamily.ALCANTARILLA, AssetFamily.SENALIZACION_VERTICAL,
        AssetFamily.PUENTE, AssetFamily.CUNETA, AssetFamily.BADEN, AssetFamily.OTROS)

    fun resolve(item: InventoryRecordWithPhotos): AssetFamily {
        val r = item.record
        when (r.sicCode) {
            "SIC-17" -> return AssetFamily.PUENTE
            "SIC-18" -> return AssetFamily.ALCANTARILLA
            "SIC-22" -> return AssetFamily.SENALIZACION_VERTICAL
            "SIC-19" -> item.sic19?.let { return when (it.classCode) {
                "08", "13" -> AssetFamily.CUNETA
                "09" -> AssetFamily.CANAL
                "10" -> AssetFamily.BAJADA_AGUA
                "11" -> AssetFamily.ZANJA_DRENAJE
                else -> AssetFamily.OTROS
            } }
            "SIC-20" -> item.sic20?.let { return when (it.classCode) {
                "12" -> AssetFamily.BADEN
                "13" -> AssetFamily.TUNEL
                "14" -> AssetFamily.MURO
                else -> AssetFamily.OTROS
            } }
            "SIC-23" -> item.sic23?.let { return if (it.classCode == "21") AssetFamily.DERECHO_DE_VIA else AssetFamily.OTROS }
        }
        val type = Normalizer.normalize(r.assetType, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").trim().uppercase(Locale.ROOT).replace(' ', '_')
        return when (type) {
            "BRIDGE", "PUENTE" -> AssetFamily.PUENTE
            "CULVERT", "ALCANTARILLA" -> AssetFamily.ALCANTARILLA
            "DITCH", "CUNETA" -> AssetFamily.CUNETA
            "FORD", "BADEN" -> AssetFamily.BADEN
            "TUNNEL", "TUNEL" -> AssetFamily.TUNEL
            "WALL", "MURO" -> AssetFamily.MURO
            "VERTICAL_SIGN", "SENALIZACION_VERTICAL" -> AssetFamily.SENALIZACION_VERTICAL
            "CANAL" -> AssetFamily.CANAL
            "BAJADA_AGUA", "BAJADA_DE_AGUA" -> AssetFamily.BAJADA_AGUA
            "ZANJA_DRENAJE", "ZANJA_DE_DRENAJE" -> AssetFamily.ZANJA_DRENAJE
            "HORIZONTAL_MARKS", "HORIZONTAL_STUDS", "SENALIZACION_HORIZONTAL" -> AssetFamily.SENALIZACION_HORIZONTAL
            "RIGHT_OF_WAY", "DERECHO_VIA", "DERECHO_DE_VIA" -> AssetFamily.DERECHO_DE_VIA
            else -> AssetFamily.OTROS
        }
    }
}
