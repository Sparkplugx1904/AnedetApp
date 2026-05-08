package com.example.anemiadetector.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.anemiadetector.BuildConfig
import com.example.anemiadetector.R
import com.example.anemiadetector.utils.LocaleUtils

/**
 * Settings screen for language, theme, and app info
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLanguageChanged: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState(initial = "in")
    val currentTheme by viewModel.currentTheme.collectAsState(initial = "system")

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Language setting
            SettingItem(
                icon = Icons.Default.Language,
                title = stringResource(R.string.settings_language),
                subtitle = LocaleUtils.getAppLanguage(currentLanguage).displayName,
                onClick = { showLanguageDialog = true }
            )

            Divider()

            // Theme setting
            SettingItem(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.settings_theme),
                subtitle = when (currentTheme) {
                    "light" -> stringResource(R.string.settings_theme_light)
                    "dark" -> stringResource(R.string.settings_theme_dark)
                    else -> stringResource(R.string.settings_theme_system)
                },
                onClick = { showThemeDialog = true }
            )

            Divider()

            // About section
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_about),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            SettingItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_version),
                subtitle = BuildConfig.VERSION_NAME,
                onClick = {}
            )
        }
    }

    // Language selection dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column {
                    LocaleUtils.getAvailableLanguages().forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(language.code)
                                    onLanguageChanged(language.code)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLanguage == language.code,
                                onClick = {
                                    viewModel.setLanguage(language.code)
                                    onLanguageChanged(language.code)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = language.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        )
    }

    // Theme selection dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme)) },
            text = {
                Column {
                    listOf(
                        "light" to stringResource(R.string.settings_theme_light),
                        "dark" to stringResource(R.string.settings_theme_dark),
                        "system" to stringResource(R.string.settings_theme_system)
                    ).forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setTheme(value)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTheme == value,
                                onClick = {
                                    viewModel.setTheme(value)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        )
    }
}

/**
 * Single setting item
 */
@Composable
private fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
