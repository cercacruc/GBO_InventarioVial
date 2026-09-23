package com.tuempresa.inventariovial

object DriveFolderRouter {

    fun resolveSib(
        sicCode: String,
        assetType: String
    ): String {

        val sic =
            sicCode
                .trim()
                .uppercase()

        val asset =
            assetType
                .trim()
                .uppercase()

        return when (sic) {

            "SIC-17" ->
                "SIB-08"

            "SIC-18" ->
                "SIB-02"

            "SIC-19" ->
                "SIB-02"

            "SIC-20" ->
                "SIB-02"

            "SIC-21" -> {
                when (asset) {
                    "SAFETY" ->
                        "SIB-02"

                    "HORIZONTAL_MARKS",
                    "HORIZONTAL_STUDS" ->
                        "SIB-07"

                    else ->
                        "SIB-07"
                }
            }

            // Convención de la aplicación: elementos de derecho de vía en SIB-02.
            "SIC-23" -> "SIB-02"

            "SIC-22" ->
                "SIB-07"

            else ->
                throw IllegalArgumentException(
                    "No existe una carpeta SIB configurada para $sicCode / $assetType"
                )
        }
    }
}
