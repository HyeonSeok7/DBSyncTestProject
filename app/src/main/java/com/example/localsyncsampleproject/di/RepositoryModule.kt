package com.example.localsyncsampleproject.di

import android.content.Context
import com.example.localsyncsampleproject.data.datastore.DataStorePreferenceManager
import com.example.localsyncsampleproject.data.repository.MediaRepositoryImpl
import com.example.localsyncsampleproject.data.repository.SyncTimeRepositoryImpl
import com.example.localsyncsampleproject.data.room.AppDataBase
import com.example.localsyncsampleproject.domain.repository.MediaRepository
import com.example.localsyncsampleproject.domain.repository.SyncTimeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideMediaRepository(
        @ApplicationContext appContext: Context,
        database: AppDataBase,
        dataStorePreferenceManager: DataStorePreferenceManager,
        syncTimeRepository: SyncTimeRepository
    ) : MediaRepository {
        return MediaRepositoryImpl(appContext, database, dataStorePreferenceManager, syncTimeRepository)
    }

    @Singleton
    @Provides
    fun provideSyncTimeRepository(
        @ApplicationContext appContext: Context,
        dataStorePreferenceManager: DataStorePreferenceManager
    ) : SyncTimeRepository {
        return SyncTimeRepositoryImpl(appContext, dataStorePreferenceManager)
    }

}