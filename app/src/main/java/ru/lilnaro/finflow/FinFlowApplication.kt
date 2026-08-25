package ru.lilnaro.finflow

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.lilnaro.finflow.data.local.initializer.DefaultTransactionCategoriesInitializer
import ru.lilnaro.finflow.di.appModule

class FinFlowApplication : Application() {

    private val applicationScope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO,
        )

    override fun onCreate() {
        super.onCreate()

        val koin =
            startKoin {
                androidContext(
                    this@FinFlowApplication,
                )

                modules(
                    appModule,
                )
            }.koin

        val categoriesInitializer =
            koin.get<DefaultTransactionCategoriesInitializer>()

        initializeDefaultCategories(
            categoriesInitializer =
                categoriesInitializer,
        )
    }

    private fun initializeDefaultCategories(
        categoriesInitializer:
        DefaultTransactionCategoriesInitializer,
    ) {
        applicationScope.launch {
            try {
                categoriesInitializer.initialize()
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Не удалось инициализировать встроенные категории",
                    exception,
                )
            }
        }
    }

    private companion object {

        const val TAG =
            "FinFlowApplication"
    }
}