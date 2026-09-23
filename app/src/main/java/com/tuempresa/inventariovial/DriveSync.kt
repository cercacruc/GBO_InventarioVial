package com.tuempresa.inventariovial

import android.content.Context

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager


fun scheduleDriveUpload(
    context: Context,
    photoPath: String,
    driveFileName: String,
    routeCode: String,
    sibCode: String,
    photoId: String? = null,
    recordId: String? = null
) {

    val constraints =
        Constraints.Builder()
            .setRequiredNetworkType(
                NetworkType.CONNECTED
            )
            .build()


    val inputData =
        Data.Builder()
            .putString(
                "photoId",
                photoId
            )
            .putString(
                "recordId",
                recordId
            )
            .putString(
                "photoPath",
                photoPath
            )
            .putString(
                "driveFileName",
                driveFileName
            )
            .putString(
                "routeCode",
                routeCode
            )
            .putString(
                "sibCode",
                sibCode
            )
            .build()


    val workRequest =
        OneTimeWorkRequestBuilder<DriveUploadWorker>()
            .setConstraints(
                constraints
            )
            .setInputData(
                inputData
            )
            .build()


    WorkManager
        .getInstance(context)
        .enqueueUniqueWork(
            "drive-upload-${photoId ?: photoPath}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
}
