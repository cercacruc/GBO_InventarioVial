package com.tuempresa.inventariovial.validation

import com.tuempresa.inventariovial.model.form.InventorySaveRequest
import com.tuempresa.inventariovial.model.form.SicFormDetail

/** Provisional engineering policy, identical to the previous MISSING_END_GPS rule. */
fun requiresEndLocation(sicCode:String,assetType:String="",sic20Class:String?=null,sic23Class:String?=null):Boolean =
    sicCode in setOf("SIC-19","SIC-21") || assetType in setOf("MURO","TUNEL") ||
        sic20Class in setOf("13","14") || (sicCode=="SIC-23" && sic23Class !in setOf("23","24"))

fun requiresEndLocation(request:InventorySaveRequest)=requiresEndLocation(request.sicCode,request.assetType,
    (request.detail as? SicFormDetail.Sic20)?.state?.classCode,(request.detail as? SicFormDetail.Sic23)?.state?.classCode)
