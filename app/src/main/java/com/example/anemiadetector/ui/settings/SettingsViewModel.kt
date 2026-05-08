package com.example.anemiadetector.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * ViewModel for Settings Screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val THEME_KEY = stringPreferencesKey("theme")
        private val ONBOARDING_COMPLETED_KEY = stringPreferencesKey("onboarding_completed")
    }

    /**
     * Get current language
     */
    val currentLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: "in"  // Default: Indonesian
    }

    /**
     * Get current theme
     */
    val currentTheme: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "system"  // Default: Follow system
    }

    /**
     * Check if onboarding is completed
     */
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED_KEY] == "true"
    }

    /**
     * Set language
     */
    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[LANGUAGE_KEY] = languageCode
            }
        }
    }

    /**
     * Set theme
     */
    fun setTheme(theme: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[THEME_KEY] = theme
            }
        }
    }

    /**
     * Mark onboarding as completed
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[ONBOARDING_COMPLETED_KEY] = "true"
            }
        }
    }
}
