package ru.af0xdev.fileloggerexample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import ru.af0xdev.androidfilelogger.IUploaderCallBack
import ru.af0xdev.androidfilelogger.Log

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var tvProgress: TextView
    private lateinit var progress: ProgressBar
    private lateinit var etAddToLog: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnSend).setOnClickListener(this)
        findViewById<Button>(R.id.btnI).setOnClickListener(this)
        findViewById<Button>(R.id.btnD).setOnClickListener(this)
        findViewById<Button>(R.id.btnW).setOnClickListener(this)
        findViewById<Button>(R.id.btnE).setOnClickListener(this)
        progress = findViewById(R.id.progress)
        tvProgress = findViewById(R.id.tvProgress)
        etAddToLog = findViewById(R.id.etAddToLogField)
    }


    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnSend -> upload()
            R.id.btnI -> addToLogI()
            R.id.btnD -> addToLogD()
            R.id.btnE -> addToLogE()
            R.id.btnW -> addToLogW()
        }
    }

    private fun addToLogW() {
        if (etAddToLog.text.isNullOrEmpty())
            return
        Log.w(javaClass.simpleName, etAddToLog.text.toString())
        etAddToLog.text = null

    }

    private fun addToLogE() {
        if (etAddToLog.text.isNullOrEmpty())
            return
        Log.e(javaClass.simpleName, etAddToLog.text.toString())
        etAddToLog.text = null

    }

    private fun addToLogD() {
        if (etAddToLog.text.isNullOrEmpty())
            return
        Log.d(javaClass.simpleName, etAddToLog.text.toString())
        etAddToLog.text = null

    }

    private fun addToLogI() {
        if (etAddToLog.text.isNullOrEmpty())
            return
        Log.i(javaClass.simpleName, etAddToLog.text.toString())
        etAddToLog.text = null
    }


    private fun upload() {
        Log.uploadLog(object : IUploaderCallBack {
            override fun onUploadStart() {
                runOnUiThread {
                    progress.visibility = VISIBLE
                    tvProgress.text = "upload started!"
                }

            }

            override fun onUploadFinish() {
                Log.flushLog().delete()
                runOnUiThread {
                    progress.visibility = GONE
                    tvProgress.text = "upload finished!"
                }
            }

            override fun onUploadError(throwable: Throwable) {
                runOnUiThread {
                    throwable.printStackTrace()
                    progress.visibility = GONE
                    tvProgress.text = "upload error: ${throwable.message}!"
                }
            }


        })
    }
}
