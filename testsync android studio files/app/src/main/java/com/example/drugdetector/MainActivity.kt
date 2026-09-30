package com.example.drugdetector

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.drugdetector.ui.screens.CalibrationResultScreen
import com.example.drugdetector.ui.screens.CaptureScreen
import com.example.drugdetector.ui.screens.DashboardScreen
import com.example.drugdetector.ui.screens.IntegrityVerifierScreen
import com.example.drugdetector.ui.screens.LoginScreen
import com.example.drugdetector.ui.screens.RecordDetailScreen
import com.example.drugdetector.ui.screens.SettingsScreen
import com.example.drugdetector.ui.screens.SplashScreen
import com.example.drugdetector.ui.screens.TestLogScreen
import com.example.drugdetector.ui.theme.DrugDetectorTheme
import com.example.drugdetector.ui.viewmodel.DrugDetectorViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: DrugDetectorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request Location Permissions on App Start
        checkAndRequestLocationPermissions()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            DrugDetectorTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                MainAppStructure(viewModel = viewModel)
            }
        }
    }

    private fun checkAndRequestLocationPermissions() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1001
            )
        } else {
            viewModel.refreshLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            viewModel.refreshLocation()
        }
    }
}

@Composable
fun MainAppStructure(viewModel: DrugDetectorViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in listOf("dashboard", "capture", "log", "verifier", "settings")) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == "dashboard",
                        onClick = {
                            navController.navigate("dashboard") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "capture",
                        onClick = {
                            navController.navigate("capture") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.AddAPhoto, contentDescription = "Capture") },
                        label = { Text("Test") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "log",
                        onClick = {
                            navController.navigate("log") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.History, contentDescription = "Log") },
                        label = { Text("Log") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "verifier",
                        onClick = {
                            navController.navigate("verifier") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Security, contentDescription = "Verifier") },
                        label = { Text("Audit") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "settings",
                        onClick = {
                            navController.navigate("settings") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Profile") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("splash") {
                SplashScreen(
                    onSplashFinished = {
                        val destination = if (viewModel.isLoggedIn.value) "dashboard" else "login"
                        navController.navigate(destination) {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }

            composable("login") {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToCapture = { navController.navigate("capture") },
                    onNavigateToLog = { navController.navigate("log") },
                    onNavigateToVerifier = { navController.navigate("verifier") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToLogin = { navController.navigate("login") },
                    onSelectRecord = { recordId ->
                        viewModel.selectRecordForDetail(recordId)
                        navController.navigate("detail")
                    }
                )
            }

            composable("capture") {
                CaptureScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToResult = { navController.navigate("result") }
                )
            }

            composable("result") {
                CalibrationResultScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSavedRecordDetail = {
                        navController.navigate("detail") {
                            popUpTo("dashboard")
                        }
                    }
                )
            }

            composable("log") {
                TestLogScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSelectRecord = { recordId ->
                        viewModel.selectRecordForDetail(recordId)
                        navController.navigate("detail")
                    }
                )
            }

            composable("detail") {
                RecordDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("verifier") {
                IntegrityVerifierScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate("login") }
                )
            }
        }
    }
}
