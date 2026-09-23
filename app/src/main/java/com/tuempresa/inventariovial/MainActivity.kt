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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.model.RoadAssetType
import com.tuempresa.inventariovial.model.SignalizationType
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


import com.tuempresa.inventariovial.model.form.Sic17FormState
import com.tuempresa.inventariovial.model.form.Sic18FormState
import com.tuempresa.inventariovial.model.form.Sic19FormState
import com.tuempresa.inventariovial.model.form.Sic20FormState
import com.tuempresa.inventariovial.model.form.Sic21FormState
import com.tuempresa.inventariovial.model.form.Sic22FormState
import com.tuempresa.inventariovial.model.form.Sic23FormState

import androidx.compose.runtime.collectAsState

import androidx.lifecycle.ViewModelProvider

import com.tuempresa.inventariovial.model.form.InventorySaveRequest
import com.tuempresa.inventariovial.model.form.SicFormDetail

import com.tuempresa.inventariovial.viewmodel.InventoryViewModel


// =========================================================
// UBICACIÓN GPS AUXILIAR
// Se usa para guardar la ubicación final de un elemento.
// =========================================================

//data class GeoLocation(
//    val latitude: Double,
//    val longitude: Double,
//    val accuracyHorizontal: Float
//)


class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        enableEdgeToEdge()


        val inventoryViewModel =

            ViewModelProvider(
                this
            )[
                InventoryViewModel::class.java
            ]


        setContent {

            InventarioVialTheme {

                InventarioVialApp(
                    inventoryViewModel =
                        inventoryViewModel
                )
            }
        }
    }
}




// =========================================================
// APP PRINCIPAL
// =========================================================

