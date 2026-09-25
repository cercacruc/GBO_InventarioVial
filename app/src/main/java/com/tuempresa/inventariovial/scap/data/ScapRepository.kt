package com.tuempresa.inventariovial.scap.data

import androidx.room.withTransaction
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.road.GeoMath
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.domain.*
import java.util.UUID
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ScapRepository(private val db:InventoryDatabase,val catalog:ScapCatalog) {
    val dao=db.scapDao()
    private fun uuid()=UUID.randomUUID().toString()
    suspend fun create(operator:String,device:String,sessionId:String?):String=db.withTransaction {
        val now=System.currentTimeMillis();val id=uuid();val roadId=uuid()
        // Technical draft: absent coordinates are not exported as an observed (0,0) location.
        db.inventoryDao().insertRecord(InventoryRecordEntity(roadId,"SCAP","PUENTE","","","",0.0,null,null,null,
            0.0,0.0,null,null,LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),null,"DRAFT","PENDING","PENDING",now,now,sessionId=sessionId))
        dao.insertInspection(ScapInspectionEntity(id,roadId,"","",now,now,operator,device,"DRAFT","PENDING",null,null,null,null,"MANUAL"))
        id
    }
    private suspend fun editable(id:String):ScapInspectionSnapshot {
        val s=dao.snapshot(id) ?: error("Inspección inexistente.")
        check(s.inspection.status!="COMPLETE") {"Reabre la inspección antes de editar."}
        return s
    }
    suspend fun setField(id:String,owner:String,key:String,value:String,source:String="MANUAL")=db.withTransaction {
        val s=editable(id)
        val ownerType=ScapValidation.ownerType(s,owner)
        require(ownerType!=null || s.elements.any {it.element.id==owner}) {"Sección inexistente."}
        dao.putValue(ScapFieldValueEntity(id,owner,key,value,source))
        val values=s.values(owner)+(key to value)
        if(owner=="inspection") {
            if(key in setOf("bridgeName","bridgeCode","createdBy")) dao.updateInspection(s.inspection.copy(
                bridgeName=values["bridgeName"] ?: s.inspection.bridgeName,bridgeCode=values["bridgeCode"] ?: s.inspection.bridgeCode,
                createdBy=if(key=="createdBy") value else s.inspection.createdBy))
            val road=db.inventoryDao().recordById(s.inspection.roadRecordId)!!
            val chainage=ScapNumbers.chainage(values["progressive"])
            db.inventoryDao().updateRecord(road.copy(routeCode=values["route"].orEmpty(),segment=values["segment"],
                startPrCode=chainage?.let {(it/1000).toInt().toString().padStart(4,'0')}.orEmpty(),
                startDistanceM=chainage?.rem(1000) ?: 0.0,updatedAt=maxOf(System.currentTimeMillis(),road.updatedAt+1)))
        }
        s.spans.find {it.id==owner}?.let {span->
            if(key=="category") {dao.putValue(ScapFieldValueEntity(id,owner,"type","",source))}
            dao.putSpan(span.copy(lengthM=ScapNumbers.decimal(values["lengthM"]),category=values["category"].orEmpty(),
                type=if(key=="category") "" else values["type"].orEmpty(),secondaryCharacteristic=values["secondaryCharacteristic"].orEmpty(),
                edgeCondition=values["edgeCondition"].orEmpty(),predominantMaterial=values["predominantMaterial"].orEmpty()))
        }
        s.substructures.find {it.id==owner}?.let {e->dao.putSubstructure(e.copy(elevationType=values["elevationType"].orEmpty(),
            elevationMaterial=values["elevationMaterial"].orEmpty(),foundationType=values["foundationType"].orEmpty(),foundationMaterial=values["foundationMaterial"].orEmpty(),soil=values["soil"].orEmpty()))}
        s.supports.find {it.id==owner}?.let {e->dao.putSupport(e.copy(type=values["type"].orEmpty(),material=values["material"].orEmpty(),location=values["location"].orEmpty(),number=values["number"]?.toIntOrNull()))}
        s.profile.find {it.id==owner}?.let {e->dao.putProfilePoint(e.copy(distanceM=ScapNumbers.decimal(values["distanceM"]),downstreamM=ScapNumbers.decimal(values["downstreamM"]),upstreamM=ScapNumbers.decimal(values["upstreamM"]),axisM=ScapNumbers.decimal(values["axisM"])))}
        s.elements.find {it.element.id==owner}?.let {entry->
            if(key=="quantity") dao.putElement(entry.element.copy(quantity=ScapNumbers.decimal(value)))
            else if(key.matches(Regex("percent[0-5]"))) {
                val c=entry.condition
                val p=(0..5).map {i->ScapNumbers.decimal(values["percent$i"])}
                dao.putCondition(ScapElementConditionEntity(c?.id ?: uuid(),owner,p[0],p[1],p[2],p[3],p[4],p[5]))
            }
        }
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun addRow(id:String,kind:String)=db.withTransaction {
        val s=editable(id);val rowId=uuid()
        when(kind) {
            "SPAN" -> dao.putSpan(ScapSpanEntity(rowId,id,(s.spans.maxOfOrNull {it.spanIndex} ?: 0)+1,null,"","","","",""))
            "BEARING" -> dao.putSupport(ScapSupportEntity(rowId,id,(s.supports.maxOfOrNull {it.supportIndex} ?: 0)+1,"","","",null))
            "PROFILE" -> dao.putProfilePoint(ScapProfilePointEntity(rowId,id,(s.profile.maxOfOrNull {it.pointIndex} ?: 0)+1,null,null,null,null))
            else -> {
                require(kind in setOf("LEFT_ABUTMENT","RIGHT_ABUTMENT","PIER","LEFT_ANCHOR","RIGHT_ANCHOR"))
                if(kind!="PIER") require(s.substructures.none {it.kind==kind}) {"Este elemento ya existe."}
                dao.putSubstructure(ScapSubstructureEntity(rowId,id,kind,(s.substructures.filter {it.kind==kind}.maxOfOrNull {it.elementIndex} ?: 0)+1,"","","","",""))
            }
        }
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun removeRow(id:String,rowId:String)=db.withTransaction {
        editable(id);dao.deleteValues(id,rowId);dao.deleteSpan(id,rowId);dao.deleteSubstructure(id,rowId)
        dao.deleteSupport(id,rowId);dao.deleteProfilePoint(id,rowId);dao.touch(id,System.currentTimeMillis())
    }
    suspend fun selectElement(id:String,code:String,present:Boolean)=db.withTransaction {
        val s=editable(id);val item=catalog.element(code) ?: error("Código SCAP desconocido.")
        val old=s.elements.find {it.element.elementCode==code}?.element
        dao.putElement(old?.copy(isPresent=present) ?: ScapElementEntity(uuid(),id,code,item.name,null,item.unit,item.importanceFactor,item.group,present))
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun setLocation(id:String,fix:GeoLocation)=db.withTransaction {
        val s=editable(id);require(GeoMath.validCoordinate(fix.latitude,fix.longitude))
        dao.updateInspection(s.inspection.copy(latitude=fix.latitude,longitude=fix.longitude,gpsAccuracyM=fix.horizontalAccuracy.toDouble().takeIf {it.isFinite()},gpsTimestamp=fix.timestamp,locationSource=fix.provider.name))
        val r=db.inventoryDao().recordById(s.inspection.roadRecordId)!!
        db.inventoryDao().updateRecord(r.copy(latitude=fix.latitude,longitude=fix.longitude,gpsAccuracyM=fix.horizontalAccuracy.toDouble().takeIf {it.isFinite()},gpsTimestamp=fix.timestamp,gnssProvider=fix.provider.name))
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun saveDefect(id:String,defect:ScapDefectEntity)=db.withTransaction {
        val s=editable(id);require(defect.inspectionId==id)
        require(defect.elementCode==null || catalog.element(defect.elementCode)!=null)
        require(defect.photoId==null || s.photos.any {it.id==defect.photoId})
        dao.putDefect(defect);dao.touch(id,System.currentTimeMillis())
    }
    suspend fun removeDefect(id:String,defectId:String)=db.withTransaction {editable(id);dao.deleteDefect(id,defectId);dao.touch(id,System.currentTimeMillis())}
    suspend fun setDefectField(id:String,defectId:String,key:String,value:String)=db.withTransaction {
        val s=editable(id);val d=s.defects.single {it.id==defectId}
        val updated=when(key) {
            "description"->d.copy(description=value)
            "locationDescription"->d.copy(locationDescription=value)
            "elementCode"->d.copy(elementCode=value.ifBlank{null})
            "photoId"->d.copy(photoId=value.ifBlank{null})
            "validatedByUser"->d.copy(validatedByUser=value=="true")
            else->error("Campo de defecto desconocido")
        }
        saveDefect(id,updated)
    }
    suspend fun setPhotoMetadata(id:String,photoId:String,category:String,code:String?)=db.withTransaction {
        val s=editable(id);require(category in PHOTO_CATEGORIES);require(code==null || catalog.element(code)!=null)
        val p=s.photos.single{it.id==photoId}
        dao.updatePhoto(p.copy(photoCategory=category,scapElementCode=code));dao.touch(id,System.currentTimeMillis())
    }
    suspend fun removeSketch(id:String,sketchId:String)=db.withTransaction {editable(id);dao.deleteSketch(id,sketchId);dao.touch(id,System.currentTimeMillis())}
    suspend fun addPhoto(id:String,path:String,category:String,code:String?)=db.withTransaction {
        val s=editable(id);require(category in PHOTO_CATEGORIES);require(code==null || catalog.element(code)!=null)
        val index=(s.photos.maxOfOrNull {it.photoIndex} ?: 0)+1
        db.inventoryDao().insertPhoto(PhotoEntity(uuid(),s.inspection.roadRecordId,index,path,index==1,null,null,null,"PENDING",System.currentTimeMillis(),scapInspectionId=id,scapElementCode=code,photoCategory=category))
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun addSketch(id:String,type:String,path:String)=db.withTransaction {
        editable(id);require(type in SKETCH_TYPES);dao.insertSketch(ScapSketchEntity(uuid(),id,type,path));dao.touch(id,System.currentTimeMillis())
    }
    suspend fun reopen(id:String)=db.withTransaction {requireNotNull(dao.inspection(id));dao.touch(id,System.currentTimeMillis())}
    suspend fun complete(id:String)=db.withTransaction {
        val s=editable(id);val review=ScapValidation.review(s,catalog)
        require(review.errors.isEmpty()) {review.errors.joinToString("\n")}
        dao.updateInspection(s.inspection.copy(status="COMPLETE",syncStatus="PENDING",updatedAt=maxOf(System.currentTimeMillis(),s.inspection.updatedAt+1)))
    }
    companion object {
        val PHOTO_CATEGORIES=listOf("GENERAL","UPSTREAM","DOWNSTREAM","TRANSVERSE","ELEMENT","DEFECT","ACCESS","CHANNEL","OTHER")
        val SKETCH_TYPES=listOf("ELEVATION","PLAN","CROSS_SECTION")
    }
}
