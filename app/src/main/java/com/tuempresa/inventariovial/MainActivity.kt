package com.tuempresa.inventariovial

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.tuempresa.inventariovial.camera.createPhotoFile
import com.tuempresa.inventariovial.camera.loadCorrectlyOrientedBitmap
import com.tuempresa.inventariovial.location.getCurrentGpsLocation
import com.tuempresa.inventariovial.model.RoadAssetType
import com.tuempresa.inventariovial.model.SignalizationType
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            InventarioVialTheme {
                InventarioVialApp()
            }
        }
    }
}




// =========================================================
// APP PRINCIPAL
// =========================================================

@Composable
fun InventarioVialApp() {

    var selectedAsset by remember {
        mutableStateOf<RoadAssetType?>(null)
    }

    var selectedSignalization by remember {
        mutableStateOf<SignalizationType?>(null)
    }

    var recordsToday by remember {
        mutableIntStateOf(0)
    }

    var pendingSync by remember {
        mutableIntStateOf(0)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            when {

                selectedAsset == null -> {

                    HomeScreen(
                        recordsToday = recordsToday,
                        pendingSync = pendingSync,
                        onAssetSelected = {
                            selectedAsset = it
                        }
                    )
                }


                selectedAsset == RoadAssetType.SIGNALIZATION &&
                        selectedSignalization == null -> {

                    SignalizationMenuScreen(
                        onBack = {
                            selectedAsset = null
                        },
                        onSelected = {
                            selectedSignalization = it
                        }
                    )
                }


                selectedAsset == RoadAssetType.SIGNALIZATION &&
                        selectedSignalization != null -> {

                    SignalizationFormScreen(
                        signalizationType = selectedSignalization!!,
                        onBack = {
                            selectedSignalization = null
                        },
                        onSave = {
                            recordsToday++
                            pendingSync++
                            selectedSignalization = null
                            selectedAsset = null
                        }
                    )
                }


                else -> {

                    AssetFormScreen(
                        assetType = selectedAsset!!,
                        onBack = {
                            selectedAsset = null
                        },
                        onSave = {
                            recordsToday++
                            pendingSync++
                            selectedAsset = null
                        }
                    )
                }
            }
        }
    }
}


// =========================================================
// HOME
// =========================================================

@Composable
fun HomeScreen(
    recordsToday: Int,
    pendingSync: Int,
    onAssetSelected: (RoadAssetType) -> Unit
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),

        contentPadding = PaddingValues(
            top = 24.dp,
            bottom = 32.dp
        ),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        item {

            Text(
                text = "Inventario Vial",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Levantamiento de información en campo",
                modifier = Modifier.padding(top = 4.dp),
                style =
                    MaterialTheme.typography.bodyLarge
            )
        }

        item {
            ProjectCard()
        }

        item {

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                StatusCard(
                    title = "Registros",
                    value =
                        recordsToday.toString(),
                    modifier =
                        Modifier.weight(1f)
                )

                Spacer(
                    modifier =
                        Modifier.width(16.dp)
                )

                StatusCard(
                    title = "Pendientes",
                    value =
                        pendingSync.toString(),
                    modifier =
                        Modifier.weight(1f)
                )
            }
        }

        item {

            Text(
                text = "Nuevo registro",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier.padding(top = 12.dp)
            )

            Text(
                text =
                    "Seleccione el elemento encontrado en carretera.",
                style =
                    MaterialTheme.typography.bodyMedium
            )
        }

        items(
            items =
                RoadAssetType.values().toList()
        ) { asset ->

            AssetButton(
                asset = asset,
                onClick = {
                    onAssetSelected(asset)
                }
            )
        }

        item {

            Divider(
                modifier =
                    Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = "Estado del sistema",
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text("● Captura offline habilitada")
            Text("● Fotografías guardadas localmente")
            Text("● GPS disponible durante cada registro")
            Text(
                "● Pendientes de sincronización: $pendingSync"
            )
        }
    }
}


// =========================================================
// MENÚ DE SEÑALIZACIÓN
// =========================================================

@Composable
fun SignalizationMenuScreen(
    onBack: () -> Unit,
    onSelected: (SignalizationType) -> Unit
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),

        contentPadding = PaddingValues(
            top = 24.dp,
            bottom = 40.dp
        ),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        item {

            OutlinedButton(
                onClick = onBack
            ) {
                Text("← Volver")
            }
        }

        item {

            Text(
                text = "Señalización y seguridad",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    "Seleccione el tipo de elemento que encontró en la carretera.",
                style =
                    MaterialTheme.typography.bodyLarge
            )
        }

        items(
            items =
                SignalizationType.values().toList()
        ) { item ->

            Button(
                onClick = {
                    onSelected(item)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {

                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = item.title,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text =
                            "${item.sicCode} · ${item.subtitle}",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}


// =========================================================
// TARJETAS
// =========================================================

@Composable
fun ProjectCard() {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(20.dp)
        ) {

            Text(
                text = "Jornada activa",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text("Proyecto: Sin seleccionar")
            Text("Ruta: Se registra por elemento")
            Text("Tramo: Sin seleccionar")
            Text("Ingeniero: Sin registrar")
        }
    }
}


@Composable
fun StatusCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 3.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(20.dp)
        ) {

            Text(
                text = value,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Text(text = title)
        }
    }
}


@Composable
fun AssetButton(
    asset: RoadAssetType,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,

        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
    ) {

        Column(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text = asset.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    "${asset.sicCode} · ${asset.subtitle}",
                fontSize = 14.sp
            )
        }
    }
}


// =========================================================
// FORMULARIO DE SEÑALIZACIÓN
// =========================================================

