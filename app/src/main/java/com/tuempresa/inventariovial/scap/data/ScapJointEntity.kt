package com.tuempresa.inventariovial.scap.data

import androidx.room.*

@Entity(tableName="scap_joints", primaryKeys=["id"],
    foreignKeys=[ForeignKey(entity=ScapInspectionEntity::class,parentColumns=["id"],childColumns=["inspectionId"],onDelete=ForeignKey.CASCADE)],
    indices=[Index(value=["inspectionId","jointIndex"],unique=true)])
data class ScapJointEntity(val id:String,val inspectionId:String,val jointIndex:Int,val type:String,val material:String)
