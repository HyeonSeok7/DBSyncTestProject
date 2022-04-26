package com.example.localsyncsampleproject.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStorePreferenceManager @Inject constructor(
    @ApplicationContext appContext: Context
) {

    private val Context.dataStore by preferencesDataStore("mediaStore")

    private val mediaStore = appContext.dataStore

    private val firstSyncTime = stringPreferencesKey("first_sync_time")
    val firstSyncTimeFlow: Flow<String?> = mediaStore.data.map { pref ->
        pref[firstSyncTime]
    }

    suspend fun setFirstSyncTime(firstTime: String) {
        mediaStore.edit { pref ->
            pref[firstSyncTime] = firstTime
        }
    }

    private val lastSyncTime = stringPreferencesKey("last_sync_time")
    val lastSyncTimeFlow: Flow<String?> = mediaStore.data.map { pref ->
        pref[lastSyncTime]
    }

    suspend fun setLastSyncTime(lastTime: String) {
        mediaStore.edit { pref ->
            pref[lastSyncTime] = lastTime
        }
    }
}