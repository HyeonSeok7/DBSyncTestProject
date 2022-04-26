package com.example.localsyncsampleproject.domain.repository

import kotlinx.coroutines.flow.Flow

interface SyncTimeRepository {

    suspend fun setFirstSyncTime(firstTime: String)

    fun observeFirstTime(): Flow<String?>

    suspend fun setLastSyncTime(lastTime: String)

    fun observeLastTime(): Flow<String?>

}