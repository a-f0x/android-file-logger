package ru.af0xdev.fileloggerexample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import ru.af0xdev.androidfilelogger.Log

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.init().

    }
}
