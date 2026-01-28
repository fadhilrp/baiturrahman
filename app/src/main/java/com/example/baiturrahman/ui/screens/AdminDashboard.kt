package com.example.baiturrahman.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import com.example.baiturrahman.R
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.core.net.toUri
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.example.baiturrahman.BaiturrahmanApp
import com.example.baiturrahman.ui.theme.emeraldGreen
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

    // Local state for form fields
    var quoteText by remember { mutableStateOf(viewModel.quoteText.value) }
    var mosqueName by remember { mutableStateOf(viewModel.mosqueName.value) }
    var mosqueLocation by remember { mutableStateOf(viewModel.mosqueLocation.value) }
    var marqueeText by remember { mutableStateOf(viewModel.marqueeText.value) }
    val mosqueImages by viewModel.mosqueImages.collectAsState()

    // Prayer API settings
    var prayerAddress by remember { mutableStateOf(viewModel.prayerAddress.value) }
    var prayerTimezone by remember { mutableStateOf(viewModel.prayerTimezone.value) }
    var timezoneMenuExpanded by remember { mutableStateOf(false) }

    // Character count states
    val mosqueNameCharCount = mosqueName.length
    val mosqueLocationCharCount = mosqueLocation.length
    val quoteTextCharCount = quoteText.length
    val marqueeTextCharCount = marqueeText.length

    // Manual sync state
    var isSyncing by remember { mutableStateOf(false) }

    // Image picker launchers
    val logoImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                snackbarHostState.showSnackbar("Mengupload logo... Periksa logcat untuk detail")
            }
            viewModel.updateLogoImage(it.toString())
        }
    }

    val mosqueImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            scope.launch {
                snackbarHostState.showSnackbar("Mengupload gambar masjid... Periksa logcat untuk detail")
            }
            viewModel.addMosqueImage(it.toString())
        }
    }

    // Permission launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // All permissions granted, launch image picker
            mosqueImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            // Show a message that permissions are required
            scope.launch {
                snackbarHostState.showSnackbar("Izin penyimpanan diperlukan untuk memilih gambar")
            }
        }
    }

    // Function to check and request permissions
    fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ (API 33+), use READ_MEDIA_IMAGES
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
            // For Android 6-12, use READ_EXTERNAL_STORAGE
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

    // Use koinInject() to get DevicePreferences
    val devicePreferences = koinInject<DevicePreferences>()
    var isMasterDevice by remember { mutableStateOf(devicePreferences.isMasterDevice) }
    var deviceName by remember { mutableStateOf(devicePreferences.deviceName) }
    var syncEnabled by remember { mutableStateOf(devicePreferences.syncEnabled) }
    val syncService = (context.applicationContext as BaiturrahmanApp).syncService

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Admin") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    // Debug Supabase button
                    IconButton(
                        onClick = {
                            viewModel.debugSupabaseConnection()
                            scope.launch {
                                snackbarHostState.showSnackbar("Tes koneksi Supabase - periksa logcat")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Build, contentDescription = "Debug Supabase")
                    }

                    // Add Force Sync button
                    IconButton(
                        onClick = {
                            scope.launch {
                                syncService.forceSyncNow()
                                snackbarHostState.showSnackbar("Paksa sinkronisasi dipicu - periksa log")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Paksa Sinkronisasi")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = emeraldGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
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
            // Debug Info Section - Always show this
            AdminSection(
                title = "Info Debug & Koneksi",
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Perangkat: ${devicePreferences.deviceName}")
                        Text("Perangkat Utama: ${if (devicePreferences.isMasterDevice) "Ya" else "Tidak"}")
                        Text("Sinkronisasi Aktif: ${if (devicePreferences.syncEnabled) "Ya" else "Tidak"}")

                        // Debug buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.debugSupabaseConnection()
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Tes koneksi Supabase dimulai - periksa logcat dengan filter 'ImageRepository'")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                            ) {
                                Text("Tes Supabase", color = Color.White)
                            }
                        }

                        Text(
                            text = "💡 Tip: Buka logcat dan filter dengan 'ImageRepository' atau 'MosqueDashboardViewModel' untuk melihat log detail upload",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            )

            // Sync Settings Section
            AdminSection(
                title = "Pengaturan Sinkronisasi",
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Device Name
                        OutlinedTextField(
                            value = deviceName,
                            onValueChange = { deviceName = it },
                            label = { Text("Nama Perangkat") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Master Device Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Perangkat Utama (dapat mengubah TV lain)")
                            androidx.compose.material3.Switch(
                                checked = isMasterDevice,
                                onCheckedChange = { isMasterDevice = it }
                            )
                        }

                        // Sync Enabled Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Aktifkan Sinkronisasi")
                            androidx.compose.material3.Switch(
                                checked = syncEnabled,
                                onCheckedChange = { syncEnabled = it }
                            )
                        }

                        // Info text
                        Text(
                            text = "Ketika sinkronisasi diaktifkan, perangkat ini akan menerima pembaruan dari perangkat utama. Jika ini adalah perangkat utama, perubahan yang dibuat di sini akan memperbarui semua TV lain.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            )

            // Only show the sections below if Master Device is enabled
            if (isMasterDevice) {
                // Header Section
                AdminSection(
                    title = "Pengaturan Header",
                    content = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Mosque Name with character limit
                            OutlinedTextField(
                                value = mosqueName,
                                onValueChange = {
                                    if (it.length <= 35) mosqueName = it
                                },
                                label = { Text("Nama Masjid") },
                                modifier = Modifier.fillMaxWidth(),
                                supportingText = {
                                    Text(
                                        text = "$mosqueNameCharCount/35",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.End,
                                        color = if (mosqueNameCharCount >= 35) Color.Red else Color.Gray
                                    )
                                },
                                isError = mosqueNameCharCount >= 35
                            )

                            // Mosque Location with character limit
                            OutlinedTextField(
                                value = mosqueLocation,
                                onValueChange = {
                                    if (it.length <= 25) mosqueLocation = it
                                },
                                label = { Text("Lokasi Masjid") },
                                modifier = Modifier.fillMaxWidth(),
                                supportingText = {
                                    Text(
                                        text = "$mosqueLocationCharCount/25",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.End,
                                        color = if (mosqueLocationCharCount >= 25) Color.Red else Color.Gray
                                    )
                                },
                                isError = mosqueLocationCharCount >= 25
                            )

                            // Logo Image
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Logo Masjid:",
                                    fontWeight = FontWeight.Medium
                                )

                                Button(
                                    onClick = {
                                        // Check permissions for logo image too
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            val hasPermission = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.READ_MEDIA_IMAGES
                                            ) == PackageManager.PERMISSION_GRANTED

                                            if (hasPermission) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Mengupload logo... Periksa logcat untuk detail")
                                                }
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
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Mengupload logo... Periksa logcat untuk detail")
                                                }
                                                logoImageLauncher.launch("image/*")
                                            } else {
                                                requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = emeraldGreen
                                    )
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Ubah Logo")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ubah Logo")
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
                            // Address Input
                            OutlinedTextField(
                                value = prayerAddress,
                                onValueChange = { prayerAddress = it },
                                label = { Text("Alamat Waktu Sholat") },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("contoh: Lebak Bulus, Jakarta, ID") }
                            )

                            // Timezone Dropdown
                            Column {
                                Text(
                                    text = "Zona Waktu:",
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                            .clickable { timezoneMenuExpanded = true }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(prayerTimezone)
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Pilih Zona Waktu"
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

                // Quote Section with character limit
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
                            minLines = 3,
                            supportingText = {
                                Text(
                                    text = "$quoteTextCharCount/100",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End,
                                    color = if (quoteTextCharCount >= 100) Color.Red else Color.Gray
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
                            // Current images
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
                                                .border(1.dp, emeraldGreen, RoundedCornerShape(8.dp))
                                        ) {
                                            // Use SupabaseImage for better error handling
                                            SupabaseImage(
                                                imageUrl = imageUri,
                                                contentDescription = "Gambar Masjid ${index + 1}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                fallbackResourceId = R.drawable.mosque
                                            )

                                            // Delete button
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Menghapus gambar... Periksa logcat untuk detail")
                                                    }
                                                    viewModel.removeMosqueImage(index)
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(32.dp)
                                                    .background(
                                                        Color.White.copy(alpha = 0.7f),
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

                                            // Image number
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(4.dp)
                                                    .background(
                                                        Color.Black.copy(alpha = 0.7f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    color = Color.White,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Add image button (only if less than 5 images)
                            if (mosqueImages.size < 5) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Memilih gambar untuk diupload...")
                                        }
                                        checkAndRequestPermissions()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = emeraldGreen
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Tambah Gambar Masjid")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tambah Gambar (${mosqueImages.size}/5)")
                                }
                            } else {
                                Text(
                                    text = "Jumlah maksimum gambar tercapai (5/5)",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                )

                // Marquee Text Section with character limit
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
                            supportingText = {
                                Text(
                                    text = "$marqueeTextCharCount/100",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End,
                                    color = if (marqueeTextCharCount >= 100) Color.Red else Color.Gray
                                )
                            },
                            isError = marqueeTextCharCount >= 100
                        )
                    }
                )

                // Save Button - only show for master devices
                Button(
                    onClick = {
                        viewModel.updateQuoteText(quoteText)
                        viewModel.updateMosqueName(mosqueName)
                        viewModel.updateMosqueLocation(mosqueLocation)
                        viewModel.updateMarqueeText(marqueeText)
                        viewModel.updatePrayerAddress(prayerAddress)
                        viewModel.updatePrayerTimezone(prayerTimezone)

                        // Update device preferences
                        devicePreferences.deviceName = deviceName
                        devicePreferences.isMasterDevice = isMasterDevice
                        devicePreferences.syncEnabled = syncEnabled

                        viewModel.saveAllSettings() // This will trigger push to PostgreSQL
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = emeraldGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Simpan Perubahan")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan Perubahan")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Manual Sync Button
                Button(
                    onClick = {
                        isSyncing = true
                        scope.launch {
                            try {
                                snackbarHostState.showSnackbar("Memulai sinkronisasi manual...")
                                syncService.forceSyncNow()
                                snackbarHostState.showSnackbar("✅ Sinkronisasi berhasil! Periksa logcat untuk detail.")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("❌ Sinkronisasi gagal: ${e.message}")
                            } finally {
                                isSyncing = false
                            }
                        }
                    },
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSyncing) Color.Gray else Color(0xFF2196F3)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Paksa Sinkronisasi")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSyncing) "Sinkronisasi..." else "Paksa Sinkronisasi Sekarang")
                }
            } else {
                // Show message for non-master devices
                AdminSection(
                    title = "Pengaturan Perangkat",
                    content = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Perangkat ini dikonfigurasi sebagai perangkat non-utama. Perangkat ini akan menerima pembaruan dari perangkat utama tetapi tidak dapat membuat perubahan pada pengaturan masjid.",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )

                            Text(
                                text = "Untuk membuat perubahan, pilih salah satu:",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )

                            Text(
                                text = "• Aktifkan 'Perangkat Utama' di atas untuk memungkinkan perangkat ini mengontrol pengaturan",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )

                            Text(
                                text = "• Gunakan perangkat utama yang ditunjuk untuk membuat perubahan",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                )

                // Save Button for sync settings only
                Button(
                    onClick = {
                        // Update device preferences
                        devicePreferences.deviceName = deviceName
                        devicePreferences.isMasterDevice = isMasterDevice
                        devicePreferences.syncEnabled = syncEnabled
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = emeraldGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Simpan Pengaturan Sinkronisasi")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan Pengaturan Sinkronisasi")
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
            color = emeraldGreen
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
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
