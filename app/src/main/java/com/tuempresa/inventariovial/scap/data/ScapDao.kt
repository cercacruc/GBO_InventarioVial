package com.tuempresa.inventariovial.scap.data

import androidx.room.*
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

data class ScapElementWithCondition(
    @Embedded val element:ScapElementEntity,
    @Relation(parentColumn="id",entityColumn="scapElementId") val condition:ScapElementConditionEntity?=null
)
data class ScapInspectionSnapshot(
    @Embedded val inspection:ScapInspectionEntity,
    @Relation(parentColumn="id",entityColumn="inspectionId") val values:List<ScapFieldValueEntity> = emptyList(),
    @Relation(parentColumn="id",entityColumn="inspectionId") val spans:List<ScapSpanEntity> = emptyList(),
    @Relation(parentColumn="id",entityColumn="inspectionId") val substructures:List<ScapSubstructureEntity> = emptyList(),
    @Relation(parentColumn="id",entityColumn="inspectionId") val supports:List<ScapSupportEntity> = emptyList(),
    @Relation(parentColumn="id",entityColumn="inspectionId",entity=ScapElementEntity::class) val elements:List<ScapElementWithCondition> = emptyList(),
    @Relation(parentColumn="id",entityColumn="inspectionId") val defects:List<ScapDefectEntity> = emptyList(),
    @Relation(parentColumn="id",entityColumn="inspectionId") val sketches:List<ScapSketchEntity> = emptyList(),
    @Relation(parentColumn="id",entityColumn="scapInspectionId") val photos:List<PhotoEntity> = emptyList(),
    @Relation(parentColumn="id",entityColumn="inspectionId") val profile:List<ScapProfilePointEntity> = emptyList(),
    @Relation(parentColumn="id",entityColumn="inspectionId") val joints:List<ScapJointEntity> = emptyList()
) {
    fun values(owner:String="inspection")=values.filter {it.ownerId==owner}.associate {it.key to it.value}
}

@Dao
interface ScapDao {
    @Query("SELECT * FROM scap_inspections ORDER BY updatedAt DESC") fun observeInspections():Flow<List<ScapInspectionEntity>>
    @Transaction @Query("SELECT * FROM scap_inspections WHERE id=:id") fun observe(id:String):Flow<ScapInspectionSnapshot?>
    @Transaction @Query("SELECT * FROM scap_inspections WHERE id=:id") suspend fun snapshot(id:String):ScapInspectionSnapshot?
    @Query("SELECT * FROM scap_inspections WHERE id=:id") suspend fun inspection(id:String):ScapInspectionEntity?
    @Query("SELECT * FROM scap_inspections WHERE roadRecordId=:recordId") suspend fun forRoadRecord(recordId:String):ScapInspectionEntity?
    @Insert suspend fun insertInspection(value:ScapInspectionEntity)
    @Update suspend fun updateInspection(value:ScapInspectionEntity)
    @Upsert suspend fun putValue(value:ScapFieldValueEntity)
    @Upsert suspend fun putSpan(value:ScapSpanEntity)
    @Upsert suspend fun putSubstructure(value:ScapSubstructureEntity)
    @Upsert suspend fun putJoint(value:ScapJointEntity)
    @Query("DELETE FROM scap_joints WHERE id=:id AND inspectionId=:inspectionId") suspend fun deleteJoint(inspectionId:String,id:String)
    @Query("DELETE FROM photos WHERE id=:id AND scapInspectionId=:inspectionId") suspend fun deletePhoto(inspectionId:String,id:String)
    @Upsert suspend fun putSupport(value:ScapSupportEntity)
    @Upsert suspend fun putElement(value:ScapElementEntity)
    @Upsert suspend fun putCondition(value:ScapElementConditionEntity)
    @Upsert suspend fun putDefect(value:ScapDefectEntity)
    @Insert suspend fun insertSketch(value:ScapSketchEntity)
    @Upsert suspend fun putProfilePoint(value:ScapProfilePointEntity)
    @Query("DELETE FROM scap_values WHERE inspectionId=:inspectionId AND ownerId=:owner") suspend fun deleteValues(inspectionId:String,owner:String)
    @Query("DELETE FROM scap_spans WHERE id=:id AND inspectionId=:inspectionId") suspend fun deleteSpan(inspectionId:String,id:String)
    @Query("DELETE FROM scap_substructures WHERE id=:id AND inspectionId=:inspectionId") suspend fun deleteSubstructure(inspectionId:String,id:String)
    @Query("DELETE FROM scap_supports WHERE id=:id AND inspectionId=:inspectionId") suspend fun deleteSupport(inspectionId:String,id:String)
    @Query("DELETE FROM scap_profile_points WHERE id=:id AND inspectionId=:inspectionId") suspend fun deleteProfilePoint(inspectionId:String,id:String)
    @Query("DELETE FROM scap_defects WHERE id=:id AND inspectionId=:inspectionId") suspend fun deleteDefect(inspectionId:String,id:String)
    @Query("DELETE FROM scap_sketches WHERE id=:id AND inspectionId=:inspectionId") suspend fun deleteSketch(inspectionId:String,id:String)
    @Update suspend fun updatePhoto(photo:PhotoEntity)
    @Query("UPDATE scap_inspections SET status='IN_PROGRESS',syncStatus='PENDING',updatedAt=MAX(updatedAt+1,:now) WHERE id=:id")
    suspend fun touch(id:String,now:Long)
}