@Composable
fun InventarioVialApp(
    inventoryViewModel:
    InventoryViewModel
) {

    var showExport by rememberSaveable { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var selectedAsset by remember {
        mutableStateOf<RoadAssetType?>(null)
    }

    var selectedSignalization by remember {
        mutableStateOf<SignalizationType?>(null)
    }

    val recordsToday by
    inventoryViewModel
        .recordsToday
        .collectAsState()


    val pendingSync by
    inventoryViewModel
        .pendingSync
        .collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            when {
                showExport -> SicExportScreen(inventoryViewModel) { showExport = false }
                showHistory -> RecordHistoryScreen(inventoryViewModel) { showHistory = false }

                selectedAsset == null -> {

                    HomeScreen(
                        onExport = { showExport = true },
                        onHistory = { showHistory = true },
                        onSync = { inventoryViewModel.syncPending { syncMessage = it } },
                        syncMessage = syncMessage,
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
                        inventoryViewModel = inventoryViewModel,
                        onBack = {
                            selectedSignalization = null
                        },
                        onSave = {
                            selectedSignalization = null
                            selectedAsset = null
                        }
                    )
                }


                else -> {

                    AssetFormScreen(
                        assetType = selectedAsset!!,
                        inventoryViewModel = inventoryViewModel,
                        onBack = {
                            selectedAsset = null
                        },
                        onSave = {
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
    onAssetSelected: (RoadAssetType) -> Unit,
    onExport: () -> Unit = {},
    onHistory: () -> Unit = {},
    onSync: () -> Unit = {},
    syncMessage: String? = null
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onHistory) { Text("Ver registros") }
                OutlinedButton(onClick = onSync) { Text("Sincronizar") }
            }
            OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Exportar SIC a Excel") }
            syncMessage?.let { Text(it) }
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
    inventoryViewModel: InventoryViewModel,
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
    var capturedPhotos by remember { mutableStateOf<List<String>>(emptyList()) }
    var endLocation by remember { mutableStateOf<GeoLocation?>(null) }
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

    // =========================================================
    // ESTADOS SIC CALIFICADOS
    // =========================================================

    var sic21State by remember(signalizationType) {

        mutableStateOf(

            when (signalizationType) {

                SignalizationType.HORIZONTAL_MARKS ->
                    Sic21FormState(
                        classCode = "18",
                        typeCode = "1",
                        materialCode = "5",
                        conditionCode = "1"
                    )

                SignalizationType.SAFETY ->
                    Sic21FormState(
                        classCode = "19",
                        typeCode = "1",
                        materialCode = "1",
                        conditionCode = "1"
                    )

                SignalizationType.HORIZONTAL_STUDS ->
                    Sic21FormState(
                        classCode = "20",
                        typeCode = "1",
                        materialCode = "4",
                        conditionCode = "1"
                    )

                SignalizationType.VERTICAL ->
                    Sic21FormState()
            }
        )
    }


    var sic22State by remember(signalizationType) {

        mutableStateOf(
            Sic22FormState()
        )
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

            if (success) photoPath?.let { capturedPhotos = capturedPhotos + it }
            photoCaptured = capturedPhotos.isNotEmpty()
            photoPath = capturedPhotos.lastOrNull()

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

        item {
            Text("Fotografías del elemento: ${capturedPhotos.size}")
            capturedPhotos.forEachIndexed { index, path ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Fotografía ${index + 1}")
                    OutlinedButton(onClick = {
                        capturedPhotos = capturedPhotos - path
                        photoPath = capturedPhotos.lastOrNull()
                        photoCaptured = capturedPhotos.isNotEmpty()
                    }) { Text("Quitar") }
                }
            }
            FinalLocationCapture(endLocation) { endLocation = it }
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

                SignalizationType.VERTICAL -> {

                    Sic22Fields(
                        state = sic22State,
                        onStateChange = {
                            sic22State = it
                        }
                    )
                }

                SignalizationType.HORIZONTAL_MARKS,
                SignalizationType.HORIZONTAL_STUDS,
                SignalizationType.SAFETY -> {

                    Sic21Fields(
                        state = sic21State,
                        onStateChange = {
                            sic21State = it
                        }
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

                        startDistance.trim().isEmpty() -> {

                            formError =
                                "Debes ingresar la distancia desde el PR de inicio."
                        }


                        endPr.trim().isEmpty() -> {

                            formError =
                                "Debes ingresar el PR de fin."
                        }


                        endDistance.trim().isEmpty() -> {

                            formError =
                                "Debes ingresar la distancia desde el PR de fin."
                        }


                        signalizationType ==
                                SignalizationType.VERTICAL &&
                                sic22State.usesSignalCode &&
                                sic22State.signalCode
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar el código de la señal."
                        }


                        signalizationType ==
                                SignalizationType.VERTICAL &&
                                sic22State
                                    .usesKilometerPostNumber &&
                                sic22State
                                    .kilometerPostNumber
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar el número del poste kilométrico."
                        }

                        else -> {
                            formError = null

                            val path = photoPath

                            if (path == null) {

                                formError =
                                    "No se encontró la fotografía."

                            } else {

                                val detail =
                                    when (signalizationType) {

                                        SignalizationType.VERTICAL ->
                                            SicFormDetail.Sic22(
                                                sic22State
                                            )

                                        SignalizationType.HORIZONTAL_MARKS,
                                        SignalizationType.HORIZONTAL_STUDS,
                                        SignalizationType.SAFETY ->
                                            SicFormDetail.Sic21(
                                                sic21State
                                            )
                                    }

                                inventoryViewModel.saveRecord(

                                    request =
                                        InventorySaveRequest(

                                            sicCode =
                                                if (
                                                    signalizationType ==
                                                    SignalizationType.VERTICAL
                                                ) {
                                                    "SIC-22"
                                                } else {
                                                    "SIC-21"
                                                },

                                            assetType =
                                                signalizationType.name,

                                            routeCode = route,

                                            roadbedCode = roadbed,

                                            startPrCode = startPr,

                                            startDistanceM =
                                                startDistance,

                                            endPrCode = endPr,

                                            endDistanceM =
                                                endDistance,

                                            sideCode =
                                                codeFromOption(
                                                    side
                                                ),

                                            latitude = latitude!!,

                                            longitude = longitude!!,

                                            gpsAccuracyM =
                                                gpsAccuracy,

                                            surveyDate =
                                                registrationDate,

                                            observations =
                                                observations,

                                            photoPath = capturedPhotos.first(),
                                            photoPaths = capturedPhotos,
                                            endLatitude = endLocation?.latitude,
                                            endLongitude = endLocation?.longitude,
                                            endGpsAccuracyM = endLocation?.accuracyHorizontal,

                                            detail = detail
                                        ),

                                    onSuccess = {
                                        onSave()
                                    },

                                    onError = { error ->
                                        formError = error
                                    }
                                )
                            }
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
// INVENTARIO VIAL CALIFICADO
// =========================================================

@Composable
fun Sic22Fields(
    state: Sic22FormState,
    onStateChange: (Sic22FormState) -> Unit
) {

    val typeOptions = listOf(
        "1 - Reglamento",
        "2 - Preventivo",
        "3 - Informativo",
        "4 - Poste Kilométrico",
        "5 - Semáforos",
        "6 - Postes SOS"
    )

    val materialOptions = listOf(
        "1 - Fibra de vidrio",
        "2 - Acero",
        "3 - Concreto",
        "4 - Madera",
        "5 - Otro"
    )

    val conditionOptions = listOf(
        "1 - Buena · no tiene problema",
        "2 - Regular · dañado pero se puede leer",
        "3 - Mala · no se puede leer o ausente"
    )


    SectionTitle(
        "SIC-22 · Señalización vertical"
    )

    Text(
        "Inventario Vial Calificado",
        fontWeight = FontWeight.Bold,
        style =
            MaterialTheme.typography.bodySmall
    )


    Text(
        "Clase: 20 - Señalización Vertical",
        fontWeight =
            FontWeight.Bold
    )


    Text(
        "Tipo",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = typeOptions,
        selected = optionForCode(
            typeOptions,
            state.typeCode
        ),
        onSelected = {

            val newType =
                codeFromOption(it)

            onStateChange(
                state.copy(
                    typeCode = newType,

                    signalCode =
                        if (
                            newType == "1" ||
                            newType == "2" ||
                            newType == "3"
                        )
                            state.signalCode
                        else
                            "",

                    kilometerPostNumber =
                        if (newType == "4")
                            state.kilometerPostNumber
                        else
                            ""
                )
            )
        }
    )


    Text(
        "Material",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = materialOptions,
        selected = optionForCode(
            materialOptions,
            state.materialCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    materialCode =
                        codeFromOption(it)
                )
            )
        }
    )


    if (state.usesSignalCode) {

        OutlinedTextField(
            value =
                state.signalCode,

            onValueChange = {

                onStateChange(
                    state.copy(
                        signalCode =
                            it.uppercase()
                    )
                )
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

            modifier =
                Modifier.fillMaxWidth()
        )
    }


    if (
        state.usesKilometerPostNumber
    ) {

        OutlinedTextField(
            value =
                state.kilometerPostNumber,

            onValueChange = {

                onStateChange(
                    state.copy(
                        kilometerPostNumber = it
                    )
                )
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


    Text(
        "Condición",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = conditionOptions,
        selected = optionForCode(
            conditionOptions,
            state.conditionCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    conditionCode =
                        codeFromOption(it)
                )
            )
        }
    )


    Spacer(
        modifier =
            Modifier.height(18.dp)
    )


    SectionTitle(
        "Mediciones adicionales del cliente"
    )

    Text(
        text =
            "Estos tres campos complementan al SIC-22; no forman parte de las columnas oficiales del formato.",
        style =
            MaterialTheme.typography.bodySmall
    )


    OutlinedTextField(
        value =
            state.signWidthM,

        onValueChange = {

            onStateChange(
                state.copy(
                    signWidthM = it
                )
            )
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
        value =
            state.signHeightM,

        onValueChange = {

            onStateChange(
                state.copy(
                    signHeightM = it
                )
            )
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
        value =
            state.lowerEdgeHeightM,

        onValueChange = {

            onStateChange(
                state.copy(
                    lowerEdgeHeightM = it
                )
            )
        },

        label = {
            Text(
                "Altura suelo → borde inferior (m)"
            )
        },

        modifier =
            Modifier.fillMaxWidth()
    )


    OutlinedButton(
        onClick = {
            // Futura integración ARCore.
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
// SIC-21
// SEGURIDAD Y SEÑALIZACIÓN HORIZONTAL
// INVENTARIO VIAL CALIFICADO
// =========================================================

@Composable
fun Sic21Fields(
    state: Sic21FormState,
    onStateChange: (Sic21FormState) -> Unit
) {

    val typeOptions =
        when (state.classCode) {

            "18" -> listOf(
                "1 - Central",
                "2 - Lateral",
                "3 - Central y Lateral"
            )

            "19" -> listOf(
                "1 - Guardavías",
                "2 - Postes Delineadores",
                "3 - Barreras de Contención",
                "4 - Resaltos"
            )

            "20" -> listOf(
                "1 - Central",
                "2 - Lateral",
                "3 - Central y Lateral"
            )

            else ->
                listOf(
                    "1 - Otro"
                )
        }


    val materialOptions = listOf(
        "1 - Acero",
        "2 - Concreto",
        "3 - Mampostería",
        "4 - Plástico",
        "5 - Otro"
    )


    val conditionOptions =
        when (state.classCode) {

            "18" -> listOf(
                "1 - Buena · no tiene problema",
                "2 - Regular · todavía visible",
                "3 - Mala · apenas visible"
            )

            "20" -> listOf(
                "1 - Buena · no tiene problema",
                "2 - Regular · dañada o ausente en menos del 30%",
                "3 - Mala · dañada o ausente en más del 30%"
            )

            "19" -> listOf(
                "1 - Buena · no tiene problema",
                "2 - Regular · dañada o ausente en menos del 30%",
                "3 - Mala · dañada o ausente en más del 30%"
            )

            else ->
                listOf(
                    "1 - Buena",
                    "2 - Regular",
                    "3 - Mala"
                )
        }


    val classDescription =
        when (state.classCode) {

            "18" ->
                "18 - Señalización Horizontal - Marcas"

            "19" ->
                "19 - Seguridad"

            "20" ->
                "20 - Señalización Horizontal - Tachas"

            else ->
                state.classCode
        }


    SectionTitle(
        "SIC-21 · Seguridad y señalización horizontal"
    )

    Text(
        "Inventario Vial Calificado",
        fontWeight = FontWeight.Bold,
        style =
            MaterialTheme.typography.bodySmall
    )


    Text(
        text =
            "Clase: $classDescription",
        fontWeight =
            FontWeight.Bold
    )


    Text(
        "Tipo",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = typeOptions,
        selected = optionForCode(
            typeOptions,
            state.typeCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    typeCode =
                        codeFromOption(it)
                )
            )
        }
    )


    Text(
        "Material",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = materialOptions,
        selected = optionForCode(
            materialOptions,
            state.materialCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    materialCode =
                        codeFromOption(it)
                )
            )
        }
    )


    Text(
        "Condición",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = conditionOptions,
        selected = optionForCode(
            conditionOptions,
            state.conditionCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    conditionCode =
                        codeFromOption(it)
                )
            )
        }
    )
}


// =========================================================
// FORMULARIO GENERAL PARA ALCANTARILLA / CUNETA / BADÉN / PUENTE
// =========================================================

@Composable
fun AssetFormScreen(
    assetType: RoadAssetType,
    inventoryViewModel: InventoryViewModel,
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

    var capturedPhotos by remember { mutableStateOf<List<String>>(emptyList()) }
    var endLocation by remember { mutableStateOf<GeoLocation?>(null) }
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

    // =========================================================
    // ESTADOS SIC CALIFICADOS
    // =========================================================

    var sic17State by remember(assetType) {
        mutableStateOf(
            Sic17FormState()
        )
    }

    var sic18State by remember(assetType) {
        mutableStateOf(
            Sic18FormState()
        )
    }

    var sic19State by remember(assetType) {
        mutableStateOf(
            Sic19FormState()
        )
    }

    var sic20State by remember(assetType) {
        mutableStateOf(
            Sic20FormState()
        )
    }


    var sic23State by remember(assetType) { mutableStateOf(Sic23FormState()) }

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

            if (success) photoPath?.let { capturedPhotos = capturedPhotos + it }
            photoCaptured = capturedPhotos.isNotEmpty()
            photoPath = capturedPhotos.lastOrNull()

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

                RoadAssetType.RIGHT_OF_WAY -> "DERECHO_VIA"

                RoadAssetType.CULVERT ->
                    "ALCANTARILLA"

                RoadAssetType.DITCH ->
                    "CUNETA"

                RoadAssetType.FORD ->
                    "SIC20"

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
                assetType == RoadAssetType.BRIDGE ||
                assetType == RoadAssetType.RIGHT_OF_WAY

    val needsSide =
        assetType == RoadAssetType.DITCH ||
                assetType == RoadAssetType.FORD ||
                assetType == RoadAssetType.RIGHT_OF_WAY

    val sideOptions =
        when (assetType) {

            RoadAssetType.DITCH ->
                listOf(
                    "D - Derecho",
                    "I - Izquierdo"
                )

            RoadAssetType.FORD, RoadAssetType.RIGHT_OF_WAY ->
                listOf(
                    "D - Derecho",
                    "I - Izquierdo",
                    "S - Sin objeto"
                )

            else ->
                emptyList()
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

        item {
            Text("Fotografías del elemento: ${capturedPhotos.size}")
            capturedPhotos.forEachIndexed { index, path ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Fotografía ${index + 1}")
                    OutlinedButton(onClick = {
                        capturedPhotos = capturedPhotos - path
                        photoPath = capturedPhotos.lastOrNull()
                        photoCaptured = capturedPhotos.isNotEmpty()
                    }) { Text("Quitar") }
                }
            }
            FinalLocationCapture(endLocation) { endLocation = it }
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
                    options = sideOptions,
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

                RoadAssetType.RIGHT_OF_WAY -> Sic23Fields(sic23State) { sic23State = it }

                RoadAssetType.CULVERT -> {

                    Sic18Fields(
                        state = sic18State,
                        onStateChange = {
                            sic18State = it
                        }
                    )
                }

                RoadAssetType.DITCH -> {

                    Sic19Fields(
                        state = sic19State,
                        onStateChange = {
                            sic19State = it
                        }
                    )
                }

                RoadAssetType.FORD -> {

                    Sic20Fields(
                        state = sic20State,
                        onStateChange = {
                            sic20State = it
                        }
                    )
                }

                RoadAssetType.BRIDGE -> {

                    Sic17Fields(
                        state = sic17State,
                        onStateChange = {
                            sic17State = it
                        }
                    )
                }

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

                        startDistance.trim().isEmpty() -> {

                            formError =
                                "Debes ingresar la distancia desde el PR de inicio."
                        }


                        needsEndLocation &&
                                endPr.trim().isEmpty() -> {

                            formError =
                                "Debes ingresar el PR de fin."
                        }


                        needsEndLocation &&
                                endDistance.trim().isEmpty() -> {

                            formError =
                                "Debes ingresar la distancia desde el PR de fin."
                        }


                        // =========================================================
                        // VALIDACIÓN SIC-17
                        // =========================================================

                        assetType == RoadAssetType.BRIDGE &&
                                sic17State.dimension1LengthM
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar la longitud del puente."
                        }


                        assetType == RoadAssetType.BRIDGE &&
                                sic17State.dimension2LowerHeightM
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar la altura libre inferior."
                        }


                        assetType == RoadAssetType.BRIDGE &&
                                sic17State.dimension3UpperHeightM
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar la altura libre superior. Si no existe limitación, usa 00.00."
                        }


                        // =========================================================
                        // VALIDACIÓN SIC-18
                        // =========================================================

                        assetType == RoadAssetType.CULVERT &&
                                sic18State.spans
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar el número de ojos / vanos."
                        }


                        assetType == RoadAssetType.CULVERT &&
                                sic18State.dimension1M
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar la Dimensión 1."
                        }


                        assetType == RoadAssetType.CULVERT &&
                                sic18State.dimension2M
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar la Dimensión 2."
                        }


                        // =========================================================
                        // VALIDACIÓN SIC-20
                        // =========================================================

                        assetType == RoadAssetType.FORD &&
                                sic20State.dimension1M
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar la Dimensión 1."
                        }


                        assetType == RoadAssetType.FORD &&
                                sic20State.usesDimension2 &&
                                sic20State.dimension2M
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar la Dimensión 2."
                        }

                        else -> {
                            formError = null

                            val path = photoPath

                            if (path == null) {

                                formError =
                                    "No se encontró la fotografía."

                            } else {

                                val detail =
                                    when (assetType) {

                                        RoadAssetType.RIGHT_OF_WAY -> SicFormDetail.Sic23(sic23State)

                                        RoadAssetType.BRIDGE ->
                                            SicFormDetail.Sic17(
                                                sic17State
                                            )

                                        RoadAssetType.CULVERT ->
                                            SicFormDetail.Sic18(
                                                sic18State
                                            )

                                        RoadAssetType.DITCH ->
                                            SicFormDetail.Sic19(
                                                sic19State
                                            )

                                        RoadAssetType.FORD ->
                                            SicFormDetail.Sic20(
                                                sic20State
                                            )

                                        RoadAssetType.SIGNALIZATION -> {
                                            formError =
                                                "Utilice el módulo de señalización."
                                            return@Button
                                        }
                                    }

                                val actualAssetType =
                                    when (assetType) {

                                        RoadAssetType.RIGHT_OF_WAY -> sic23State.assetName

                                        RoadAssetType.BRIDGE ->
                                            "PUENTE"

                                        RoadAssetType.CULVERT ->
                                            "ALCANTARILLA"

                                        RoadAssetType.DITCH ->
                                            "DRENAJE"

                                        RoadAssetType.FORD ->
                                            when (
                                                sic20State.classCode
                                            ) {
                                                "12" -> "BADEN"
                                                "13" -> "TUNEL"
                                                "14" -> "MURO"
                                                else -> "SIC20"
                                            }

                                        RoadAssetType.SIGNALIZATION ->
                                            "SENALIZACION"
                                    }

                                inventoryViewModel.saveRecord(

                                    request =
                                        InventorySaveRequest(

                                            sicCode =
                                                assetType.sicCode,

                                            assetType =
                                                actualAssetType,

                                            routeCode = route,

                                            roadbedCode = roadbed,

                                            startPrCode = startPr,

                                            startDistanceM =
                                                startDistance,

                                            endPrCode =
                                                if (needsEndLocation) {
                                                    endPr
                                                } else {
                                                    null
                                                },

                                            endDistanceM =
                                                if (needsEndLocation) {
                                                    endDistance
                                                } else {
                                                    null
                                                },

                                            sideCode =
                                                if (needsSide) {
                                                    codeFromOption(
                                                        side
                                                    )
                                                } else {
                                                    null
                                                },

                                            latitude = latitude!!,

                                            longitude = longitude!!,

                                            gpsAccuracyM =
                                                gpsAccuracy,

                                            surveyDate =
                                                registrationDate,

                                            observations =
                                                observations,

                                            photoPath = capturedPhotos.first(),
                                            photoPaths = capturedPhotos,
                                            endLatitude = endLocation?.latitude,
                                            endLongitude = endLocation?.longitude,
                                            endGpsAccuracyM = endLocation?.accuracyHorizontal,

                                            detail = detail
                                        ),

                                    onSuccess = {
                                        onSave()
                                    },

                                    onError = { error ->
                                        formError = error
                                    }
                                )
                            }
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
// INVENTARIO VIAL CALIFICADO
// =========================================================

@Composable
fun Sic18Fields(
    state: Sic18FormState,
    onStateChange: (Sic18FormState) -> Unit
) {

    val classOptions = listOf(
        "06 - Alcantarilla Definitiva",
        "07 - Alcantarilla Estructura Artesanal"
    )

    val typeOptions =
        if (state.classCode == "06") {

            listOf(
                "1 - Concreto",
                "2 - Mampostería",
                "3 - Acero",
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

    val sectionOptions = listOf(
        "1 - Marco",
        "2 - Circular / Ovalada",
        "3 - Arco",
        "4 - Pórtico",
        "5 - Otro"
    )

    val structuralOptions = listOf(
        "1 - Buena",
        "2 - Regular",
        "3 - Mala"
    )

    val functionalOptions = listOf(
        "1 - Buena · limpia",
        "2 - Regular · parcialmente obstruida",
        "3 - Mala · totalmente obstruida"
    )


    SectionTitle(
        "SIC-18 · Alcantarillas"
    )

    Text(
        "Inventario Vial Calificado",
        fontWeight = FontWeight.Bold,
        style =
            MaterialTheme.typography.bodySmall
    )


    Text(
        "Clase",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = classOptions,
        selected = optionForCode(
            classOptions,
            state.classCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    classCode =
                        codeFromOption(it),
                    typeCode = "1"
                )
            )
        }
    )


    Text(
        "Tipo / material",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = typeOptions,
        selected = optionForCode(
            typeOptions,
            state.typeCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    typeCode =
                        codeFromOption(it)
                )
            )
        }
    )


    OutlinedTextField(
        value = state.spans,
        onValueChange = { value ->

            onStateChange(
                state.copy(
                    spans =
                        value.filter {
                            it.isDigit()
                        }
                )
            )
        },
        label = {
            Text(
                "Número de ojos / vanos"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )


    Text(
        "Sección transversal",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = sectionOptions,
        selected = optionForCode(
            sectionOptions,
            state.crossSectionCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    crossSectionCode =
                        codeFromOption(it)
                )
            )
        }
    )


    OutlinedTextField(
        value = state.dimension1M,
        onValueChange = {

            onStateChange(
                state.copy(
                    dimension1M = it
                )
            )
        },
        label = {
            Text(
                "Dimensión 1 · ancho o diámetro (m)"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )


    OutlinedTextField(
        value = state.dimension2M,
        onValueChange = {

            onStateChange(
                state.copy(
                    dimension2M = it
                )
            )
        },
        label = {
            Text(
                "Dimensión 2 · altura (m)"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )


    Text(
        "Condición estructural",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = structuralOptions,
        selected = optionForCode(
            structuralOptions,
            state.structuralConditionCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    structuralConditionCode =
                        codeFromOption(it)
                )
            )
        }
    )


    Text(
        "Condición funcional",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = functionalOptions,
        selected = optionForCode(
            functionalOptions,
            state.functionalConditionCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    functionalConditionCode =
                        codeFromOption(it)
                )
            )
        }
    )
}