@Composable
fun SignalizationFormScreen(
    signalizationType: SignalizationType,
    onBack: () -> Unit,
    onSave: () -> Unit
) {

    val context = LocalContext.current

    // Comunes SIC-21 / SIC-22
    var route by remember {
        mutableStateOf("")
    }

    var roadbed by remember {
        mutableStateOf("")
    }

    var startPr by remember {
        mutableStateOf("")
    }

    var startDistance by remember {
        mutableStateOf("")
    }

    var endPr by remember {
        mutableStateOf("")
    }

    var endDistance by remember {
        mutableStateOf("")
    }

    var side by remember {
        mutableStateOf("D - Derecho")
    }

    var observations by remember {
        mutableStateOf("")
    }

    val registrationDate = remember {
        SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        ).format(Date())
    }

    // Foto
    var photoPath by remember {
        mutableStateOf<String?>(null)
    }

    var photoCaptured by remember {
        mutableStateOf(false)
    }

    var photoError by remember {
        mutableStateOf<String?>(null)
    }

    // GPS
    var latitude by remember {
        mutableStateOf<Double?>(null)
    }

    var longitude by remember {
        mutableStateOf<Double?>(null)
    }

    var gpsAccuracy by remember {
        mutableStateOf<Float?>(null)
    }

    var locating by remember {
        mutableStateOf(false)
    }

    var locationError by remember {
        mutableStateOf<String?>(null)
    }

    var formError by remember {
        mutableStateOf<String?>(null)
    }


    fun hasLocationPermission(): Boolean {

        val fine =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) ==
                    PackageManager.PERMISSION_GRANTED

        val coarse =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) ==
                    PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }


    fun captureLocation() {

        if (!hasLocationPermission()) return

        locating = true
        locationError = null

        getCurrentGpsLocation(
            context = context,

            onSuccess = {
                    lat,
                    lon,
                    accuracy ->

                latitude = lat
                longitude = lon
                gpsAccuracy = accuracy
                locating = false
            },

            onError = { error ->

                locationError = error
                locating = false
            }
        )
    }


    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestMultiplePermissions()
        ) { permissions ->

            val fineGranted =
                permissions[
                    Manifest.permission
                        .ACCESS_FINE_LOCATION
                ] == true

            val coarseGranted =
                permissions[
                    Manifest.permission
                        .ACCESS_COARSE_LOCATION
                ] == true

            if (
                fineGranted ||
                coarseGranted
            ) {

                captureLocation()

            } else {

                locationError =
                    "Debes permitir el acceso a la ubicación."
            }
        }

    LaunchedEffect(signalizationType) {

        if (hasLocationPermission()) {

            captureLocation()

        } else {

            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val takePictureLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .TakePicture()
        ) { success ->

            photoCaptured = success

            if (success) {

                photoError = null

                if (hasLocationPermission()) {

                    captureLocation()

                } else {

                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission
                                .ACCESS_FINE_LOCATION,

                            Manifest.permission
                                .ACCESS_COARSE_LOCATION
                        )
                    )
                }

            } else {

                photoError =
                    "La fotografía fue cancelada o no pudo guardarse."
            }
        }


    fun openCamera() {

        try {

            val prefix = when (signalizationType) {

                SignalizationType.VERTICAL ->
                    "SENAL_VERTICAL"

                SignalizationType.HORIZONTAL_MARKS ->
                    "MARCA_HORIZONTAL"

                SignalizationType.HORIZONTAL_STUDS ->
                    "TACHA"

                SignalizationType.SAFETY ->
                    "SEGURIDAD_VIAL"
            }

            val photo =
                createPhotoFile(
                    context = context,
                    prefix = prefix
                )

            photoPath =
                photo.file.absolutePath

            photoCaptured = false
            photoError = null

            takePictureLauncher.launch(
                photo.uri
            )

        } catch (error: Exception) {

            photoError =
                error.message
                    ?: "No se pudo abrir la cámara."
        }
    }


    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestPermission()
        ) { granted ->

            if (granted) {

                openCamera()

            } else {

                photoError =
                    "Debes permitir el acceso a la cámara."
            }
        }


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),

        contentPadding =
            PaddingValues(
                top = 24.dp,
                bottom = 40.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(18.dp)
    ) {

        item {

            OutlinedButton(
                onClick = onBack
            ) {
                Text("← Volver")
            }
        }

        item {

            Text(
                text =
                    "Registrar ${signalizationType.title}",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    "${signalizationType.sicCode} · ${signalizationType.subtitle}",
                style =
                    MaterialTheme.typography.bodyLarge
            )
        }

        // FOTO
        item {

            SectionTitle("Fotografía")

            Button(
                onClick = {

                    val permission =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        )

                    if (
                        permission ==
                        PackageManager.PERMISSION_GRANTED
                    ) {

                        openCamera()

                    } else {

                        cameraPermissionLauncher.launch(
                            Manifest.permission.CAMERA
                        )
                    }
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
            ) {

                Text(
                    text =
                        if (photoCaptured)
                            "Tomar otra fotografía"
                        else
                            "Tomar fotografía",
                    fontSize = 18.sp
                )
            }
        }

        if (
            photoCaptured &&
            photoPath != null
        ) {

            item {

                val path = photoPath!!

                val bitmap =
                    remember(
                        path,
                        photoCaptured
                    ) {
                        loadCorrectlyOrientedBitmap(
                            path
                        )
                    }

                if (bitmap != null) {

                    Image(
                        bitmap =
                            bitmap.asImageBitmap(),

                        contentDescription =
                            "Fotografía capturada",

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),

                        contentScale =
                            ContentScale.Fit
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        "✓ Fotografía guardada",
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        File(path).name,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }

        photoError?.let { error ->

            item {
                ErrorText(error)
            }
        }

        // GPS
        item {

            SectionTitle("Ubicación GPS")

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(16.dp)
                ) {

                    Text(
                        text =
                            "Latitud: ${
                                latitude?.let {
                                    String.format(
                                        Locale.US,
                                        "%.8f",
                                        it
                                    )
                                } ?: "Pendiente"
                            }"
                    )

                    Text(
                        text =
                            "Longitud: ${
                                longitude?.let {
                                    String.format(
                                        Locale.US,
                                        "%.8f",
                                        it
                                    )
                                } ?: "Pendiente"
                            }"
                    )

                    Text(
                        text =
                            "Precisión: ${
                                gpsAccuracy?.let {
                                    String.format(
                                        Locale.US,
                                        "± %.1f m",
                                        it
                                    )
                                } ?: "Pendiente"
                            }"
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(
                        text =
                            when {

                                locating -> {
                                    "Obteniendo ubicación GPS..."
                                }

                                latitude != null &&
                                        longitude != null -> {
                                    "✓ Ubicación obtenida"
                                }

                                else -> {
                                    "Esperando ubicación GPS..."
                                }
                            },

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }

        locationError?.let { error ->

            item {
                ErrorText(error)
            }
        }

        item {
            Divider()
        }

        // UBICACIÓN VIAL COMÚN
        item {

            SectionTitle(
                "Datos generales del formato"
            )

            OutlinedTextField(
                value = route,
                onValueChange = {
                    route =
                        it.uppercase()
                },
                label = {
                    Text("Ruta")
                },
                placeholder = {
                    Text("Ej. PE-1N, PE-3S")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = roadbed,
                onValueChange = {
                    roadbed =
                        it.uppercase()
                },
                label = {
                    Text("Calzada")
                },
                placeholder = {
                    Text("Ej. UC, UD, CD, A1")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )
        }

        item {

            SectionTitle("Ubicación inicio")

            OutlinedTextField(
                value = startPr,
                onValueChange = {
                    startPr = it
                },
                label = {
                    Text("Código PR inicio")
                },
                placeholder = {
                    Text("4 dígitos, ej. 0010")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = startDistance,
                onValueChange = {
                    startDistance = it
                },
                label = {
                    Text("Distancia desde PR inicio (m)")
                },
                placeholder = {
                    Text("Ej. 125.50")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )
        }

        item {

            SectionTitle("Ubicación fin")

            OutlinedTextField(
                value = endPr,
                onValueChange = {
                    endPr = it
                },
                label = {
                    Text("Código PR fin")
                },
                placeholder = {
                    Text("4 dígitos, ej. 0010")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = endDistance,
                onValueChange = {
                    endDistance = it
                },
                label = {
                    Text("Distancia desde PR fin (m)")
                },
                placeholder = {
                    Text("Ej. 180.25")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )
        }

        item {

            Text(
                text = "Lado",
                fontWeight =
                    FontWeight.Bold
            )

            ChoiceSelector(
                options = listOf(
                    "D - Derecho",
                    "I - Izquierdo",
                    "S - Sin objeto"
                ),
                selected = side,
                onSelected = {
                    side = it
                }
            )
        }

        item {
            Divider()
        }

        // CAMPOS SEGÚN TIPO
        item {

            when (signalizationType) {

                SignalizationType.VERTICAL ->
                    VerticalSignalFields()

                SignalizationType.HORIZONTAL_MARKS ->
                    HorizontalMarksFields()

                SignalizationType.HORIZONTAL_STUDS ->
                    HorizontalStudsFields()

                SignalizationType.SAFETY ->
                    SafetyElementFields()
            }
        }

        item {
            Divider()
        }

        item {

            SectionTitle("Registro")

            Text(
                text =
                    "Fecha: $registrationDate"
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = observations,
                onValueChange = {
                    observations = it
                },
                label = {
                    Text(
                        "Observaciones adicionales"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
        }

        formError?.let { error ->

            item {
                ErrorText(error)
            }
        }

        item {

            Button(
                onClick = {

                    when {

                        !photoCaptured -> {
                            formError =
                                "Debes tomar una fotografía."
                        }

                        latitude == null ||
                                longitude == null -> {
                            formError =
                                "Debes obtener la ubicación GPS."
                        }

                        route.trim().isEmpty() -> {
                            formError =
                                "Debes ingresar el código de ruta."
                        }

                        roadbed.trim().isEmpty() -> {
                            formError =
                                "Debes ingresar el código de calzada."
                        }

                        startPr.trim().isEmpty() -> {
                            formError =
                                "Debes ingresar el PR de inicio."
                        }

                        else -> {
                            formError = null

                            // Programa la subida de la foto a Drive.
                            // Si no hay Internet, WorkManager esperará
                            // hasta que vuelva a haber conexión.
                            photoPath?.let { path ->

                                scheduleDriveUpload(
                                    context = context,
                                    photoPath = path
                                )
                            }

                            onSave()
                        }
                    }
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {

                Text(
                    text =
                        "Guardar registro",
                    fontSize = 20.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}


// =========================================================
// SIC-22 · SEÑALIZACIÓN VERTICAL
// =========================================================

@Composable
fun VerticalSignalFields() {

    var type by remember {
        mutableStateOf("2 - Preventivo")
    }

    var material by remember {
        mutableStateOf("2 - Acero")
    }

    var signalCode by remember {
        mutableStateOf("")
    }

    var kilometerPostNumber by remember {
        mutableStateOf("")
    }

    var condition by remember {
        mutableStateOf("1 - Buena")
    }

    // Requerimientos extra del cliente
    var signWidth by remember {
        mutableStateOf("")
    }

    var signHeight by remember {
        mutableStateOf("")
    }

    var lowerEdgeHeight by remember {
        mutableStateOf("")
    }

    SectionTitle(
        "SIC-22 · Señalización vertical"
    )

    Text(
        text =
            "Clase: 20 - Señalización Vertical",
        fontWeight =
            FontWeight.Bold
    )

    Spacer(
        modifier =
            Modifier.height(8.dp)
    )

    Text(
        text = "Tipo",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Reglamento",
            "2 - Preventivo",
            "3 - Informativo",
            "4 - Poste Kilométrico",
            "5 - Semáforos",
            "6 - Postes SOS"
        ),
        selected = type,
        onSelected = {
            type = it
        }
    )

    Spacer(
        modifier =
            Modifier.height(8.dp)
    )

    Text(
        text = "Material",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Fibra de vidrio",
            "2 - Acero",
            "3 - Concreto",
            "4 - Madera",
            "5 - Otro"
        ),
        selected = material,
        onSelected = {
            material = it
        }
    )

    if (
        type.startsWith("1") ||
        type.startsWith("2") ||
        type.startsWith("3")
    ) {

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        OutlinedTextField(
            value = signalCode,
            onValueChange = {
                signalCode =
                    it.uppercase()
            },
            label = {
                Text(
                    "Código de señal"
                )
            },
            placeholder = {
                Text(
                    "Ej. P-2B, R30, I5"
                )
            },
            supportingText = {
                Text(
                    "Por ahora es texto libre para admitir cualquier código del catálogo."
                )
            },
            modifier =
                Modifier.fillMaxWidth()
        )
    }

    if (type.startsWith("4")) {

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        OutlinedTextField(
            value =
                kilometerPostNumber,
            onValueChange = {
                kilometerPostNumber =
                    it
            },
            label = {
                Text(
                    "Número del poste kilométrico"
                )
            },
            modifier =
                Modifier.fillMaxWidth()
        )
    }

    Spacer(
        modifier =
            Modifier.height(12.dp)
    )

    Text(
        text = "Condición",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Buena · no tiene problema",
            "2 - Regular · dañado pero se puede leer",
            "3 - Mala · no se puede leer o ausente"
        ),
        selected = condition,
        onSelected = {
            condition = it
        }
    )

    Spacer(
        modifier =
            Modifier.height(18.dp)
    )

    SectionTitle(
        "Medición de señal · requisito del cliente"
    )

    Text(
        text =
            "Estos campos complementan SIC-22 y luego pueden automatizarse con ARCore.",
        style =
            MaterialTheme.typography.bodySmall
    )

    Spacer(
        modifier =
            Modifier.height(10.dp)
    )

    OutlinedTextField(
        value = signWidth,
        onValueChange = {
            signWidth = it
        },
        label = {
            Text(
                "Ancho de señal (m)"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = signHeight,
        onValueChange = {
            signHeight = it
        },
        label = {
            Text(
                "Alto de señal (m)"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = lowerEdgeHeight,
        onValueChange = {
            lowerEdgeHeight = it
        },
        label = {
            Text(
                "Altura piso → borde inferior (m)"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )

    Spacer(
        modifier =
            Modifier.height(10.dp)
    )

    OutlinedButton(
        onClick = {
            // Próxima fase: ARCore
        },
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Text(
            "Medir con cámara · próximamente"
        )
    }
}


// =========================================================
// SIC-21 · MARCAS HORIZONTALES
// =========================================================

@Composable
fun HorizontalMarksFields() {

    var type by remember {
        mutableStateOf("1 - Central")
    }

    var material by remember {
        mutableStateOf("5 - Otro")
    }

    var condition by remember {
        mutableStateOf("1 - Buena")
    }

    SectionTitle(
        "SIC-21 · Señalización horizontal - Marcas"
    )

    Text(
        text =
            "Clase: 18 - Señalización Horizontal-Marcas",
        fontWeight =
            FontWeight.Bold
    )

    Spacer(
        modifier =
            Modifier.height(8.dp)
    )

    Text(
        text = "Tipo",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Central",
            "2 - Lateral",
            "3 - Central y Lateral"
        ),
        selected = type,
        onSelected = {
            type = it
        }
    )

    Text(
        text = "Material",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Acero",
            "2 - Concreto",
            "3 - Mampostería",
            "4 - Plástico",
            "5 - Otro"
        ),
        selected = material,
        onSelected = {
            material = it
        }
    )

    Text(
        text = "Condición",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Buena · no tiene problema",
            "2 - Regular · todavía visible / afectación menor al 30%",
            "3 - Mala · apenas visible / afectación mayor al 30%"
        ),
        selected = condition,
        onSelected = {
            condition = it
        }
    )
}


// =========================================================
// SIC-21 · TACHAS
// =========================================================

@Composable
fun HorizontalStudsFields() {

    var type by remember {
        mutableStateOf("1 - Central")
    }

    var material by remember {
        mutableStateOf("4 - Plástico")
    }

    var condition by remember {
        mutableStateOf("1 - Buena")
    }

    SectionTitle(
        "SIC-21 · Señalización horizontal - Tachas"
    )

    Text(
        text =
            "Clase: 20 - Señalización Horizontal-Tachas",
        fontWeight =
            FontWeight.Bold
    )

    Spacer(
        modifier =
            Modifier.height(8.dp)
    )

    Text(
        text = "Tipo",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Central",
            "2 - Lateral",
            "3 - Central y Lateral"
        ),
        selected = type,
        onSelected = {
            type = it
        }
    )

    Text(
        text = "Material",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Acero",
            "2 - Concreto",
            "3 - Mampostería",
            "4 - Plástico",
            "5 - Otro"
        ),
        selected = material,
        onSelected = {
            material = it
        }
    )

    Text(
        text = "Condición",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Buena · no tiene problema",
            "2 - Regular · dañada/ausente en menos del 30%",
            "3 - Mala · dañada/ausente en más del 30%"
        ),
        selected = condition,
        onSelected = {
            condition = it
        }
    )
}


// =========================================================
// SIC-21 · SEGURIDAD VIAL
// =========================================================

@Composable
fun SafetyElementFields() {

    var type by remember {
        mutableStateOf("1 - Guardavías")
    }

    var material by remember {
        mutableStateOf("1 - Acero")
    }

    var condition by remember {
        mutableStateOf("1 - Buena")
    }

    SectionTitle(
        "SIC-21 · Seguridad vial"
    )

    Text(
        text =
            "Clase: 19 - Seguridad",
        fontWeight =
            FontWeight.Bold
    )

    Spacer(
        modifier =
            Modifier.height(8.dp)
    )

    Text(
        text = "Tipo",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Guardavías",
            "2 - Postes Delineadores",
            "3 - Barreras de Contención",
            "4 - Resaltos"
        ),
        selected = type,
        onSelected = {
            type = it
        }
    )

    Text(
        text = "Material",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Acero",
            "2 - Concreto",
            "3 - Mampostería",
            "4 - Plástico",
            "5 - Otro"
        ),
        selected = material,
        onSelected = {
            material = it
        }
    )

    Text(
        text = "Condición",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Buena · no tiene problema",
            "2 - Regular · dañado/ausente en menos del 30%",
            "3 - Mala · muy dañado/ausente en más del 30%"
        ),
        selected = condition,
        onSelected = {
            condition = it
        }
    )
}


// =========================================================
// FORMULARIO GENERAL PARA ALCANTARILLA / CUNETA / BADÉN / PUENTE
// =========================================================

@Composable
fun AssetFormScreen(
    assetType: RoadAssetType,
    onBack: () -> Unit,
    onSave: () -> Unit
) {

    val context =
        LocalContext.current

    var route by remember {
        mutableStateOf("")
    }

    var roadbed by remember {
        mutableStateOf("")
    }

    var startPr by remember {
        mutableStateOf("")
    }

    var startDistance by remember {
        mutableStateOf("")
    }

    var endPr by remember {
        mutableStateOf("")
    }

    var endDistance by remember {
        mutableStateOf("")
    }

    var side by remember {
        mutableStateOf("D - Derecho")
    }

    var observations by remember {
        mutableStateOf("")
    }

    val registrationDate = remember {
        SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        ).format(Date())
    }

    var photoPath by remember {
        mutableStateOf<String?>(null)
    }

    var photoCaptured by remember {
        mutableStateOf(false)
    }

    var photoError by remember {
        mutableStateOf<String?>(null)
    }

    var latitude by remember {
        mutableStateOf<Double?>(null)
    }

    var longitude by remember {
        mutableStateOf<Double?>(null)
    }

    var gpsAccuracy by remember {
        mutableStateOf<Float?>(null)
    }

    var locating by remember {
        mutableStateOf(false)
    }

    var locationError by remember {
        mutableStateOf<String?>(null)
    }

    var formError by remember {
        mutableStateOf<String?>(null)
    }


    fun hasLocationPermission(): Boolean {

        val fine =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) ==
                    PackageManager.PERMISSION_GRANTED

        val coarse =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) ==
                    PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }


    fun captureLocation() {

        if (!hasLocationPermission()) return

        locating = true
        locationError = null

        getCurrentGpsLocation(
            context = context,

            onSuccess = {
                    lat,
                    lon,
                    accuracy ->

                latitude = lat
                longitude = lon
                gpsAccuracy = accuracy
                locating = false
            },

            onError = { error ->

                locationError = error
                locating = false
            }
        )
    }


    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestMultiplePermissions()
        ) { permissions ->

            val fineGranted =
                permissions[
                    Manifest.permission
                        .ACCESS_FINE_LOCATION
                ] == true

            val coarseGranted =
                permissions[
                    Manifest.permission
                        .ACCESS_COARSE_LOCATION
                ] == true

            if (
                fineGranted ||
                coarseGranted
            ) {

                captureLocation()

            } else {

                locationError =
                    "Debes permitir el acceso a la ubicación."
            }
        }


    LaunchedEffect(assetType) {

        if (hasLocationPermission()) {

            captureLocation()

        } else {

            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }


    val takePictureLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .TakePicture()
        ) { success ->

            photoCaptured = success

            if (success) {

                photoError = null

                if (hasLocationPermission()) {

                    captureLocation()

                } else {

                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission
                                .ACCESS_FINE_LOCATION,

                            Manifest.permission
                                .ACCESS_COARSE_LOCATION
                        )
                    )
                }

            } else {

                photoError =
                    "La fotografía fue cancelada o no pudo guardarse."
            }
        }


    fun openCamera() {

        try {

            val prefix = when (assetType) {

                RoadAssetType.CULVERT ->
                    "ALCANTARILLA"

                RoadAssetType.DITCH ->
                    "CUNETA"

                RoadAssetType.FORD ->
                    "BADEN"

                RoadAssetType.BRIDGE ->
                    "PUENTE"

                RoadAssetType.SIGNALIZATION ->
                    "SENALIZACION"
            }

            val photo =
                createPhotoFile(
                    context = context,
                    prefix = prefix
                )

            photoPath =
                photo.file.absolutePath

            photoCaptured = false
            photoError = null

            takePictureLauncher.launch(
                photo.uri
            )

        } catch (error: Exception) {

            photoError =
                error.message
                    ?: "No se pudo abrir la cámara."
        }
    }


    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestPermission()
        ) { granted ->

            if (granted) {

                openCamera()

            } else {

                photoError =
                    "Debes permitir el acceso a la cámara."
            }
        }


    val needsEndLocation =
        assetType == RoadAssetType.DITCH ||
                assetType == RoadAssetType.FORD ||
                assetType == RoadAssetType.BRIDGE

    val needsSide =
        assetType == RoadAssetType.DITCH ||
                assetType == RoadAssetType.FORD


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),

        contentPadding =
            PaddingValues(
                top = 24.dp,
                bottom = 40.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(18.dp)
    ) {

        item {

            OutlinedButton(
                onClick = onBack
            ) {
                Text("← Volver")
            }
        }

        item {

            Text(
                text =
                    "Registrar ${assetType.title}",
                fontSize = 30.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    "${assetType.sicCode} · ${assetType.subtitle}",
                style =
                    MaterialTheme.typography.bodyLarge
            )
        }

        // FOTO
        item {

            SectionTitle("Fotografía")

            Button(
                onClick = {

                    val permission =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        )

                    if (
                        permission ==
                        PackageManager.PERMISSION_GRANTED
                    ) {

                        openCamera()

                    } else {

                        cameraPermissionLauncher.launch(
                            Manifest.permission.CAMERA
                        )
                    }
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
            ) {

                Text(
                    text =
                        if (photoCaptured)
                            "Tomar otra fotografía"
                        else
                            "Tomar fotografía",
                    fontSize = 18.sp
                )
            }
        }

        if (
            photoCaptured &&
            photoPath != null
        ) {

            item {

                val path =
                    photoPath!!

                val bitmap =
                    remember(
                        path,
                        photoCaptured
                    ) {

                        loadCorrectlyOrientedBitmap(
                            path
                        )
                    }

                if (bitmap != null) {

                    Image(
                        bitmap =
                            bitmap.asImageBitmap(),

                        contentDescription =
                            "Fotografía capturada",

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),

                        contentScale =
                            ContentScale.Fit
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        "✓ Fotografía guardada",
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        File(path).name,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }

        photoError?.let { error ->

            item {
                ErrorText(error)
            }
        }

        // GPS
        item {

            SectionTitle("Ubicación GPS")

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(16.dp)
                ) {

                    Text(
                        text =
                            "Latitud: ${
                                latitude?.let {
                                    String.format(
                                        Locale.US,
                                        "%.8f",
                                        it
                                    )
                                } ?: "Pendiente"
                            }"
                    )

                    Text(
                        text =
                            "Longitud: ${
                                longitude?.let {
                                    String.format(
                                        Locale.US,
                                        "%.8f",
                                        it
                                    )
                                } ?: "Pendiente"
                            }"
                    )

                    Text(
                        text =
                            "Precisión: ${
                                gpsAccuracy?.let {
                                    String.format(
                                        Locale.US,
                                        "± %.1f m",
                                        it
                                    )
                                } ?: "Pendiente"
                            }"
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(
                        text =
                            when {

                                locating -> {
                                    "Obteniendo ubicación GPS..."
                                }

                                latitude != null &&
                                        longitude != null -> {
                                    "✓ Ubicación obtenida automáticamente"
                                }

                                else -> {
                                    "Esperando ubicación GPS..."
                                }
                            },

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }

        locationError?.let { error ->

            item {
                ErrorText(error)
            }
        }

        item {
            Divider()
        }

        // DATOS COMUNES
        item {

            SectionTitle(
                "Datos generales del formato"
            )

            OutlinedTextField(
                value = route,
                onValueChange = {
                    route =
                        it.uppercase()
                },
                label = {
                    Text("Ruta")
                },
                placeholder = {
                    Text("Ej. PE-1N, PE-3S")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = roadbed,
                onValueChange = {
                    roadbed =
                        it.uppercase()
                },
                label = {
                    Text("Calzada")
                },
                placeholder = {
                    Text("Ej. UC, UD, CD, A1")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )
        }

        item {

            SectionTitle("Ubicación inicio")

            OutlinedTextField(
                value = startPr,
                onValueChange = {
                    startPr = it
                },
                label = {
                    Text("Código PR inicio")
                },
                placeholder = {
                    Text("4 dígitos, ej. 0010")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = startDistance,
                onValueChange = {
                    startDistance = it
                },
                label = {
                    Text(
                        "Distancia desde PR inicio (m)"
                    )
                },
                placeholder = {
                    Text("Ej. 125.50")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )
        }

        if (needsEndLocation) {

            item {

                SectionTitle("Ubicación fin")

                OutlinedTextField(
                    value = endPr,
                    onValueChange = {
                        endPr = it
                    },
                    label = {
                        Text("Código PR fin")
                    },
                    placeholder = {
                        Text(
                            "4 dígitos, ej. 0010"
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = endDistance,
                    onValueChange = {
                        endDistance = it
                    },
                    label = {
                        Text(
                            "Distancia desde PR fin (m)"
                        )
                    },
                    placeholder = {
                        Text("Ej. 180.25")
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        }

        if (needsSide) {

            item {

                Text(
                    text = "Lado",
                    fontWeight =
                        FontWeight.Bold
                )

                ChoiceSelector(
                    options = listOf(
                        "D - Derecho",
                        "I - Izquierdo",
                        "S - Sin objeto"
                    ),
                    selected = side,
                    onSelected = {
                        side = it
                    }
                )
            }
        }

        item {
            Divider()
        }

        item {

            when (assetType) {

                RoadAssetType.CULVERT ->
                    CulvertFields()

                RoadAssetType.DITCH ->
                    DitchFields()

                RoadAssetType.FORD ->
                    FordFields()

                RoadAssetType.BRIDGE ->
                    BridgeFields()

                RoadAssetType.SIGNALIZATION -> {
                    Text(
                        "Use el módulo Señalización y seguridad."
                    )
                }
            }
        }

        item {
            Divider()
        }

        item {

            SectionTitle("Registro")

            Text(
                text =
                    "Fecha: $registrationDate"
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = observations,
                onValueChange = {
                    observations = it
                },
                label = {
                    Text(
                        "Observaciones adicionales"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
        }

        formError?.let { error ->

            item {
                ErrorText(error)
            }
        }

        item {

            Button(
                onClick = {

                    when {

                        !photoCaptured -> {
                            formError =
                                "Debes tomar una fotografía."
                        }

                        latitude == null ||
                                longitude == null -> {
                            formError =
                                "Debes obtener la ubicación GPS."
                        }

                        route.trim().isEmpty() -> {
                            formError =
                                "Debes ingresar el código de ruta."
                        }

                        roadbed.trim().isEmpty() -> {
                            formError =
                                "Debes ingresar el código de calzada."
                        }

                        startPr.trim().isEmpty() -> {
                            formError =
                                "Debes ingresar el PR de inicio."
                        }

                        else -> {
                            formError = null

                            // Programa la subida de la foto a Drive.
                            // Si no hay Internet, WorkManager esperará
                            // hasta que vuelva a haber conexión.
                            photoPath?.let { path ->

                                scheduleDriveUpload(
                                    context = context,
                                    photoPath = path
                                )
                            }

                            onSave()
                        }
                    }
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {

                Text(
                    text =
                        "Guardar registro",
                    fontSize = 20.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}


// =========================================================
// SIC-18 · ALCANTARILLAS
// =========================================================

@Composable
fun CulvertFields() {

    var culvertClass by remember {
        mutableStateOf(
            "06 - Alcantarilla Definitiva"
        )
    }

    var type by remember {
        mutableStateOf("1 - Concreto")
    }

    var spans by remember {
        mutableStateOf("1")
    }

    var section by remember {
        mutableStateOf(
            "2 - Circular / Ovalada"
        )
    }

    var dimension1 by remember {
        mutableStateOf("")
    }

    var dimension2 by remember {
        mutableStateOf("")
    }

    var structuralCondition by remember {
        mutableStateOf("1 - Buena")
    }

    var functionalCondition by remember {
        mutableStateOf(
            "1 - Buena · limpia"
        )
    }

    val definitive =
        culvertClass.startsWith("06")

    val typeOptions =
        if (definitive) {

            listOf(
                "1 - Concreto",
                "2 - Mampostería",
                "3 - Acero / TMC",
                "4 - Polietileno HDPE",
                "5 - Otro"
            )

        } else {

            listOf(
                "1 - Concreto",
                "2 - Mampostería",
                "3 - Piedra",
                "4 - Otro"
            )
        }

    SectionTitle(
        "SIC-18 · Alcantarilla"
    )

    Text(
        text = "Clase",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "06 - Alcantarilla Definitiva",
            "07 - Estructura Artesanal"
        ),
        selected = culvertClass,
        onSelected = {
            culvertClass = it
            type = "1 - Concreto"
        }
    )

    Spacer(
        modifier =
            Modifier.height(8.dp)
    )

    Text(
        text = "Tipo / material",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options =
            typeOptions,
        selected =
            type,
        onSelected = {
            type = it
        }
    )

    if (definitive) {

        Text(
            text =
                "Nota: el manual usa HDPE. Si el cliente usa PVC, debe confirmarse su codificación.",
            style =
                MaterialTheme.typography.bodySmall
        )
    }

    Spacer(
        modifier =
            Modifier.height(10.dp)
    )

    OutlinedTextField(
        value = spans,
        onValueChange = {
            spans = it
        },
        label = {
            Text(
                "Número de ojos / vanos"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )

    Spacer(
        modifier =
            Modifier.height(10.dp)
    )

    Text(
        text = "Sección transversal",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Marco",
            "2 - Circular / Ovalada",
            "3 - Arco",
            "4 - Pórtico",
            "5 - Otro"
        ),
        selected = section,
        onSelected = {
            section = it
        }
    )

    Spacer(
        modifier =
            Modifier.height(10.dp)
    )

    OutlinedTextField(
        value = dimension1,
        onValueChange = {
            dimension1 = it
        },
        label = {
            Text(
                if (
                    section.startsWith("2")
                )
                    "Dimensión 1 · diámetro (m)"
                else
                    "Dimensión 1 · ancho (m)"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )

    if (
        !section.startsWith("2")
    ) {

        OutlinedTextField(
            value = dimension2,
            onValueChange = {
                dimension2 = it
            },
            label = {
                Text(
                    "Dimensión 2 · altura (m)"
                )
            },
            modifier =
                Modifier.fillMaxWidth()
        )
    }

    Spacer(
        modifier =
            Modifier.height(12.dp)
    )

    Text(
        text =
            "Condición estructural",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Buena",
            "2 - Regular",
            "3 - Mala"
        ),
        selected =
            structuralCondition,
        onSelected = {
            structuralCondition = it
        }
    )

    Text(
        text =
            "Condición funcional",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Buena · limpia",
            "2 - Regular · parcialmente obstruida",
            "3 - Mala · totalmente obstruida"
        ),
        selected =
            functionalCondition,
        onSelected = {
            functionalCondition = it
        }
    )
}


// =========================================================
// SIC-19 · CUNETAS / DRENAJE
// =========================================================

@Composable
fun DitchFields() {

    var elementClass by remember {
        mutableStateOf("08 - Cuneta")
    }

    var type by remember {
        mutableStateOf("1 - Tierra")
    }

    var section by remember {
        mutableStateOf("1 - Triangular")
    }

    var structuralCondition by remember {
        mutableStateOf("1 - Buena")
    }

    var functionalCondition by remember {
        mutableStateOf(
            "1 - Buena · limpia"
        )
    }

    SectionTitle(
        "SIC-19 · Cunetas y drenaje"
    )

    Text(
        text = "Clase",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "08 - Cuneta",
            "09 - Canal",
            "10 - Bajada de Agua",
            "11 - Zanja de Drenaje",
            "12 - Zanja de Coronación",
            "13 - Cuneta de Banqueta"
        ),
        selected =
            elementClass,
        onSelected = {
            elementClass = it
        }
    )

    Spacer(
        modifier =
            Modifier.height(8.dp)
    )

    Text(
        text = "Tipo",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Tierra",
            "2 - Concreto",
            "3 - Mampostería",
            "4 - Otro"
        ),
        selected = type,
        onSelected = {
            type = it
        }
    )

    Spacer(
        modifier =
            Modifier.height(8.dp)
    )

    Text(
        text =
            "Sección transversal",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Triangular",
            "2 - Trapezoidal",
            "3 - Rectangular",
            "4 - Otro"
        ),
        selected = section,
        onSelected = {
            section = it
        }
    )

    Spacer(
        modifier =
            Modifier.height(8.dp)
    )

    Text(
        text =
            "Condición estructural",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Buena",
            "2 - Regular",
            "3 - Mala"
        ),
        selected =
            structuralCondition,
        onSelected = {
            structuralCondition = it
        }
    )

    Text(
        text =
            "Condición funcional",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Buena · limpia",
            "2 - Regular · parcialmente obstruida",
            "3 - Mala · totalmente obstruida"
        ),
        selected =
            functionalCondition,
        onSelected = {
            functionalCondition = it
        }
    )

    Text(
        text =
            "La longitud se obtiene mediante Ubicación inicio y Ubicación fin.",
        style =
            MaterialTheme.typography.bodySmall
    )
}


// =========================================================
// SIC-20 · BADÉN
// =========================================================

@Composable
fun FordFields() {

    var type by remember {
        mutableStateOf(
            "2 - Concreto"
        )
    }

    var dimension1 by remember {
        mutableStateOf("")
    }

    var dimension2 by remember {
        mutableStateOf("")
    }

    var structuralCondition by remember {
        mutableStateOf("1 - Buena")
    }

    var functionalCondition by remember {
        mutableStateOf(
            "1 - Buena · limpia"
        )
    }

    SectionTitle(
        "SIC-20 · Badén"
    )

    Text(
        text =
            "Clase: 12 - Badén",
        fontWeight =
            FontWeight.Bold
    )

    Spacer(
        modifier =
            Modifier.height(8.dp)
    )

    Text(
        text = "Tipo",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Gavión",
            "2 - Concreto",
            "3 - Mampostería",
            "4 - Concreto ciclópeo",
            "5 - Piedra",
            "6 - Otro"
        ),
        selected = type,
        onSelected = {
            type = it
        }
    )

    Spacer(
        modifier =
            Modifier.height(10.dp)
    )

    OutlinedTextField(
        value = dimension1,
        onValueChange = {
            dimension1 = it
        },
        label = {
            Text(
                "Dimensión 1 · ancho de rodadura (m)"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = dimension2,
        onValueChange = {
            dimension2 = it
        },
        label = {
            Text(
                "Dimensión 2 · ancho total con protección contra erosión (m)"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )

    Spacer(
        modifier =
            Modifier.height(12.dp)
    )

    Text(
        text =
            "Condición estructural",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Buena · no tiene problema",
            "2 - Regular · puede tener problemas",
            "3 - Mala · necesita repararse"
        ),
        selected =
            structuralCondition,
        onSelected = {
            structuralCondition = it
        }
    )

    Text(
        text =
            "Condición funcional",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Buena · limpia",
            "2 - Regular · parcialmente obstruida",
            "3 - Mala · totalmente obstruida"
        ),
        selected =
            functionalCondition,
        onSelected = {
            functionalCondition = it
        }
    )
}


// =========================================================
// SIC-17 · PUENTE · BASE INICIAL
// =========================================================

@Composable
fun BridgeFields() {

    var bridgeCode by remember {
        mutableStateOf("")
    }

    var bridgeName by remember {
        mutableStateOf("")
    }

    var bridgeClass by remember {
        mutableStateOf(
            "01 - Puente Definitivo"
        )
    }

    var spans by remember {
        mutableStateOf("")
    }

    var length by remember {
        mutableStateOf("")
    }

    var lowerHeight by remember {
        mutableStateOf("")
    }

    var structuralCondition by remember {
        mutableStateOf("1 - Buena")
    }

    var functionalCondition by remember {
        mutableStateOf("1 - Buena")
    }

    SectionTitle(
        "SIC-17 · Puente · base inicial"
    )

    OutlinedTextField(
        value = bridgeCode,
        onValueChange = {
            bridgeCode =
                it.uppercase()
        },
        label = {
            Text("Código del puente")
        },
        modifier =
            Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = bridgeName,
        onValueChange = {
            bridgeName = it
        },
        label = {
            Text("Nombre del puente")
        },
        modifier =
            Modifier.fillMaxWidth()
    )

    Text(
        text = "Clase",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "01 - Puente Definitivo",
            "02 - Puente Provisional",
            "03 - Estructura Artesanal",
            "04 - Puente Histórico"
        ),
        selected =
            bridgeClass,
        onSelected = {
            bridgeClass = it
        }
    )

    OutlinedTextField(
        value = spans,
        onValueChange = {
            spans = it
        },
        label = {
            Text("Número de vanos")
        },
        modifier =
            Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = length,
        onValueChange = {
            length = it
        },
        label = {
            Text(
                "Dimensión 1 · longitud (m)"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = lowerHeight,
        onValueChange = {
            lowerHeight = it
        },
        label = {
            Text(
                "Dimensión 2 · altura inferior (m)"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )

    Text(
        text =
            "Condición estructural",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Buena",
            "2 - Regular",
            "3 - Mala"
        ),
        selected =
            structuralCondition,
        onSelected = {
            structuralCondition = it
        }
    )

    Text(
        text =
            "Condición funcional",
        fontWeight =
            FontWeight.Bold
    )

    ChoiceSelector(
        options = listOf(
            "1 - Buena",
            "2 - Regular",
            "3 - Mala"
        ),
        selected =
            functionalCondition,
        onSelected = {
            functionalCondition = it
        }
    )

    Text(
        text =
            "SIC-17 tiene formatos adicionales 17A y 17B. Los completaremos cuando el cliente confirme si también los requiere.",
        style =
            MaterialTheme.typography.bodySmall
    )
}


// =========================================================
// COMPONENTES AUXILIARES
// =========================================================

@Composable
fun ChoiceSelector(
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {

    LazyRow(
        horizontalArrangement =
            Arrangement.spacedBy(8.dp),

        contentPadding =
            PaddingValues(
                vertical = 8.dp
            )
    ) {

        items(options) { option ->

            AssistChip(
                onClick = {
                    onSelected(option)
                },
                label = {

                    Text(
                        text =
                            if (
                                selected == option
                            )
                                "✓ $option"
                            else
                                option
                    )
                }
            )
        }
    }
}


@Composable
fun SectionTitle(
    text: String
) {

    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight =
            FontWeight.Bold
    )
}


@Composable
fun ErrorText(
    text: String
) {

    Text(
        text = text,
        color =
            MaterialTheme.colorScheme.error,
        fontWeight =
            FontWeight.Bold
    )
}


// =========================================================
// PREVIEW
// =========================================================

@Preview(
    showBackground = true,
    widthDp = 800,
    heightDp = 1280
)
@Composable
fun PreviewInventarioVial() {

    InventarioVialTheme {
        InventarioVialApp()
    }
}
