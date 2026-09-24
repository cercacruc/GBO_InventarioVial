package com.tuempresa.inventariovial.field

import android.content.Context
import androidx.room.withTransaction
import com.tuempresa.inventariovial.access.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.gis.LocalKmlExport
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.server.scheduleServerSync
import com.tuempresa.inventariovial.tracking.TrackCaptureService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FieldController(private val context: Context,private val database: InventoryDatabase,private val scope: CoroutineScope) {
    val dao=database.inventoryDao()
    private val _reference=MutableStateFlow(RoadReferenceData())
    val reference=_reference.asStateFlow()
    private val _referenceError=MutableStateFlow<String?>(null)
    val referenceError=_referenceError.asStateFlow()
    private val _settings=MutableStateFlow(FieldSettings.load(context))
    val settings=_settings.asStateFlow()
    private val _access=MutableStateFlow<DeviceAccessState?>(null)
    val access=_access.asStateFlow()
    val session=dao.observeSession().stateIn(scope,SharingStarted.Eagerly,null)
    val drafts=dao.observeDrafts().stateIn(scope,SharingStarted.WhileSubscribed(5000),emptyList())
    val kml=LocalKmlExport(context,database)
    init { reloadReference();checkAccess();scheduleServerSync(context) }

    fun checkAccess() { scope.launch {
        _access.value=try { DeviceAccessManager(context).check() } catch(error: Exception) {
            DeviceAccessState(false,"",error.message ?: "No se pudo verificar el dispositivo.")
        }
    } }
    fun reloadReference() { scope.launch {
        try { _reference.value=RoadReferenceRepository(context).loadFromAssets();_referenceError.value=null }
        catch(error: Exception) { _reference.value=RoadReferenceData();_referenceError.value=error.message ?: "Cartografía inválida." }
    } }
    fun saveSettings(value: FieldSettings) { value.save(context);_settings.value=value }
    fun startSession(project: String,operator: String,road: String,segment: String,roadbed: String,direction: String,onSuccess: ()->Unit,onError: (String)->Unit) {
        scope.launch {
            try {
                require(project.isNotBlank() && operator.isNotBlank()) { "Completa proyecto y operador." }
                require(direction in SurveyDirection.entries.map { it.name })
                check(TrackCaptureService.activeRecordId.value==null) { "Finaliza el recorrido antes de cambiar de sesión." }
                database.withTransaction {
                    val now=System.currentTimeMillis()
                    dao.closeSessions(now)
                    dao.insertSession(FieldSession(UUID.randomUUID().toString(),project.trim(),operator.trim(),
                        DeviceAccessManager(context).fingerprint(),road.trim().uppercase().ifEmpty { null },segment.trim().ifEmpty { null },
                        roadbed.trim().uppercase().ifEmpty { null },direction,now))
                }
                scheduleServerSync(context)
                onSuccess()
            } catch(error:Exception) { onError(error.message ?: "No se pudo iniciar la sesión.") }
        }
    }
    fun endSession(onError: (String)->Unit) { scope.launch {
        try {
            check(TrackCaptureService.activeRecordId.value==null) { "Finaliza el recorrido antes de cerrar la sesión." }
            dao.closeSessions(System.currentTimeMillis())
            scheduleServerSync(context)
        } catch(error:Exception) { onError(error.message ?: "No se pudo finalizar la sesión.") }
    } }
    fun startTrack(existingId: String?,sicCode: String,asset: String,route: String,roadbed: String,pr: String,distance: String,
        side: String?,fix: GeoLocation,onStarted: (String)->Unit,onError: (String)->Unit,
        segment: String="",direction: String="INCREASING") {
        scope.launch {
            try {
                require(TrackCaptureService.activeRecordId.value==null) { "Ya hay un recorrido activo." }
                require(segment in SurveyPreferences.segments && route.isNotBlank() && roadbed.isNotBlank()) { "Completa tramo, ruta y calzada antes de iniciar el recorrido." }
                require(SurveyOrder.chainage(pr,distance)!=null) { "Completa una progresiva inicial válida." }
                val id=existingId ?: UUID.randomUUID().toString()
                if(existingId==null) {
                    val now=System.currentTimeMillis()
                    dao.insertRecord(InventoryRecordEntity(id,sicCode,asset,route.trim().uppercase(),roadbed.trim().uppercase(),pr,
                        distance.replace(',','.').toDoubleOrNull()?.takeIf { it.isFinite() && it>=0 } ?: 0.0,
                        null,null,side,fix.latitude,fix.longitude,fix.altitude,fix.horizontalAccuracy.toDouble(),
                        SimpleDateFormat("dd/MM/yyyy",Locale.US).format(Date(now)),null,"DRAFT","PENDING","PENDING",now,now,
                        sessionId=session.value?.sessionId,gpsTimestamp=fix.timestamp,segment=segment,surveyDirection=direction))
                } else check(dao.recordById(id)?.status=="DRAFT")
                onStarted(id)
                TrackCaptureService.start(context,id)
            } catch(error:Exception) { onError(error.message ?: "No se pudo iniciar el recorrido.") }
        }
    }
    fun stopTrack() = TrackCaptureService.stop(context)
}
