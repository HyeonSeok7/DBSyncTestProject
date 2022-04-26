package com.example.localsyncsampleproject.data.repository

import android.content.Context
import com.example.localsyncsampleproject.data.datastore.DataStorePreferenceManager
import com.example.localsyncsampleproject.domain.repository.SyncTimeRepository
import dagger.hilt.android.qualifiers.ActivityContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SyncTimeRepositoryImpl @Inject constructor(
    @ActivityContext private val context: Context,
    private val dataStorePreferenceManager: DataStorePreferenceManager
) : SyncTimeRepository {

    override suspend fun setFirstSyncTime(firstTime: String) {
        dataStorePreferenceManager.setFirstSyncTime(firstTime)
    }

    override fun observeFirstTime(): Flow<String?> {
        return dataStorePreferenceManager.firstSyncTimeFlow
    }

    override suspend fun setLastSyncTime(lastTime: String) {
        dataStorePreferenceManager.setLastSyncTime(lastTime)
    }

    override fun observeLastTime(): Flow<String?> {
        return dataStorePreferenceManager.lastSyncTimeFlow
    }

}