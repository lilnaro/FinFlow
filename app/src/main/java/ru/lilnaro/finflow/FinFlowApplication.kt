package ru.lilnaro.finflow

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.lilnaro.finflow.di.appModule

class FinFlowApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@FinFlowApplication)
            modules(appModule)
        }
    }
}