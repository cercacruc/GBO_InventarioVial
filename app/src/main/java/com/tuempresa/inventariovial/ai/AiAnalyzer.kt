package com.tuempresa.inventariovial.ai

import com.tuempresa.inventariovial.data.entity.PhotoEntity

data class AiResult(val suggestedClass: String?, val suggestedSubtype: String?, val confidence: Double?,
    val suggestedCondition: String?, val suggestedSignalCode: String?, val suggestedMaterial: String? = null)
interface AiAnalyzer { suspend fun analyze(photo: PhotoEntity): AiResult }
class DisabledAiAnalyzer : AiAnalyzer {
    override suspend fun analyze(photo: PhotoEntity) = AiResult(null,null,null,null,null)
}
