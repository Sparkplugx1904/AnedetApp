package com.example.anemiadetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.anemiadetector.ui.camera.CameraScreen
import com.example.anemiadetector.ui.history.HistoryScreen
import com.example.anemiadetector.ui.onboarding.OnboardingScreen
import com.example.anemiadetector.ui.settings.SettingsScreen
import com.example.anemiadetector.ui.settings.SettingsViewModel
import com.example.anemiadetector.ui.theme.AnemiaDetectorTheme
import com.example.anemiadetector.utils.LocaleUtils
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val currentLanguage by settingsViewModel.currentLanguage.collectAsState(initial = "in")
            val currentTheme by settingsViewModel.currentTheme.collectAsState(initial = "system")
            val onboardingCompleted by settingsViewModel.onboardingCompleted.collectAsState(initial = false)

            // Apply locale
            LaunchedEffect(currentLanguage) {
                LocaleUtils.setLocale(this@MainActivity, currentLanguage)
            }

            // Determine dark theme
            val darkTheme = when (currentTheme) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            AnemiaDetectorTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        startDestination = if (onboardingCompleted) "camera" else "onboarding",
                        onOnboardingComplete = {
                            settingsViewModel.completeOnboarding()
                        },
                        onLanguageChanged = { languageCode ->
                            LocaleUtils.setLocale(this@MainActivity, languageCode)
                            recreate() // Recreate activity to apply new locale
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    startDestination: String,
    onOnboardingComplete: () -> Unit,
    onLanguageChanged: (String) -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Onboarding
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    try {
                        onOnboardingComplete()
                        navController.navigate("camera") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            )
        }

        // Camera (Main screen)
        composable("camera") {
            CameraScreen(
                onNavigateToHistory = {
                    navController.navigate("history")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        // History
        composable("history") {
            HistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Settings
        composable("settings") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLanguageChanged = onLanguageChanged
            )
        }
    }
}
