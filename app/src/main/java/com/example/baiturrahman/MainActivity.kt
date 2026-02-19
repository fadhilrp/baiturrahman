package com.example.baiturrahman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.baiturrahman.data.remote.SupabaseSyncService
import com.example.baiturrahman.ui.screens.MosqueDashboard
import com.example.baiturrahman.ui.theme.BaiturrahmanTheme
import com.example.baiturrahman.ui.viewmodel.AuthViewModel
import com.example.baiturrahman.ui.viewmodel.MosqueDashboardViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    private val syncService: SupabaseSyncService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaiturrahmanTheme {
                val authViewModel: AuthViewModel = koinViewModel()
                val dashboardViewModel: MosqueDashboardViewModel = koinViewModel()

                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

                // Start/stop sync based on login state
                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn) syncService.startSync() else syncService.stopSync()
                }

                // Always show the dashboard; login is accessed via the settings button
                MosqueDashboard(viewModel = dashboardViewModel, authViewModel = authViewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        syncService.stopSync()
    }
}
