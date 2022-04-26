package com.example.localsyncsampleproject.di

import android.content.Context
import com.example.localsyncsampleproject.data.datastore.DataStorePreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Singleton
    @Provides
    fun provideDataStorePreferenceManager(@ApplicationContext context: Context) : DataStorePreferenceManager {
        return DataStorePreferenceManager(context)
    }

}