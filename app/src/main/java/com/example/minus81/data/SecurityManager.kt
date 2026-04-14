package com.example.minus81.data


import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class SecurityManager(private val context: Context) {
    companion object {
        val PASSWORD_KEY = stringPreferencesKey("app_password")
    }

    val passwordFlow: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PASSWORD_KEY] }

    suspend fun savePassword(password: String) {
        context.dataStore.edit { preferences ->
            preferences[PASSWORD_KEY] = password
        }
    }
}