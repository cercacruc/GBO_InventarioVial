package com.tuempresa.inventariovial.scap.export

import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import org.json.JSONObject

/** Prepared contract only: no network client, worker or SIC routing calls. */
object ScapDrivePayload {
    const val uploadEnabled=false
    fun bridgeIdentity(bridgeCode:String,inspectionId:String):String =
        bridgeCode.trim().takeIf{it.isNotEmpty()}?.let{"code:$it"} ?: "inspection:$inspectionId"
    fun prepare(s:ScapInspectionSnapshot,p:PhotoEntity,fileName:String,mimeType:String,base64:String):JSONObject {
        require(p.scapInspectionId==s.inspection.id && p.recordId==s.inspection.roadRecordId){"La foto no pertenece a esta inspección."}
        require(fileName.isNotBlank() && mimeType.startsWith("image/") && base64.isNotBlank())
        return JSONObject().put("recordKind","SCAP").put("scapInspectionId",s.inspection.id)
            .put("bridgeCode",s.inspection.bridgeCode).put("bridgeName",s.inspection.bridgeName)
            .put("routeCode",s.values()["route"].orEmpty()).put("photoCategory",p.photoCategory ?: JSONObject.NULL)
            .put("scapElementCode",p.scapElementCode ?: JSONObject.NULL).put("fileName",fileName)
            .put("mimeType",mimeType).put("base64",base64)
    }
}
