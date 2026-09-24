package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.road.*

fun testRecord(id: String="id",pr: String="0010",distance: Double=300.0,created: Long=1,status: String="ACTIVE",
    route: String="R",roadbed: String="CD",sic: String="SIC-19") = InventoryRecordEntity(id,sic,"DRENAJE",route,roadbed,pr,distance,
    "0011",100.0,"D",0.0,0.005,null,1.0,"23/09/2026",null,status,"PENDING","PENDING",created,created)
fun testSnapshot(record: InventoryRecordEntity=testRecord(),points:List<TrackPointEntity> = emptyList()) =
    InventorySnapshot(record,emptyList(),points,null,null,Sic19Entity(record.id,"08","2","1","1","1"),null,null,null,null,null,null,null)
fun testAxis(reversed: Boolean=false): RoadReferenceData {
    val points=listOf(RoadPolylinePoint("R","CD","axis",0,0.0,0.0,1000.0),RoadPolylinePoint("R","CD","axis",1,0.0,0.01,2000.0))
    return RoadReferenceData(listOf(RoadRoute("R")),listOf(RoadSegment("R","CD","axis",
        if(reversed) points.reversed().mapIndexed {i,p->p.copy(sequence=i)} else points)),
        listOf(RoadPr("R","CD","0010",0.0,0.0,1000.0),RoadPr("R","CD","0020",0.0,0.005,1500.0)))
}
