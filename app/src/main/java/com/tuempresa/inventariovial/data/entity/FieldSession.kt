package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SurveyDirection { INCREASING, DECREASING }

@Entity(tableName = "field_sessions")
data class FieldSession(
    @PrimaryKey val sessionId: String,
    val project: String, val operator: String, val device: String,
    val road: String?, val segment: String?, val roadbed: String?,
    val direction: String = SurveyDirection.INCREASING.name,
    val startTime: Long, val endTime: Long? = null
)
