package ru.af0xdev.fileloggerexample

import android.app.Application
import ru.af0xdev.androidfilelogger.Log

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.initialize(applicationInfo.dataDir, "log_file")
        Log.i("App", "Test log info")
    }
}