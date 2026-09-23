package com.tuempresa.inventariovial.model.form

data class InventoryFormState(

    // Datos compartidos entre los formatos.
    val common: CommonInventoryFormState =
        CommonInventoryFormState(),


    // SIC-17
    val sic17: Sic17FormState =
        Sic17FormState(),


    // SIC-18
    val sic18: Sic18FormState =
        Sic18FormState(),


    // SIC-19
    val sic19: Sic19FormState =
        Sic19FormState(),


    // SIC-20
    val sic20: Sic20FormState =
        Sic20FormState(),


    // SIC-21
    val sic21: Sic21FormState =
        Sic21FormState(),


    // SIC-22
    val sic22: Sic22FormState =
        Sic22FormState(),

    val sic23: Sic23FormState = Sic23FormState()
)