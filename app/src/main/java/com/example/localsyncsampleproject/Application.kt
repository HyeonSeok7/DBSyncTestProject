package com.example.localsyncsampleproject

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class Application: Application(), androidx.work.Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

    }

    override fun getWorkManagerConfiguration(): Configuration {
        return androidx.work.Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}