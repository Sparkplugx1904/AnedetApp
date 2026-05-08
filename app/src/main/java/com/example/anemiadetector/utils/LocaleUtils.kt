package com.example.anemiadetector.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Utility functions for locale/language management
 */
object LocaleUtils {

    enum class AppLanguage(val code: String, val displayName: String) {
        INDONESIAN("in", "Bahasa Indonesia"),
        ENGLISH("en", "English"),
        THAI("th", "ภาษาไทย")
    }

    /**
     * Update app locale at runtime
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        return context.createConfigurationContext(config)
    }

    /**
     * Get current app language
     */
    fun getCurrentLanguage(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0].language
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale.language
        }
    }

    /**
     * Get AppLanguage from language code
     */
    fun getAppLanguage(code: String): AppLanguage {
        return AppLanguage.entries.find { it.code == code } ?: AppLanguage.INDONESIAN
    }

    /**
     * Get all available languages
     */
    fun getAvailableLanguages(): List<AppLanguage> {
        return AppLanguage.entries
    }
}
