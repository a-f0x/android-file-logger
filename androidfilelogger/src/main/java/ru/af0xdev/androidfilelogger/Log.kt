package ru.af0xdev.androidfilelogger


import android.os.Build
import java.io.*
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.GZIPOutputStream


object Log {
    private const val LOG_FORMAT_D = "%s D/%s: %s"
    private const val LOG_FORMAT_I = "%s I/%s: %s"
    private const val LOG_FORMAT_E2 = "%s E/%s: %s \n %s"
    private const val LOG_FORMAT_W = "%s W/%s: %s"
    private const val TIME_FORMAT = "%02d.%02d.%04d %02d:%02d:%02d"
    private const val DEFAULT_BUFFER_LENGTH = 5000
    private val TAG = Log::class.java.simpleName
    private val lock = ReentrantLock()
    private var buffer = arrayOfNulls<String>(DEFAULT_BUFFER_LENGTH)
    private var indexer = LongArray(DEFAULT_BUFFER_LENGTH)
    private var index = 0
    private var count = DEFAULT_BUFFER_LENGTH
    private var orderIndex: Long = 0
    private lateinit var logFile: File
    private var isDebugMode = true


    fun init(bufferSize: Int = DEFAULT_BUFFER_LENGTH,
             initMessage: String? = null,
             logFile: File, isDebugMode: Boolean) {
        Log.logFile = logFile
        buffer = arrayOfNulls(bufferSize)
        indexer = LongArray(bufferSize)
        count = bufferSize
        Log.isDebugMode = isDebugMode
        initMessage?.let {
            android.util.Log.i("Init LOG ", "Start init log. Start getMessage: $it\n")
        }
        android.util.Log.i("Init LOG ", "Log dir: ${logFile.absolutePath}\n")
        android.util.Log.i("System info ", "-----------------------------------------------------")
        android.util.Log.i("Manufacturer ", Build.MANUFACTURER)
        android.util.Log.i("Model  ", Build.MODEL)
        android.util.Log.i("Version SDK ", "" + Build.VERSION.SDK_INT)
        android.util.Log.i("System info ", "-----------------------------------------------------\n")
    }

    fun d(tag: String, value: String) {
        lock.lock()
        try {
            val line = String.format(LOG_FORMAT_D, getFormattedTime(), tag, value)
            buffer[index] = line
            indexer[index] = orderIndex++
            incIndex()
            if (isDebugMode)
                android.util.Log.d(tag, value)
        } finally {
            lock.unlock()
        }
    }

    fun i(tag: String, value: String) {
        lock.lock()
        try {
            val line = String.format(LOG_FORMAT_I, getFormattedTime(), tag, value)
            buffer[index] = line
            indexer[index] = orderIndex++
            incIndex()
            if (isDebugMode)
                android.util.Log.i(tag, value)
        } finally {
            lock.unlock()
        }
    }

    fun e(tag: String, value: String, e: Throwable? = null) {
        lock.lock()
        try {
            val line = String.format(LOG_FORMAT_E2, getFormattedTime(), tag, value, getStackTraceString(e))
            buffer[index] = line
            indexer[index] = orderIndex++
            incIndex()
            if (isDebugMode)
                android.util.Log.e(tag, value, e)
        } finally {
            lock.unlock()
        }
    }

    fun w(tag: String, value: String) {
        lock.lock()
        try {
            val line = String.format(LOG_FORMAT_W, getFormattedTime(), tag, value) ?: return
            buffer[index] = line
            indexer[index] = orderIndex++
            incIndex()
            if (isDebugMode)
                android.util.Log.w(tag, value)
        } finally {
            lock.unlock()
        }
    }


    private fun writeLog(): File? {
        val fileName = "log.gz"

        val logFile = File(logFile, fileName)
        var fos: FileOutputStream? = null
        try {
            android.util.Log.i("WRITE_LOG", "show")
            if (!logFile.exists()) {
                if (logFile.createNewFile())
                    android.util.Log.i("WRITE_LOG", "create log file " + logFile.absolutePath)
                else
                    android.util.Log.i("WRITE_LOG", "error create log file")
            } else {
                if (logFile.length() > 1024 * 1024 * 3) {
                    android.util.Log.i("WRITE_LOG", "recreate file. old file size > 3mb.")
                    logFile.delete()
                    logFile.createNewFile()
                }
            }
            val array = getCompressedLog()
            if (array.isEmpty()) {
                android.util.Log.i("WRITE_LOG", "buffer 0")
                return null
            }
            fos = FileOutputStream(logFile, true)
            fos.write(array)
            fos.flush()
            fos.close()
            android.util.Log.i("WRITE_LOG", "write successful!")

        } catch (e: IOException) {
            android.util.Log.i("WRITE_LOG", "write ERROR!!!")
            e.printStackTrace()
        } finally {
            if (fos != null) {
                try {
                    fos.flush()
                    fos.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        init(buffer.size, "Log file write successful. " + logFile.absolutePath, this.logFile, isDebugMode)
        return logFile
    }

    private fun getCompressedLog(): ByteArray {
        var os: ByteArrayOutputStream? = null
        var gos: GZIPOutputStream? = null
        try {
            val log = getLog()
            os = ByteArrayOutputStream(log.length)
            gos = GZIPOutputStream(os)
            gos.write(log.toByteArray())
            gos.flush()
            gos.close()
            val compressed = os.toByteArray()
            os.close()
            return compressed
        } catch (e: IOException) {
            android.util.Log.e(TAG, "error", e)
        } finally {
            if (gos != null) {
                try {
                    gos.close()
                } catch (e: IOException) {
                    Log.e(TAG, "error close GZIP output stream", e)
                }
            }
            if (os != null) {
                try {
                    os.close()
                } catch (e: IOException) {
                    Log.e(TAG, "error close output stream", e)
                }
            }
        }
        return ByteArray(0)
    }

    private fun getLog(): String {
        val sb = StringBuilder()
        lock.lock()
        try {
            if (indexer[index] > count) {
                // was cycle
                for (i in index + 1 until count) {
                    if (buffer[i] == null)
                        continue
                    sb.append(buffer[i])
                    sb.append("\n")
                }
                for (i in 0..index) {
                    if (buffer[i] == null)
                        continue
                    sb.append(buffer[i])
                    sb.append("\n")
                }
            } else {
                for (i in 0..index) {
                    if (buffer[i] == null)
                        continue
                    sb.append(buffer[i])
                    sb.append("\n")
                }
            }
        } finally {
            lock.unlock()
        }

        return sb.toString()
    }

    private fun incIndex() {
        index++
        if (index == count) {
            index = 0
            writeLog()
        }
    }

    private fun getFormattedTime(): String {
        val cal = Calendar.getInstance()

        return String.format(TIME_FORMAT,
                cal.get(Calendar.DATE), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND)
        )
    }

    private fun getStackTraceString(tr: Throwable?): String {
        if (tr == null) {
            return ""
        }
        var t = tr
        while (t != null) {
            if (t is UnknownHostException) {
                return ""
            }
            t = t.cause
        }
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        tr.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }
}



