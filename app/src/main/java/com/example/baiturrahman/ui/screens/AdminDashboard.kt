package com.example.baiturrahman.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.example.baiturrahman.ui.theme.emeraldGreen
import com.example.baiturrahman.ui.viewmodel.MosqueDashboardViewModel
import kotlinx.coroutines.launch

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

    // Image picker launchers
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
                snackbarHostState.showSnackbar("Storage permissions are required to select images")
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = emeraldGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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
            // Header Section
            AdminSection(
                title = "Header Settings",
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Mosque Name
                        OutlinedTextField(
                            value = mosqueName,
                            onValueChange = { mosqueName = it },
                            label = { Text("Mosque Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Mosque Location
                        OutlinedTextField(
                            value = mosqueLocation,
                            onValueChange = { mosqueLocation = it },
                            label = { Text("Mosque Location") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Logo Image
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Logo Image:",
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
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = emeraldGreen
                                )
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Change Logo")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Change Logo")
                            }
                        }
                    }
                }
            )

            // Prayer API Settings Section
            AdminSection(
                title = "Prayer Times Settings",
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Address Input
                        OutlinedTextField(
                            value = prayerAddress,
                            onValueChange = { prayerAddress = it },
                            label = { Text("Prayer Times Address") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g., Lebak Bulus, Jakarta, ID") }
                        )

                        // Timezone Dropdown
                        Column {
                            Text(
                                text = "Timezone:",
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
                                        contentDescription = "Select Timezone"
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
                title = "Quote Settings",
                content = {
                    OutlinedTextField(
                        value = quoteText,
                        onValueChange = { quoteText = it },
                        label = { Text("Quote Text") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            )

            // Mosque Images Section
            AdminSection(
                title = "Mosque Images (Max 5)",
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
                                        Image(
                                            painter = rememberAsyncImagePainter(
                                                ImageRequest.Builder(context)
                                                    .data(imageUri.toUri())
                                                    .build()
                                            ),
                                            contentDescription = "Mosque Image ${index + 1}",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )

                                        // Delete button
                                        IconButton(
                                            onClick = { viewModel.removeMosqueImage(index) },
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
                                                contentDescription = "Remove Image",
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
                                onClick = { checkAndRequestPermissions() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = emeraldGreen
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Mosque Image")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Image (${mosqueImages.size}/5)")
                            }
                        } else {
                            Text(
                                text = "Maximum number of images reached (5/5)",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            )

            // Marquee Text Section
            AdminSection(
                title = "Marquee Text",
                content = {
                    OutlinedTextField(
                        value = marqueeText,
                        onValueChange = { marqueeText = it },
                        label = { Text("Marquee Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )

            // Save Button
            Button(
                onClick = {
                    viewModel.updateQuoteText(quoteText)
                    viewModel.updateMosqueName(mosqueName)
                    viewModel.updateMosqueLocation(mosqueLocation)
                    viewModel.updateMarqueeText(marqueeText)
                    viewModel.updatePrayerAddress(prayerAddress)
                    viewModel.updatePrayerTimezone(prayerTimezone)
                    onClose()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = emeraldGreen
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save Changes")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Changes")
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

