package com.tuempresa.inventariovial.field

import android.content.Context
import com.tuempresa.inventariovial.road.MatchConfig
import com.tuempresa.inventariovial.tracking.TrackConfig
import com.tuempresa.inventariovial.validation.QualityConfig

data class FieldSettings(val axisThresholdM: Double = 30.0,val trackAccuracyM: Double = 10.0,
    val minTrackDistanceM: Double = 3.0,val qualityAccuracyM: Double = 1.0) {
    fun validate() = listOf(axisThresholdM,trackAccuracyM,minTrackDistanceM,qualityAccuracyM).all { it.isFinite() && it>0 && it<=1000 }
    fun matchConfig() = MatchConfig(maxDistanceToAxisM=axisThresholdM)
    fun trackConfig() = TrackConfig(maxAccuracyM=trackAccuracyM,minDistanceM=minTrackDistanceM)
    fun qualityConfig() = QualityConfig(maxAccuracyM=qualityAccuracyM)
    fun save(context: Context) {
        require(validate()) { "Los umbrales deben ser mayores que cero y hasta 1000 m." }
        context.getSharedPreferences("field_settings",0).edit().putFloat("axis",axisThresholdM.toFloat())
            .putFloat("track_accuracy",trackAccuracyM.toFloat()).putFloat("track_distance",minTrackDistanceM.toFloat())
            .putFloat("quality_accuracy",qualityAccuracyM.toFloat()).apply()
    }
    companion object {
        fun load(context: Context): FieldSettings {
            val p=context.getSharedPreferences("field_settings",0)
            return FieldSettings(p.getFloat("axis",30f).toDouble(),p.getFloat("track_accuracy",10f).toDouble(),
                p.getFloat("track_distance",3f).toDouble(),p.getFloat("quality_accuracy",1f).toDouble())
        }
    }
}
