package com.tuempresa.inventariovial.scap.domain

data class ScapAiContext(val inspectionId:String,val selectedElementCodes:List<String>)
data class ScapAiComponent(val suggestedScapCode:String?,val visibleComponent:String,val visibleMaterial:String?,val visibleType:String?)
data class ScapAiResult(val detectedComponents:List<ScapAiComponent>,val visibleDefects:List<String>,
    val suggestedBridgeCategory:String?,val suggestedBridgeType:String?,val confidence:Double?,val fieldsRequiringReview:List<String>)
/** Visual suggestions only. No dimensions, quantities, percentages, capacity, hydraulics or hidden foundations. */
interface ScapAiAnalyzer {
    suspend fun analyzePhoto(photoUri:String,context:ScapAiContext):ScapAiResult
}
class DisabledScapAiAnalyzer:ScapAiAnalyzer {
    override suspend fun analyzePhoto(photoUri:String,context:ScapAiContext)=
        ScapAiResult(emptyList(),emptyList(),null,null,null,listOf("IA SCAP no integrada; evaluación manual del ingeniero."))
}
