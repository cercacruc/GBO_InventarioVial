import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val driveProperties = Properties().apply {
    val config = rootProject.file("drive.local.properties")
    if (config.exists()) config.inputStream().use { load(it) }
}
val driveApiToken = driveProperties.getProperty("DRIVE_API_TOKEN", "")
    .replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
val driveWebAppUrl = driveProperties.getProperty("DRIVE_WEB_APP_URL", "").trim()
    .replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")

val serverProperties = Properties().apply {
    val config = rootProject.file("server.local.properties")
    if (config.exists()) config.inputStream().use { load(it) }
}
fun localString(name: String) = serverProperties.getProperty(name, "")
    .replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
fun localFlag(name: String) = serverProperties.getProperty(name, "false").toBoolean().toString()

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

val scapTemplateAssets = tasks.register<Copy>("scapTemplateAssets") {
    from(rootProject.file("docs/reference/SCAP_PUENTE_AGUA_BLANCA.xlsx")) { rename { "scap_template.xlsx" } }
    from(rootProject.file("tools/scap_field_map.json"))
    into(layout.buildDirectory.dir("generated/scapAssets"))
}
tasks.named("preBuild").configure { dependsOn(scapTemplateAssets) }

android {
    namespace = "com.tuempresa.inventariovial"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        buildConfigField("String", "DRIVE_API_TOKEN", "\"$driveApiToken\"")
        buildConfigField("String", "DRIVE_WEB_APP_URL", "\"$driveWebAppUrl\"")
        buildConfigField("String", "SERVER_BASE_URL", "\"${localString("SERVER_BASE_URL")}\"")
        for (flag in listOf("ACCESS_CONTROL_ENABLED", "AI_ENABLED", "AI_SCAP_ENABLED", "SIC17A_ENABLED", "SIC17B_ENABLED", "SIC18A_ENABLED")) {
            buildConfigField("boolean", flag, localFlag(flag))
        }
        applicationId = "com.tuempresa.inventariovial"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
    sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/scapAssets").get().asFile)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("org.json:json:20250517")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("com.google.android.gms:play-services-location:21.4.0")
    implementation("com.google.android.gms:play-services-auth:22.0.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // =========================================================
    // ROOM
    // =========================================================

    val roomVersion = "2.8.5"

    implementation(
        "androidx.room:room-runtime:$roomVersion"
    )

    implementation(
        "androidx.room:room-ktx:$roomVersion"
    )

    ksp(
        "androidx.room:room-compiler:$roomVersion"
    )


    // =========================================================
    // VIEWMODEL
    // =========================================================

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0"
    )
}
