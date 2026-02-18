package com.example.baiturrahman.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.baiturrahman.ui.components.SupabaseImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.baiturrahman.BaiturrahmanApp
import com.example.baiturrahman.ui.theme.DarkBackground
import com.example.baiturrahman.ui.theme.DarkSurface
import com.example.baiturrahman.ui.theme.EmeraldGreen
import com.example.baiturrahman.ui.theme.EmeraldLight
import com.example.baiturrahman.ui.theme.GlassBorder
import com.example.baiturrahman.ui.theme.TextPrimary
import com.example.baiturrahman.ui.theme.TextSecondary
import com.example.baiturrahman.ui.viewmodel.MosqueDashboardViewModel
import com.example.baiturrahman.utils.DevicePreferences
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    viewModel: MosqueDashboardViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var quoteText by remember { mutableStateOf(viewModel.quoteText.value) }
    var mosqueName by remember { mutableStateOf(viewModel.mosqueName.value) }
    var mosqueLocation by remember { mutableStateOf(viewModel.mosqueLocation.value) }
    var marqueeText by remember { mutableStateOf(viewModel.marqueeText.value) }
    val mosqueImages by viewModel.mosqueImages.collectAsState()

    var prayerAddress by remember { mutableStateOf(viewModel.prayerAddress.value) }
    var prayerTimezone by remember { mutableStateOf(viewModel.prayerTimezone.value) }
    var timezoneMenuExpanded by remember { mutableStateOf(false) }

    val mosqueNameCharCount = mosqueName.length
    val mosqueLocationCharCount = mosqueLocation.length
    val quoteTextCharCount = quoteText.length
    val marqueeTextCharCount = marqueeText.length

    val isSaving by viewModel.isSaving.collectAsState()
    val isUploadingImage by viewModel.isUploadingImage.collectAsState()
    val isUploadingLogo by viewModel.isUploadingLogo.collectAsState()
    val isDeletingImage by viewModel.isDeletingImage.collectAsState()

    val logoImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.updateLogoImage(it.toString())
        }
    }

    val mosqueImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.addMosqueImage(it.toString())
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            mosqueImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Izin penyimpanan diperlukan untuk memilih gambar")
            }
        }
    }

    fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                mosqueImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
            }
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                mosqueImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            }
        }
    }

    val devicePreferences = koinInject<DevicePreferences>()
    var isMasterDevice by remember { mutableStateOf(devicePreferences.isMasterDevice) }
    var deviceName by remember { mutableStateOf(devicePreferences.deviceName) }
    val syncService = (context.applicationContext as BaiturrahmanApp).syncService

    // Device name dropdown state
    val deviceNames by viewModel.deviceNames.collectAsState()
    var deviceNameMenuExpanded by remember { mutableStateOf(false) }
    var isAddingNewDevice by remember { mutableStateOf(false) }
    // Shared OutlinedTextField colors for dark theme
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = EmeraldGreen,
        unfocusedBorderColor = GlassBorder,
        focusedLabelColor = EmeraldGreen,
        unfocusedLabelColor = TextSecondary,
        cursorColor = EmeraldGreen,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
    )

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Admin") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary,
                    actionIconContentColor = TextPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sync Settings Section
            AdminSection(
                title = "Pengaturan Sinkronisasi",
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Device Name
                        Column {
                            Text(
                                text = "Nama Perangkat:",
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            if (isAddingNewDevice) {
                                OutlinedTextField(
                                    value = deviceName,
                                    onValueChange = { deviceName = it },
                                    label = { Text("Nama Perangkat Baru") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextButton(
                                        onClick = {
                                            isAddingNewDevice = false
                                            // Reset to current saved name if user cancels
                                            deviceName = devicePreferences.deviceName
                                        }
                                    ) {
                                        Text("Batal", color = Color.Red)
                                    }
                                    TextButton(
                                        onClick = {
                                            if (deviceName.isNotBlank()) {
                                                devicePreferences.deviceName = deviceName
                                                isAddingNewDevice = false
                                                viewModel.loadDeviceNames()
                                            }
                                        },
                                        enabled = deviceName.isNotBlank()
                                    ) {
                                        Text("Simpan", color = if (deviceName.isNotBlank()) EmeraldGreen else Color.Gray)
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                            .clickable { deviceNameMenuExpanded = true }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(deviceName.ifEmpty { "Pilih Perangkat" })
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Pilih Perangkat"
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = deviceNameMenuExpanded,
                                        onDismissRequest = { deviceNameMenuExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        deviceNames.forEach { name ->
                                            DropdownMenuItem(
                                                text = { Text(name) },
                                                onClick = {
                                                    deviceName = name
                                                    deviceNameMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        isAddingNewDevice = true
                                        deviceName = ""
                                    }
                                ) {
                                    Text("+ Tambah Perangkat Baru", color = EmeraldGreen)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Perangkat Utama (dapat mengubah TV lain)", color = TextPrimary)
                            androidx.compose.material3.Switch(
                                checked = isMasterDevice,
                                onCheckedChange = { isMasterDevice = it }
                            )
                        }

                        // Info text
                        Text(
                            text = "Perangkat utama dapat mengubah pengaturan untuk semua TV. Perangkat non-utama menerima pembaruan secara otomatis dari perangkat utama.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            )

            if (isMasterDevice) {
                // Header Section
                AdminSection(
                    title = "Pengaturan Header",
                    content = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = mosqueName,
                                onValueChange = {
                                    if (it.length <= 35) mosqueName = it
                                },
                                label = { Text("Nama Masjid") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = textFieldColors,
                                supportingText = {
                                    Text(
                                        text = "$mosqueNameCharCount/35",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.End,
                                        color = if (mosqueNameCharCount >= 35) Color.Red else TextSecondary
                                    )
                                },
                                isError = mosqueNameCharCount >= 35
                            )

                            OutlinedTextField(
                                value = mosqueLocation,
                                onValueChange = {
                                    if (it.length <= 25) mosqueLocation = it
                                },
                                label = { Text("Lokasi Masjid") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = textFieldColors,
                                supportingText = {
                                    Text(
                                        text = "$mosqueLocationCharCount/25",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.End,
                                        color = if (mosqueLocationCharCount >= 25) Color.Red else TextSecondary
                                    )
                                },
                                isError = mosqueLocationCharCount >= 25
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Logo Masjid:",
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )

                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            val hasPermission = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.READ_MEDIA_IMAGES
                                            ) == PackageManager.PERMISSION_GRANTED

                                            if (hasPermission) {
                                                logoImageLauncher.launch("image/*")
                                            } else {
                                                requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                                            }
                                        } else {
                                            val hasPermission = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.READ_EXTERNAL_STORAGE
                                            ) == PackageManager.PERMISSION_GRANTED

                                            if (hasPermission) {
                                                logoImageLauncher.launch("image/*")
                                            } else {
                                                requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                                            }
                                        }
                                    },
                                    enabled = !isUploadingLogo,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldGreen
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isUploadingLogo) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Mengupload...")
                                    } else {
                                        Icon(Icons.Default.Edit, contentDescription = "Ubah Logo")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Ubah Logo")
                                    }
                                }
                            }
                        }
                    }
                )

                // Prayer API Settings Section
                AdminSection(
                    title = "Pengaturan Waktu Sholat",
                    content = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = prayerAddress,
                                onValueChange = { prayerAddress = it },
                                label = { Text("Alamat Waktu Sholat") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = textFieldColors,
                                placeholder = { Text("contoh: Lebak Bulus, Jakarta, ID", color = TextSecondary) }
                            )

                            Column {
                                Text(
                                    text = "Zona Waktu:",
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, GlassBorder, RoundedCornerShape(4.dp))
                                            .clickable { timezoneMenuExpanded = true }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(prayerTimezone, color = TextPrimary)
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Pilih Zona Waktu",
                                            tint = TextSecondary
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = timezoneMenuExpanded,
                                        onDismissRequest = { timezoneMenuExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        viewModel.availableTimezones.forEach { timezone ->
                                            DropdownMenuItem(
                                                text = { Text(timezone) },
                                                onClick = {
                                                    prayerTimezone = timezone
                                                    timezoneMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                )

                // Quote Section
                AdminSection(
                    title = "Pengaturan Kutipan",
                    content = {
                        OutlinedTextField(
                            value = quoteText,
                            onValueChange = {
                                if (it.length <= 100) quoteText = it
                            },
                            label = { Text("Teks Kutipan") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = textFieldColors,
                            minLines = 3,
                            supportingText = {
                                Text(
                                    text = "$quoteTextCharCount/100",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End,
                                    color = if (quoteTextCharCount >= 100) Color.Red else TextSecondary
                                )
                            },
                            isError = quoteTextCharCount >= 100
                        )
                    }
                )

                // Mosque Images Section
                AdminSection(
                    title = "Slide Gambar (640 x 410) (Maks 5)",
                    content = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Gambar yang diupload akan otomatis tersimpan dan disinkronkan ke database.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )

                            if (mosqueImages.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                ) {
                                    itemsIndexed(mosqueImages) { index, imageUri ->
                                        Box(
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, EmeraldGreen, RoundedCornerShape(8.dp))
                                        ) {
                                            SupabaseImage(
                                                imageUrl = imageUri,
                                                contentDescription = "Gambar Masjid ${index + 1}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                fallbackResourceId = null
                                            )

                                            IconButton(
                                                onClick = {
                                                    viewModel.removeMosqueImage(index)
                                                },
                                                enabled = !isDeletingImage && !isUploadingImage,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(32.dp)
                                                    .background(
                                                        DarkBackground.copy(alpha = 0.7f),
                                                        CircleShape
                                                    )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Hapus Gambar",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(4.dp)
                                                    .background(
                                                        DarkBackground.copy(alpha = 0.7f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    color = TextPrimary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (mosqueImages.size < 5) {
                                Button(
                                    onClick = {
                                        checkAndRequestPermissions()
                                    },
                                    enabled = !isUploadingImage,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldGreen
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isUploadingImage) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Mengupload...")
                                    } else {
                                        Icon(Icons.Default.Add, contentDescription = "Tambah Gambar Masjid")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Tambah Gambar (${mosqueImages.size}/5)")
                                    }
                                }
                            } else {
                                Text(
                                    text = "Jumlah maksimum gambar tercapai (5/5)",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                )

                // Marquee Text Section
                AdminSection(
                    title = "Teks Berjalan",
                    content = {
                        OutlinedTextField(
                            value = marqueeText,
                            onValueChange = {
                                if (it.length <= 100) marqueeText = it
                            },
                            label = { Text("Teks Berjalan") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = textFieldColors,
                            supportingText = {
                                Text(
                                    text = "$marqueeTextCharCount/100",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End,
                                    color = if (marqueeTextCharCount >= 100) Color.Red else TextSecondary
                                )
                            },
                            isError = marqueeTextCharCount >= 100
                        )
                    }
                )

                // Save Button
                Button(
                    onClick = {
                        val oldName = devicePreferences.deviceName
                        val newName = deviceName

                        scope.launch {
                            if (oldName != newName && newName.isNotBlank()) {
                                val success = viewModel.renameDevice(oldName, newName)
                                if (!success) {
                                    snackbarHostState.showSnackbar("Gagal mengubah nama perangkat")
                                    return@launch
                                }
                            }

                            viewModel.updateQuoteText(quoteText)
                            viewModel.updateMosqueName(mosqueName)
                            viewModel.updateMosqueLocation(mosqueLocation)
                            viewModel.updateMarqueeText(marqueeText)
                            viewModel.updatePrayerAddress(prayerAddress)
                            viewModel.updatePrayerTimezone(prayerTimezone)

                            devicePreferences.deviceName = newName
                            devicePreferences.isMasterDevice = isMasterDevice

                            syncService.stopSync()
                            syncService.startSync()

                            viewModel.saveAllSettings()
                            onClose()
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Menyimpan...")
                    } else {
                        Icon(Icons.Default.Check, contentDescription = "Simpan Perubahan")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan Perubahan")
                    }
                }
            } else {
                // Non-master device sections
                AdminSection(
                    title = "Pengaturan Perangkat",
                    content = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Perangkat ini dikonfigurasi sebagai perangkat non-utama. Perangkat ini akan menerima pembaruan dari perangkat utama tetapi tidak dapat membuat perubahan pada pengaturan masjid.",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )

                            Text(
                                text = "Untuk membuat perubahan, pilih salah satu:",
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )

                            Text(
                                text = "\u2022 Aktifkan 'Perangkat Utama' di atas untuk memungkinkan perangkat ini mengontrol pengaturan",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )

                            Text(
                                text = "\u2022 Gunakan perangkat utama yang ditunjuk untuk membuat perubahan",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                )

                // Save Button for sync settings only
                Button(
                    onClick = {
                        val oldName = devicePreferences.deviceName
                        val newName = deviceName

                        scope.launch {
                            if (oldName != newName && newName.isNotBlank()) {
                                val success = viewModel.renameDevice(oldName, newName)
                                if (!success) {
                                    snackbarHostState.showSnackbar("Gagal mengubah nama perangkat")
                                    return@launch
                                }
                            }

                            devicePreferences.deviceName = newName
                            devicePreferences.isMasterDevice = isMasterDevice

                            syncService.stopSync()
                            syncService.startSync()

                            onClose()
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Menyimpan...")
                    } else {
                        Icon(Icons.Default.Check, contentDescription = "Simpan Pengaturan Sinkronisasi")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan Pengaturan Sinkronisasi")
                    }
                }
            }

        }
    }
}

@Composable
fun AdminSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EmeraldLight
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(
                containerColor = DarkSurface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }
}
