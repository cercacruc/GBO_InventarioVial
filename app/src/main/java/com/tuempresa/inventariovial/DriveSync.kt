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
    photoPath: String
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
                "photoPath",
                photoPath
            )
            .build()


    val workRequest =
        OneTimeWorkRequestBuilder<
                DriveUploadWorker
                >()
            .setConstraints(
                constraints
            )
            .setInputData(
                inputData
            )
            .build()


    val fileName =
        java.io.File(
            photoPath
        ).name


    WorkManager
        .getInstance(context)
        .enqueueUniqueWork(
            "drive-upload-$fileName",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
}