// =========================================================
// SIC-19 · CUNETAS Y DRENAJE
// INVENTARIO VIAL CALIFICADO
// =========================================================

@Composable
fun Sic19Fields(
    state: Sic19FormState,
    onStateChange: (Sic19FormState) -> Unit
) {

    val classOptions = listOf(
        "08 - Cuneta",
        "09 - Canal",
        "10 - Bajada de Agua",
        "11 - Zanja de Drenaje",
        "12 - Zanja de Coronación",
        "13 - Cuneta de Banqueta"
    )

    val typeOptions = listOf(
        "1 - Tierra",
        "2 - Concreto",
        "3 - Mampostería",
        "4 - Otro"
    )

    val sectionOptions = listOf(
        "1 - Triangular",
        "2 - Trapezoidal",
        "3 - Rectangular",
        "4 - Otro"
    )

    val structuralOptions = listOf(
        "1 - Buena",
        "2 - Regular",
        "3 - Mala"
    )

    val functionalOptions = listOf(
        "1 - Buena · limpia",
        "2 - Regular · parcialmente obstruida",
        "3 - Mala · totalmente obstruida"
    )


    SectionTitle(
        "SIC-19 · Cunetas y drenaje"
    )

    Text(
        "Inventario Vial Calificado",
        fontWeight = FontWeight.Bold,
        style =
            MaterialTheme.typography.bodySmall
    )


    Text(
        "Clase",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = classOptions,
        selected = optionForCode(
            classOptions,
            state.classCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    classCode =
                        codeFromOption(it)
                )
            )
        }
    )


    Text(
        "Tipo",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = typeOptions,
        selected = optionForCode(
            typeOptions,
            state.typeCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    typeCode =
                        codeFromOption(it)
                )
            )
        }
    )


    Text(
        "Sección transversal",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = sectionOptions,
        selected = optionForCode(
            sectionOptions,
            state.crossSectionCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    crossSectionCode =
                        codeFromOption(it)
                )
            )
        }
    )


    Text(
        "Condición estructural",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = structuralOptions,
        selected = optionForCode(
            structuralOptions,
            state.structuralConditionCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    structuralConditionCode =
                        codeFromOption(it)
                )
            )
        }
    )


    Text(
        "Condición funcional",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = functionalOptions,
        selected = optionForCode(
            functionalOptions,
            state.functionalConditionCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    functionalConditionCode =
                        codeFromOption(it)
                )
            )
        }
    )


    Text(
        text =
            "La ubicación inicio y fin identifican el tramo. La longitud curva se calculará posteriormente a partir de la geometría/trayectoria.",
        style =
            MaterialTheme.typography.bodySmall
    )
}


