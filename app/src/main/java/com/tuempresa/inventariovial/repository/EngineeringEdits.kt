package com.tuempresa.inventariovial.repository

import androidx.room.withTransaction
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.catalog.EngineeringConditions
import com.tuempresa.inventariovial.validation.CaptureValidation
import java.util.UUID

object EngineeringEdits {
    fun form(s:InventorySnapshot):SicFormDetail = when(s.record.sicCode) {
        "SIC-18" -> with(requireNotNull(s.sic18)) {SicFormDetail.Sic18(Sic18FormState(classCode,typeCode,spans?.toString().orEmpty(),crossSectionCode,dimension1M?.toString().orEmpty(),dimension2M?.toString().orEmpty(),structuralConditionCode,functionalConditionCode,sectionShape=sectionShape.orEmpty()))}
        "SIC-19" -> with(requireNotNull(s.sic19)) {SicFormDetail.Sic19(Sic19FormState(classCode,typeCode,crossSectionCode,structuralConditionCode,functionalConditionCode,structuralCriterion.orEmpty()))}
        "SIC-20" -> with(requireNotNull(s.sic20)) {SicFormDetail.Sic20(Sic20FormState(classCode,typeCode,dimension1M?.toString().orEmpty(),dimension2M?.toString().orEmpty(),structuralConditionCode,functionalConditionCode.orEmpty(),wallLengthMeters?.toString().orEmpty()))}
        "SIC-21" -> with(requireNotNull(s.sic21)) {SicFormDetail.Sic21(Sic21FormState(classCode,typeCode,materialCode,conditionCode))}
        "SIC-22" -> with(requireNotNull(s.sic22)) {SicFormDetail.Sic22(Sic22FormState(classCode,typeCode,materialCode,signalCode.orEmpty(),kilometerPostNumber.orEmpty(),conditionCode,
            signWidthM?.toString().orEmpty(),signHeightM?.toString().orEmpty(),lowerEdgeHeightM?.toString().orEmpty()))}
        else -> error("Este editor requiere SIC-18 a SIC-22.")
    }
    suspend fun save(db:InventoryDatabase,id:String,detail:SicFormDetail,paths:List<String>,categories:Map<String,String>)=db.withTransaction {
        val dao=db.inventoryDao();val s=requireNotNull(dao.snapshot(id));val r=s.record
        require(r.status=="ACTIVE") {"Solo se pueden editar registros activos."}
        val request=InventorySaveRequest(r.sicCode,r.assetType,r.routeCode,r.roadbedCode,r.startPrCode,r.startDistanceM.toString(),
            r.endPrCode,r.endDistanceM?.toString(),r.sideCode,r.latitude,r.longitude,r.gpsAccuracyM?.toFloat(),r.surveyDate,r.observations.orEmpty(),"",detail,photoPaths=paths)
        val unchangedLegacyType=detail is SicFormDetail.Sic22 && detail.state.typeCode==s.sic22?.typeCode && detail.state.typeCode in setOf("4","5","6")
        val errors=CaptureValidation.errors(request).filterNot {unchangedLegacyType && it.field=="typeCode"}
        require(errors.isEmpty()) {errors.joinToString("\n") {it.message}}
        when(detail) {
            is SicFormDetail.Sic18 -> with(detail.state) {
                val old=requireNotNull(s.sic18)
                dao.updateSic18(old.copy(classCode=classCode,typeCode=typeCode,spans=spans.toInt(),crossSectionCode=crossSectionCode,
                    dimension1M=dimension1M.replace(',','.').toDoubleOrNull(),dimension2M=if(usesDimension2)dimension2M.replace(',','.').toDoubleOrNull() else old.dimension2M,
                    structuralConditionCode=structuralConditionCode,functionalConditionCode=functionalConditionCode,sectionShape=sectionShape))
                s.sic18a?.let {dao.saveSic18A(it.copy(classCode=classCode,typeCode=typeCode,spans=spans))}
            }
            is SicFormDetail.Sic19 -> with(detail.state) {dao.updateSic19(requireNotNull(s.sic19).copy(classCode=classCode,typeCode=typeCode,crossSectionCode=crossSectionCode,structuralConditionCode=structuralConditionCode,functionalConditionCode=functionalConditionCode,structuralCriterion=structuralCriterion.ifBlank {null}))}
            is SicFormDetail.Sic20 -> with(detail.state) {dao.updateSic20(requireNotNull(s.sic20).copy(classCode=classCode,typeCode=typeCode,dimension1M=dimension1M.replace(',','.').toDoubleOrNull(),dimension2M=if(usesDimension2)dimension2M.replace(',','.').toDoubleOrNull() else s.sic20.dimension2M,structuralConditionCode=structuralConditionCode,functionalConditionCode=functionalConditionCode,wallLengthMeters=wallLengthMeters.replace(',','.').toDoubleOrNull()))}
            is SicFormDetail.Sic21 -> with(detail.state) {dao.updateSic21(requireNotNull(s.sic21).copy(classCode=classCode,typeCode=typeCode,materialCode=materialCode,conditionCode=conditionCode))}
            is SicFormDetail.Sic22 -> with(detail.state) {
                // Removed capture dimensions and historical marker number remain intact in Room.
                dao.updateSic22(requireNotNull(s.sic22).copy(classCode=classCode,typeCode=typeCode,materialCode=materialCode,
                    signalCode=signalCode.ifBlank {null},conditionCode=conditionCode))
            }
            else -> error("Detalle incompatible")
        }
        if(detail is SicFormDetail.Sic18) {
            var nextIndex=(s.photos.maxOfOrNull {it.photoIndex} ?: 0)+1
            for(photo in s.photos) {
                if(photo.localPath !in paths) dao.deleteLocalPhoto(photo.id,id)
                else dao.updatePhotoMetadata(photo.copy(photoCategory=categories[photo.localPath] ?: photo.photoCategory))
            }
            paths.filter {path->s.photos.none {it.localPath==path}}.forEach {path->
                val index=nextIndex++
                require(categories[path] in EngineeringConditions.culvertPhotos.keys) {"Selecciona categoría fotográfica."}
                // Keep existing names/paths intact. Null names use the same legacy Drive filename fallback.
                dao.insertPhoto(PhotoEntity(UUID.randomUUID().toString(),id,index,path,false,null,null,null,"PENDING",System.currentTimeMillis(),photoCategory=categories[path]))
            }
            dao.refreshRecordPhotoStatus(id)
        }
        dao.markServerPending(id,System.currentTimeMillis())
    }
}
