package com.example.baiturrahman.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.baiturrahman.ui.components.SupabaseImage
import com.example.baiturrahman.ui.theme.EmeraldDark
import com.example.baiturrahman.ui.theme.EmeraldGreen
import com.example.baiturrahman.ui.theme.EmeraldLight
import com.example.baiturrahman.ui.theme.GlassBorder
import com.example.baiturrahman.ui.theme.LocalAppColors
import com.example.baiturrahman.ui.theme.mosqueTextFieldColors
import com.example.baiturrahman.ui.viewmodel.AuthViewModel
import com.example.baiturrahman.ui.viewmodel.MosqueDashboardViewModel
import com.example.baiturrahman.utils.AccountPreferences
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    viewModel: MosqueDashboardViewModel,
    authViewModel: AuthViewModel = koinViewModel(),
    onClose: () -> Unit
) {
    val c = LocalAppColors.current
    val accountPreferences: AccountPreferences = koinInject()
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

    val isSaving by viewModel.isSaving.collectAsState()
    val isUploadingImage by viewModel.isUploadingImage.collectAsState()
    val isUploadingLogo by viewModel.isUploadingLogo.collectAsState()
    val isDeletingImage by viewModel.isDeletingImage.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val currentUsername: String? by authViewModel.currentUsername.collectAsState()
    val isDarkTheme by accountPreferences.isDarkThemeFlow.collectAsState()

    // Change password state
    var showChangePassword by remember { mutableStateOf(false) }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmNewPasswordVisible by remember { mutableStateOf(false) }
    var isChangingPassword by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadConnectedDevices()
    }

    val logoImage by viewModel.logoImage.collectAsState()

    val logoImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.updateLogoImage(it.toString()) } }

    val mosqueImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.addMosqueImage(it.toString()) } }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            mosqueImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            scope.launch { snackbarHostState.showSnackbar("Izin penyimpanan diperlukan") }
        }
    }

    fun checkAndRequestPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            mosqueImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            requestPermissionLauncher.launch(arrayOf(permission))
        }
    }

    val textFieldColors = mosqueTextFieldColors()

    Scaffold(
        containerColor = c.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (showChangePassword) {
                            Text("Ubah Kata Sandi")
                        } else {
                            Column {
                                Text("Dashboard Admin")
                                if (currentUsername != null) {
                                    Text(
                                        text = currentUsername!!,
                                        fontSize = 12.sp,
                                        color = c.textSecondary,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (showChangePassword) showChangePassword = false else onClose() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = c.surface,
                        titleContentColor = c.textPrimary,
                        navigationIconContentColor = c.textPrimary
                    )
                )
                // Offline banner
                if (isOffline) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFB71C1C))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada koneksi — perubahan akan disimpan saat online",
                            color = Color.White,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (showChangePassword) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Kata Sandi Saat Ini") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true,
                    visualTransformation = if (oldPasswordVisible) VisualTransformation.None
                                          else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                            Icon(
                                if (oldPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null, tint = c.textSecondary
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Kata Sandi Baru (min. 6 karakter)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true,
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None
                                          else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null, tint = c.textSecondary
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = { confirmNewPassword = it },
                    label = { Text("Konfirmasi Kata Sandi Baru") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true,
                    visualTransformation = if (confirmNewPasswordVisible) VisualTransformation.None
                                          else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { confirmNewPasswordVisible = !confirmNewPasswordVisible }) {
                            Icon(
                                if (confirmNewPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null, tint = c.textSecondary
                            )
                        }
                    },
                    isError = confirmNewPassword.isNotBlank() && newPassword != confirmNewPassword,
                    supportingText = {
                        if (confirmNewPassword.isNotBlank() && newPassword != confirmNewPassword) {
                            Text("Kata sandi tidak cocok", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                )

                Button(
                    onClick = {
                        scope.launch {
                            isChangingPassword = true
                            try {
                                val error = viewModel.changePassword(oldPassword, newPassword)
                                if (error == null) {
                                    snackbarHostState.showSnackbar("Kata sandi berhasil diubah")
                                    oldPassword = ""
                                    newPassword = ""
                                    confirmNewPassword = ""
                                    showChangePassword = false
                                } else {
                                    snackbarHostState.showSnackbar(error)
                                }
                            } finally {
                                isChangingPassword = false
                            }
                        }
                    },
                    enabled = !isChangingPassword && oldPassword.isNotBlank() &&
                        newPassword.length >= 6 && newPassword == confirmNewPassword,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isChangingPassword) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Menyimpan...")
                    } else {
                        Text("Simpan Kata Sandi Baru")
                    }
                }
            }
        } else Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Section
            AdminSection(title = "Pengaturan Header") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = mosqueName,
                        onValueChange = { if (it.length <= 35) mosqueName = it },
                        label = { Text("Nama Masjid") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors,
                        supportingText = {
                            Text(
                                "${mosqueName.length}/35",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End,
                                color = if (mosqueName.length >= 35) Color.Red else c.textSecondary
                            )
                        },
                        isError = mosqueName.length >= 35
                    )

                    OutlinedTextField(
                        value = mosqueLocation,
                        onValueChange = { if (it.length <= 25) mosqueLocation = it },
                        label = { Text("Lokasi Masjid") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors,
                        supportingText = {
                            Text(
                                "${mosqueLocation.length}/25",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End,
                                color = if (mosqueLocation.length >= 25) Color.Red else c.textSecondary
                            )
                        },
                        isError = mosqueLocation.length >= 25
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Logo Masjid:", fontWeight = FontWeight.Medium, color = c.textPrimary)
                        Text(
                            "Logo yang diupload otomatis tersimpan ke semua perangkat.",
                            color = c.textSecondary, fontSize = 13.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (logoImage != null) {
                                SupabaseImage(
                                    imageUrl = logoImage!!,
                                    contentDescription = "Logo Masjid",
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, EmeraldGreen, RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit,
                                    fallbackResourceId = null
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                        .background(c.background),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Belum ada", color = c.textSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
                                }
                            }
                            Button(
                                onClick = {
                                    logoImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                enabled = !isUploadingLogo,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isUploadingLogo) {
                                    CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Mengupload...")
                                } else {
                                    Icon(Icons.Default.Edit, "Ubah Logo")
                                    Spacer(Modifier.width(8.dp))
                                    Text("Ubah Logo")
                                }
                            }
                        }
                    }
                }
            }

            // Prayer Time Settings
            AdminSection(title = "Pengaturan Waktu Sholat") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = prayerAddress,
                        onValueChange = { prayerAddress = it },
                        label = { Text("Alamat Waktu Sholat") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors,
                        placeholder = { Text("contoh: Lebak Bulus, Jakarta, ID", color = c.textSecondary) }
                    )

                    Column {
                        Text("Zona Waktu:", fontWeight = FontWeight.Medium, color = c.textPrimary, modifier = Modifier.padding(bottom = 8.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GlassBorder, RoundedCornerShape(4.dp))
                                    .clickable { timezoneMenuExpanded = true }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(prayerTimezone, color = c.textPrimary)
                                Icon(Icons.Default.ArrowDropDown, "Pilih Zona Waktu", tint = c.textSecondary)
                            }
                            DropdownMenu(
                                expanded = timezoneMenuExpanded,
                                onDismissRequest = { timezoneMenuExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                viewModel.availableTimezones.forEach { timezone ->
                                    DropdownMenuItem(
                                        text = { Text(timezone) },
                                        onClick = { prayerTimezone = timezone; timezoneMenuExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quote Section
            AdminSection(title = "Pengaturan Kutipan") {
                OutlinedTextField(
                    value = quoteText,
                    onValueChange = { if (it.length <= 150) quoteText = it },
                    label = { Text("Teks Kutipan") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    minLines = 3,
                    supportingText = {
                        Text(
                            "${quoteText.length}/100",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            color = if (quoteText.length >= 100) Color.Red else c.textSecondary
                        )
                    },
                    isError = quoteText.length >= 100
                )
            }

            // Mosque Images Section
            AdminSection(title = "Slide Gambar (640 x 410) (Maks 5)") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Gambar yang diupload otomatis tersinkronisasi ke semua perangkat.",
                        color = c.textSecondary, fontSize = 13.sp
                    )

                    if (mosqueImages.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().height(120.dp)
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
                                        contentDescription = "Gambar ${index + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        fallbackResourceId = null
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeMosqueImage(index) },
                                        enabled = !isDeletingImage && !isUploadingImage,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(32.dp)
                                            .background(c.background.copy(alpha = 0.7f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, "Hapus", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(4.dp)
                                            .background(c.background.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("${index + 1}", color = c.textPrimary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (mosqueImages.size < 5) {
                        Button(
                            onClick = { checkAndRequestPermissions() },
                            enabled = !isUploadingImage,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isUploadingImage) {
                                CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                                Spacer(Modifier.width(8.dp))
                                Text("Mengupload...")
                            } else {
                                Icon(Icons.Default.Add, "Tambah")
                                Spacer(Modifier.width(8.dp))
                                Text("Tambah Gambar (${mosqueImages.size}/5)")
                            }
                        }
                    } else {
                        Text("Jumlah maksimum gambar tercapai (5/5)", color = c.textSecondary, fontSize = 14.sp)
                    }
                }
            }

            // Marquee Text Section
            AdminSection(title = "Teks Berjalan") {
                OutlinedTextField(
                    value = marqueeText,
                    onValueChange = { if (it.length <= 200) marqueeText = it },
                    label = { Text("Teks Berjalan") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    supportingText = {
                        Text(
                            "${marqueeText.length}/200",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            color = if (marqueeText.length >= 200) Color.Red else c.textSecondary
                        )
                    },
                    isError = marqueeText.length >= 200
                )
            }

            // Theme Toggle Section
            AdminSection(title = "Tema Tampilan") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mode Gelap", color = c.textPrimary)
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { accountPreferences.isDarkTheme = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldGreen,
                            checkedTrackColor = EmeraldDark
                        )
                    )
                }
            }

            // Save settings button
            Button(
                onClick = {
                    scope.launch {
                        viewModel.updateQuoteText(quoteText)
                        viewModel.updateMosqueName(mosqueName)
                        viewModel.updateMosqueLocation(mosqueLocation)
                        viewModel.updateMarqueeText(marqueeText)
                        viewModel.updatePrayerAddress(prayerAddress)
                        viewModel.updatePrayerTimezone(prayerTimezone)
                        viewModel.saveAllSettings()
                        viewModel.fetchPrayerTimes()
                        snackbarHostState.showSnackbar("Pengaturan disimpan")
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), Color.White, 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Menyimpan...")
                } else {
                    Icon(Icons.Default.Check, "Simpan")
                    Spacer(Modifier.width(8.dp))
                    Text("Simpan Perubahan")
                }
            }

            // Connected Devices Section
            AdminSection(title = "Perangkat Terhubung") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (connectedDevices.isEmpty()) {
                        Text("Memuat daftar perangkat...", color = c.textSecondary, fontSize = 14.sp)
                    } else {
                        connectedDevices.forEachIndexed { index, session ->
                            if (index > 0) HorizontalDivider(color = GlassBorder)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            session.deviceLabel,
                                            color = c.textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (session.isCurrent) {
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "(Perangkat ini)",
                                                color = EmeraldGreen,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Text(
                                        "Terakhir aktif: ${formatLastSeen(session.lastSeenAt)}",
                                        color = c.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                if (!session.isCurrent) {
                                    TextButton(
                                        onClick = { viewModel.forceLogoutDevice(session.id) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                    ) {
                                        Text("Keluarkan", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { viewModel.loadConnectedDevices() },
                        colors = ButtonDefaults.textButtonColors(contentColor = EmeraldGreen)
                    ) {
                        Text("Perbarui Daftar", fontSize = 13.sp)
                    }
                }
            }

            // Change Password Button
            Button(
                onClick = { showChangePassword = true },
                colors = ButtonDefaults.buttonColors(containerColor = c.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = EmeraldGreen)
                Spacer(Modifier.width(8.dp))
                Text("Ubah Kata Sandi", color = c.textPrimary)
            }

            // Logout Button
            Button(
                onClick = { authViewModel.logout() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Keluar", color = Color.White, fontSize = 16.sp)
            }

            Spacer(Modifier.height(16.dp))
        } // end else Column
    }
}

private fun formatLastSeen(isoTimestamp: String): String {
    return try {
        // Simple formatting: show the date/time part up to seconds
        if (isoTimestamp.length >= 16) {
            isoTimestamp.substring(0, 16).replace("T", " ")
        } else {
            isoTimestamp
        }
    } catch (_: Exception) {
        isoTimestamp
    }
}

@Composable
fun AdminSection(
    title: String,
    content: @Composable () -> Unit
) {
    val c = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            colors = CardDefaults.cardColors(containerColor = c.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                content()
            }
        }
    }
}