// =========================================================
// SIC-20 · BADENES, TÚNELES Y MUROS
// INVENTARIO VIAL CALIFICADO
// =========================================================

@Composable
fun Sic20Fields(
    state: Sic20FormState,
    onStateChange: (Sic20FormState) -> Unit
) {

    val classOptions = listOf(
        "12 - Badén",
        "13 - Túnel",
        "14 - Muro"
    )

    val typeOptions =
        when (state.classCode) {

            "12" -> listOf(
                "1 - Gavión",
                "2 - Concreto",
                "3 - Mampostería",
                "4 - Concreto ciclópeo",
                "5 - Piedra",
                "6 - Otro"
            )

            "13" -> listOf(
                "1 - Concreto",
                "2 - Mampostería",
                "3 - Concreto ciclópeo",
                "4 - Roca",
                "5 - Otro"
            )

            "14" -> listOf(
                "1 - Gavión",
                "2 - Concreto",
                "3 - Mampostería",
                "4 - Concreto ciclópeo",
                "5 - Piedra",
                "6 - Otro"
            )

            else ->
                listOf(
                    "1 - Otro"
                )
        }


    val structuralOptions = listOf(
        "1 - Buena · no tiene problema",
        "2 - Regular · puede tener problemas",
        "3 - Mala · necesita repararse"
    )

    val functionalOptions = listOf(
        "1 - Buena · limpia",
        "2 - Regular · parcialmente obstruida",
        "3 - Mala · totalmente obstruida"
    )


    SectionTitle(
        "SIC-20 · Badenes, túneles y muros"
    )

    Text(
        "Inventario Vial Calificado",
        fontWeight = FontWeight.Bold,
        style =
            MaterialTheme.typography.bodySmall
    )


    Text(
        "Clase",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = classOptions,
        selected = optionForCode(
            classOptions,
            state.classCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    classCode =
                        codeFromOption(it),

                    typeCode =
                        if (
                            codeFromOption(it) ==
                            "12"
                        )
                            "2"
                        else
                            "1",

                    dimension2M = ""
                )
            )
        }
    )


    Text(
        "Tipo",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = typeOptions,
        selected = optionForCode(
            typeOptions,
            state.typeCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    typeCode =
                        codeFromOption(it)
                )
            )
        }
    )


    OutlinedTextField(
        value = state.dimension1M,
        onValueChange = {

            onStateChange(
                state.copy(
                    dimension1M = it
                )
            )
        },
        label = {

            Text(
                when (state.classCode) {

                    "12" ->
                        "Dimensión 1 · ancho de rodadura (m)"

                    "13" ->
                        "Dimensión 1 · ancho del túnel (m)"

                    "14" ->
                        "Dimensión 1 · altura promedio del muro (m)"

                    else ->
                        "Dimensión 1 (m)"
                }
            )
        },

        modifier =
            Modifier.fillMaxWidth()
    )


    if (state.usesDimension2) {

        OutlinedTextField(
            value =
                state.dimension2M,

            onValueChange = {

                onStateChange(
                    state.copy(
                        dimension2M = it
                    )
                )
            },

            label = {

                Text(
                    when (state.classCode) {

                        "12" ->
                            "Dimensión 2 · ancho total con protección contra erosión (m)"

                        "13" ->
                            "Dimensión 2 · altura útil (m)"

                        else ->
                            "Dimensión 2 (m)"
                    }
                )
            },

            modifier =
                Modifier.fillMaxWidth()
        )
    }


    Text(
        "Condición estructural",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = structuralOptions,
        selected = optionForCode(
            structuralOptions,
            state.structuralConditionCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    structuralConditionCode =
                        codeFromOption(it)
                )
            )
        }
    )


    if (state.usesFunctionalCondition) {

        Text(
            "Condición funcional",
            fontWeight = FontWeight.Bold
        )

        ChoiceSelector(
            options = functionalOptions,
            selected = optionForCode(
                functionalOptions,
                state.functionalConditionCode
            ),
            onSelected = {

                onStateChange(
                    state.copy(
                        functionalConditionCode =
                            codeFromOption(it)
                    )
                )
            }
        )
    }
}


