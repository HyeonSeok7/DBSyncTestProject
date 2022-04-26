package com.example.localsyncsampleproject.di

import android.app.Application
import com.example.localsyncsampleproject.data.room.AppDataBase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    @Singleton
    @Provides
    fun provideAppDatabase(application: Application): AppDataBase {
        return AppDataBase.create(application)
    }

}