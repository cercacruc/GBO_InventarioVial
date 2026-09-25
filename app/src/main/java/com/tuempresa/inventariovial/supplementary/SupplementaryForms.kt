package com.tuempresa.inventariovial.supplementary

import com.tuempresa.inventariovial.BuildConfig

enum class FieldKind { TEXT, YEAR, INTEGER, DECIMAL, SIGNED_DECIMAL, LATITUDE, LONGITUDE, CODE, DESCRIBED_CODE }
data class SupplementaryField(val key: String, val label: String, val kind: FieldKind, val options: List<String> = emptyList())
enum class SupplementaryFormat(val title: String) { SIC17A("SIC-17A"), SIC17B("SIC-17B"), SIC18A("SIC-18A");
    val enabled get() = when(this) { SIC17A -> BuildConfig.SIC17A_ENABLED; SIC17B -> BuildConfig.SIC17B_ENABLED; SIC18A -> true }
}
data class SupplementaryFormState(val format: SupplementaryFormat, val values: Map<String, String> = emptyMap()) {
    fun validate(): List<String> = SupplementaryForms.fields(format, values).mapNotNull { field ->
        val value=values[field.key].orEmpty().trim()
        if(value.isEmpty()) return@mapNotNull if(format==SupplementaryFormat.SIC18A) "${field.label}: obligatorio." else null // These optional forms can be progressively completed.
        val number=value.replace(',', '.').toDoubleOrNull()
        val valid=when(field.kind) {
            FieldKind.TEXT -> true
            FieldKind.YEAR -> value.matches(Regex("[0-9]{4}")) && value.toInt() in 1000..java.time.Year.now().value
            FieldKind.INTEGER -> value.toIntOrNull()?.let { if(format==SupplementaryFormat.SIC18A && field.key=="spans") it>0 else it>=0 } == true
            FieldKind.DECIMAL -> number!=null && number.isFinite() && number>=0 && value.matches(Regex("[0-9]+([.,][0-9]{1,2})?"))
            FieldKind.SIGNED_DECIMAL -> number?.isFinite()==true
            FieldKind.LATITUDE -> number!=null && number in -90.0..90.0
            FieldKind.LONGITUDE -> number!=null && number in -180.0..180.0
            FieldKind.CODE -> field.options.any { it.substringBefore(" - ")==value }
            FieldKind.DESCRIBED_CODE -> value in field.options
        }
        if(valid) null else "${field.label}: valor inválido."
    }
}
object SupplementaryForms {
    fun fields(format: SupplementaryFormat, values: Map<String,String> = emptyMap()): List<SupplementaryField> {
        val fields=definitions.getValue(format)
        return if(format==SupplementaryFormat.SIC18A && values["classCode"]=="07") fields.map {
            if(it.key=="typeCode") it.copy(options=com.tuempresa.inventariovial.catalog.SicCatalogRepository.options("sic18.type.1")) else it
        } else fields
    }
    private val definitions = mapOf(
        SupplementaryFormat.SIC17A to listOf(
            SupplementaryField("bridgeName","Nombre del puente",FieldKind.TEXT,listOf()),
            SupplementaryField("bridgeCode","Código del puente",FieldKind.TEXT,listOf()),
            SupplementaryField("constructionYear","Año de construcción",FieldKind.YEAR,listOf()),
            SupplementaryField("department","Departamento",FieldKind.TEXT,listOf()),
            SupplementaryField("province","Provincia",FieldKind.TEXT,listOf()),
            SupplementaryField("district","Distrito",FieldKind.TEXT,listOf()),
            SupplementaryField("nearbyTown","Poblado cercano",FieldKind.TEXT,listOf()),
            SupplementaryField("latitude","Latitud WGS84",FieldKind.LATITUDE,listOf()),
            SupplementaryField("longitude","Longitud WGS84",FieldKind.LONGITUDE,listOf()),
            SupplementaryField("altitude","Altitud (msnm)",FieldKind.SIGNED_DECIMAL,listOf()),
            SupplementaryField("lanes","Número de carriles",FieldKind.INTEGER,listOf()),
            SupplementaryField("roadwayWidthM","Ancho calzada (m)",FieldKind.DECIMAL,listOf()),
            SupplementaryField("sidewalkWidthM","Ancho vereda (m)",FieldKind.DECIMAL,listOf()),
            SupplementaryField("deckWidthM","Ancho tablero (m)",FieldKind.DECIMAL,listOf()),
            SupplementaryField("superstructureWidthM","Ancho superestructura (m)",FieldKind.DECIMAL,listOf()),
            SupplementaryField("alignmentCode","Alineamiento",FieldKind.CODE,listOf("1 - Recto","2 - Curvo","3 - Esviado","4 - Otro"))
        ),
        SupplementaryFormat.SIC17B to listOf(
            SupplementaryField("bridgeCode","Código del puente",FieldKind.TEXT,listOf()),
            SupplementaryField("designLoad","Sobrecarga de diseño (ficha p. 212)",FieldKind.TEXT,listOf()),
            SupplementaryField("maximumCapacity","Capacidad máxima (tabla p. 243; unidad por confirmar)",FieldKind.DECIMAL,listOf()),
            SupplementaryField("wearingSurface","Superficie de desgaste (3 duplicado en Manual)",FieldKind.DESCRIBED_CODE,listOf("1 - Asfalto","2 - Concreto de la losa","3 - Concreto pobre","3 - Acero","4 - Madera","5 - Afirmado","6 - Otro")),
            SupplementaryField("vehicleRestraintCode","Contención vehicular",FieldKind.CODE,listOf("0 - No presenta","1 - Barrera","2 - Guardavía","3 - Baranda de concreto","4 - Baranda de acero","5 - Baranda combinada","6 - Otro")),
            SupplementaryField("mainSpanM","Tramo principal: luz (m)",FieldKind.DECIMAL,listOf()),
            SupplementaryField("boundaryCode","Condición de borde",FieldKind.CODE,listOf("1 - Simplemente apoyado","2 - Continuo","3 - Aporticado","4 - Colgado","5 - Sobre el terreno","6 - Otro")),
            SupplementaryField("crossSectionCode","Sección transversal",FieldKind.CODE,listOf("1 - Losa construida in situ","2 - Losa prefabricada","3 - Losa y vigas in situ","4 - Losa y vigas prefabricadas de concreto","5 - Losa y vigas de acero","6 - Cajón de concreto","7 - Cajón de acero","8 - Otro")),
            SupplementaryField("beams","Número de vigas",FieldKind.INTEGER,listOf()),
            SupplementaryField("slabMaterialCode","Material losa",FieldKind.CODE,listOf("0 - No aplica","1 - Concreto armado","2 - Concreto preesforzado","3 - Acero estructural","4 - Acero corrugado","5 - Madera","6 - Piedra","7 - Otro")),
            SupplementaryField("beamMaterialCode","Material vigas",FieldKind.CODE,listOf("0 - No aplica","1 - Concreto armado","2 - Concreto preesforzado","3 - Acero estructural","4 - Acero corrugado","5 - Madera","6 - Piedra","7 - Otro")),
            SupplementaryField("abutmentElevationCode","Estribos: elevación",FieldKind.CODE,listOf("0 - No aplica","1 - Gravedad","2 - Muro en voladizo","3 - Muro con contrafuertes","4 - Pórtico","5 - Cepa","6 - Otro")),
            SupplementaryField("abutmentMaterialCode","Estribos: material",FieldKind.CODE,listOf("0 - No aplica","1 - Concreto armado","2 - Acero","3 - Madera","4 - Piedra","5 - Otro")),
            SupplementaryField("abutmentFoundationCode","Estribos: cimentación",FieldKind.CODE,listOf("0 - No aplica","1 - Zapata","2 - Cajón","3 - Pilotes","4 - Otro")),
            SupplementaryField("pierElevationCode","Pilares: elevación",FieldKind.CODE,listOf("0 - No aplica","1 - Columna","2 - Muro","3 - Pórtico","4 - Cepa","5 - Celosía","6 - Otro")),
            SupplementaryField("pierMaterialCode","Pilares: material",FieldKind.CODE,listOf("0 - No aplica","1 - Concreto armado","2 - Acero","3 - Madera","4 - Piedra","5 - Otro")),
            SupplementaryField("pierFoundationCode","Pilares: cimentación",FieldKind.CODE,listOf("0 - No aplica","1 - Zapata","2 - Cajón","3 - Pilotes","4 - Otro")),
            SupplementaryField("comments","Comentarios",FieldKind.TEXT,listOf())
        ),
        SupplementaryFormat.SIC18A to listOf(
            SupplementaryField("classCode","Clase",FieldKind.CODE,listOf("06 - Alcantarilla definitiva","07 - Alcantarilla estructura artesanal")),
            SupplementaryField("typeCode","Tipo (según clase)",FieldKind.CODE,listOf("1 - Concreto","2 - Mampostería","3 - Acero","4 - Polietileno HDPE","5 - Otro")),
            SupplementaryField("spans","Ojos / vanos",FieldKind.INTEGER,listOf()),
            SupplementaryField("functionCode","Función",FieldKind.CODE,listOf("1 - Drenaje de quebrada importante","2 - Drenaje de quebrada secundaria","3 - Drenaje de cunetas","4 - Pase de agua")),
            SupplementaryField("failureLocationCode","Falla estructural: ubicación",FieldKind.CODE,listOf("1 - Cabezal de entrada","2 - Cabezal de salida","3 - Alcantarilla","4 - Ambos cabezales","5 - Todas las estructuras")),
            SupplementaryField("failureTypeCode","Falla estructural: tipo",FieldKind.CODE,listOf("1 - Colapso","2 - Deformación","3 - Fractura","4 - Hundimiento","5 - Socavación","6 - Diámetro mínimo","7 - Otros")),
            SupplementaryField("functionalStateCode","Falla funcional: estado",FieldKind.CODE,listOf("1 - Ahogada","2 - Obstruida","3 - Otras")),
            SupplementaryField("probableCauseCode","Falla funcional: causa probable",FieldKind.CODE,listOf("1 - Subdimensionado","2 - Poca pendiente","3 - Huayco","4 - Otros"))
        ),
    )
}
