package com.app.pubglibrary.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

enum class AppLanguage(val code: String) {
    EN("EN"),
    MM("MM");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: EN
        }
    }
}

data class AppPreferencesState(
    val language: AppLanguage = AppLanguage.EN,
    val dontShowNoticeAgain: Boolean = false
)

private object LanguageDataStoreRegistry {
    private val stores = ConcurrentHashMap<String, DataStore<Preferences>>()

    fun getOrCreate(context: Context): DataStore<Preferences> {
        val appContext = context.applicationContext ?: context
        val file = appContext.preferencesDataStoreFile("dropzone_language_prefs")
        return stores.computeIfAbsent(file.canonicalPath) {
            PreferenceDataStoreFactory.create(
                produceFile = { file }
            )
        }
    }
}

class LanguagePreferences(context: Context) {

    private val dataStore: DataStore<Preferences> = LanguageDataStoreRegistry.getOrCreate(context)

    val preferencesFlow: Flow<AppPreferencesState> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            AppPreferencesState(
                language = AppLanguage.fromCode(prefs[KEY_APP_LANGUAGE]),
                dontShowNoticeAgain = prefs[KEY_DONT_SHOW_NOTICE_AGAIN] ?: false
            )
        }

    val languageFlow: Flow<AppLanguage> = preferencesFlow.map { it.language }

    val dontShowNoticeAgainFlow: Flow<Boolean> = preferencesFlow.map { it.dontShowNoticeAgain }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { prefs ->
            prefs[KEY_APP_LANGUAGE] = language.code
        }
    }

    suspend fun setDontShowNoticeAgain(dontShow: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DONT_SHOW_NOTICE_AGAIN] = dontShow
        }
    }

    private companion object {
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_DONT_SHOW_NOTICE_AGAIN = booleanPreferencesKey("dont_show_notice_again")
    }
}
