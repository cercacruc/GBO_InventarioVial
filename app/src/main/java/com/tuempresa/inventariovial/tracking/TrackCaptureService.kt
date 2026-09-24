package com.tuempresa.inventariovial.tracking

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.tuempresa.inventariovial.MainActivity
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.TrackPointEntity
import com.tuempresa.inventariovial.field.FieldSettings
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.location.TabletLocationProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.coroutines.resume

/** Foreground location service: Android keeps a visible notification while field tracking is active. */
class TrackCaptureService : Service() {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private var job: Job? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?,flags: Int,startId: Int): Int {
        if(intent?.action=="STOP") { stopSelf();return START_NOT_STICKY }
        val recordId=intent?.getStringExtra("recordId") ?: return START_NOT_STICKY
        if(job?.isActive==true) return START_NOT_STICKY
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("field_track","Recorrido GNSS",NotificationManager.IMPORTANCE_LOW))
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop=PendingIntent.getService(this,1,Intent(this,TrackCaptureService::class.java).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification=Notification.Builder(this,"field_track").setContentTitle("Recorrido GNSS activo")
            .setContentText("Guardando puntos localmente").setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(open).addAction(Notification.Action.Builder(null,"Finalizar",stop).build()).setOngoing(true).build()
        try {
            if(Build.VERSION.SDK_INT>=29) startForeground(72,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION) else startForeground(72,notification)
        } catch(error: Exception) { _message.value=error.message;stopSelf();return START_NOT_STICKY }
        _activeRecordId.value=recordId
        job=scope.launch {
            try {
                val dao=InventoryDatabase.getInstance(applicationContext).inventoryDao()
                check(dao.recordById(recordId)?.status=="DRAFT") { "El recorrido requiere un borrador." }
                var last=dao.track(recordId).lastOrNull()
                var resumed=last!=null
                val provider=TabletLocationProvider(applicationContext)
                val config=FieldSettings.load(applicationContext).trackConfig()
                while(isActive) {
                    val fix=withTimeoutOrNull(20_000) {
                        suspendCancellableCoroutine<GeoLocation?> { continuation ->
                            provider.getCurrentLocation({ if(continuation.isActive) continuation.resume(it) },{
                                _message.value=it; if(continuation.isActive) continuation.resume(null)
                            })
                        }
                    }
                    if(fix!=null && TrackFilter.accepts(last,fix,System.currentTimeMillis(),config)) {
                        // A sequence gap records a pause/restart without adding an invented connecting path.
                        val increment=if(resumed || last?.let { fix.timestamp-it.timestamp>60_000 }==true) 2 else 1
                        val point=TrackPointEntity(UUID.randomUUID().toString(),recordId,(last?.sequence ?: -1)+increment,
                            fix.latitude,fix.longitude,fix.altitude,fix.horizontalAccuracy.toDouble(),fix.timestamp)
                        if(!dao.insertDraftTrackPoint(point)) break
                        last=point;resumed=false;_message.value="Punto GNSS guardado."
                    } else if(fix!=null) _message.value="Muestra omitida por precisión, ruido o tiempo."
                    delay(config.sampleIntervalMs)
                }
            } catch(cancelled: CancellationException) { throw cancelled }
            catch(error: Exception) { _message.value=error.message ?: "Error capturando recorrido." }
            finally { stopSelf() }
        }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        val current=job
        current?.invokeOnCompletion { _activeRecordId.value=null }
        current?.cancel();scope.cancel()
        if(current==null) _activeRecordId.value=null
        super.onDestroy()
    }
    companion object {
        private val _activeRecordId=MutableStateFlow<String?>(null)
        val activeRecordId=_activeRecordId.asStateFlow()
        private val _message=MutableStateFlow<String?>(null)
        val message=_message.asStateFlow()
        fun start(context: Context,recordId: String) { ContextCompat.startForegroundService(context,Intent(context,TrackCaptureService::class.java).putExtra("recordId",recordId)) }
        fun stop(context: Context) { context.stopService(Intent(context,TrackCaptureService::class.java)) }
    }
}