// =========================================================
// SIC-17 · PUENTES
// INVENTARIO VIAL CALIFICADO
// =========================================================

@Composable
fun Sic17Fields(
    state: Sic17FormState,
    onStateChange: (Sic17FormState) -> Unit
) {

    val classOptions = listOf(
        "01 - Puente Definitivo",
        "02 - Puente Provisional",
        "03 - Estructura Artesanal",
        "04 - Puente Histórico"
    )

    val typeOptions =
        when (state.classCode) {

            "01" -> listOf(
                "1 - Losa",
                "2 - Losa con Vigas",
                "3 - Celular estilo Alcantarilla",
                "4 - Pórtico",
                "5 - Reticulado",
                "6 - Arco",
                "7 - Atirantado",
                "8 - Colgante",
                "9 - Otro"
            )

            "02" -> listOf(
                "1 - Modular",
                "2 - Yawata",
                "3 - Otro"
            )

            "03" -> listOf(
                "1 - Vigas de Troncos de Madera",
                "2 - Vigas de Rieles de Ferrocarril",
                "3 - Otro"
            )

            "04" -> listOf(
                "1 - Mampostería de Piedra",
                "2 - Otro"
            )

            else -> listOf(
                "1 - Otro"
            )
        }

    val inventoriedOptions = listOf(
        "S - Sí",
        "N - No"
    )

    val structuralOptions = listOf(
        "1 - Buena",
        "2 - Regular",
        "3 - Mala"
    )

    val functionalOptions = listOf(
        "1 - Buena · limpia",
        "2 - Regular · parcialmente obstruida",
        "3 - Mala · totalmente obstruida"
    )

    val serviceOptions = listOf(
        "0 - Fuera de servicio",
        "1 - Vehicular",
        "2 - Ferroviario",
        "3 - Peatonal",
        "4 - Otro"
    )

    val singularityOptions = listOf(
        "1 - Río",
        "2 - Quebrada",
        "3 - Canal",
        "4 - Camino",
        "5 - Vía Férrea",
        "6 - Otro"
    )


    SectionTitle(
        "SIC-17 · Puentes"
    )

    Text(
        "Inventario Vial Calificado",
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.bodySmall
    )


    Text(
        "Clase",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = classOptions,
        selected = optionForCode(
            classOptions,
            state.classCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    classCode =
                        codeFromOption(it),

                    typeCode = "1"
                )
            )
        }
    )


    Text(
        "Tipo",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = typeOptions,
        selected = optionForCode(
            typeOptions,
            state.typeCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    typeCode =
                        codeFromOption(it)
                )
            )
        }
    )


    OutlinedTextField(
        value = state.bridgeCode,
        onValueChange = {

            onStateChange(
                state.copy(
                    bridgeCode =
                        it.uppercase()
                )
            )
        },
        label = {
            Text("Código del puente")
        },
        modifier =
            Modifier.fillMaxWidth()
    )


    Text(
        "Inventariado",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = inventoriedOptions,
        selected = optionForCode(
            inventoriedOptions,
            state.inventoriedCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    inventoriedCode =
                        codeFromOption(it)
                )
            )
        }
    )


    OutlinedTextField(
        value = state.spans,
        onValueChange = { value ->

            onStateChange(
                state.copy(
                    spans =
                        value.filter {
                            it.isDigit()
                        }
                )
            )
        },
        label = {
            Text("Número de vanos")
        },
        modifier =
            Modifier.fillMaxWidth()
    )


    OutlinedTextField(
        value =
            state.dimension1LengthM,
        onValueChange = {

            onStateChange(
                state.copy(
                    dimension1LengthM = it
                )
            )
        },
        label = {
            Text(
                "Dimensión 1 · longitud total (m)"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )


    OutlinedTextField(
        value =
            state.dimension2LowerHeightM,
        onValueChange = {

            onStateChange(
                state.copy(
                    dimension2LowerHeightM = it
                )
            )
        },
        label = {
            Text(
                "Dimensión 2 · altura libre inferior (m)"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )


    Text(
        "Condición estructural",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = structuralOptions,
        selected = optionForCode(
            structuralOptions,
            state.structuralConditionCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    structuralConditionCode =
                        codeFromOption(it)
                )
            )
        }
    )


    Text(
        "Condición funcional",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = functionalOptions,
        selected = optionForCode(
            functionalOptions,
            state.functionalConditionCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    functionalConditionCode =
                        codeFromOption(it)
                )
            )
        }
    )


    Text(
        "Tipo de servicio",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = serviceOptions,
        selected = optionForCode(
            serviceOptions,
            state.serviceTypeCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    serviceTypeCode =
                        codeFromOption(it)
                )
            )
        }
    )


    Text(
        "Singularidad salvada",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = singularityOptions,
        selected = optionForCode(
            singularityOptions,
            state.singularityCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    singularityCode =
                        codeFromOption(it)
                )
            )
        }
    )


    OutlinedTextField(
        value =
            state.singularityName,
        onValueChange = {

            onStateChange(
                state.copy(
                    singularityName =
                        it.uppercase()
                )
            )
        },
        label = {
            Text(
                "Nombre de la singularidad"
            )
        },
        placeholder = {
            Text(
                "Ej. RÍO RÍMAC"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )


    OutlinedTextField(
        value =
            state.dimension3UpperHeightM,
        onValueChange = {

            onStateChange(
                state.copy(
                    dimension3UpperHeightM = it
                )
            )
        },
        label = {
            Text(
                "Dimensión 3 · altura libre superior (m)"
            )
        },
        supportingText = {
            Text(
                "Si no existe limitación de altura, el manual indica 00.00."
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )
}



// =========================================================
// CAPTURA DE UBICACIÓN GPS FINAL
// =========================================================



// =========================================================
// COMPONENTES AUXILIARES
// =========================================================
internal fun codeFromOption(
    option: String
): String {

    return option
        .substringBefore(" - ")
        .trim()
}


internal fun optionForCode(
    options: List<String>,
    code: String
): String {

    return options.firstOrNull {
        codeFromOption(it) == code
    } ?: options.first()
}
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

        HomeScreen(
            recordsToday = 0,
            pendingSync = 0,
            onAssetSelected = {}
        )
    }
}
