package ru.af0xdev.androidfilelogger


import java.io.*
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.GZIPOutputStream


internal class FileLogger constructor(bufferSize: Int,
                                      private var logFile: File,
                                      private val isDebugMode: Boolean = true) {
    companion object {
        const val LOG_FORMAT_D = "%s D/%s: %s"
        const val LOG_FORMAT_I = "%s I/%s: %s"
        const val LOG_FORMAT_E2 = "%s E/%s: %s \n %s"
        const val LOG_FORMAT_W = "%s W/%s: %s"
        const val TIME_FORMAT = "%02d.%02d.%04d %02d:%02d:%02d"
    }

    private val lock = ReentrantLock()
    private var buffer = arrayOfNulls<String>(bufferSize)
    private var indexer = LongArray(bufferSize)
    private var index = 0
    private var count = bufferSize
    private var orderIndex: Long = 0
    private val tag = javaClass.canonicalName


    internal fun d(tag: String, value: String, throwable: Throwable? = null) {
        lock.lock()
        try {
            val line = String.format(LOG_FORMAT_D, getFormattedTime(), tag, value)
            buffer[index] = line
            indexer[index] = orderIndex++
            incIndex()
            if (isDebugMode)
                android.util.Log.d(tag, value, throwable)
        } finally {
            lock.unlock()
        }
    }

    internal fun i(tag: String, value: String) {
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

    internal fun e(tag: String, value: String, e: Throwable? = null) {
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

    internal fun w(tag: String, value: String) {
        lock.lock()
        try {
            val line = String.format(LOG_FORMAT_W, getFormattedTime(), tag, value)
            buffer[index] = line
            indexer[index] = orderIndex++
            incIndex()
            if (isDebugMode)
                android.util.Log.w(tag, value)
        } finally {
            lock.unlock()
        }
    }


    internal fun writeLog(): File {
        var fos: FileOutputStream? = null
        try {
            if (!logFile.exists()) {
                if (logFile.createNewFile())
                    android.util.Log.i(javaClass.simpleName, "create log file " + logFile.absolutePath)
                else
                    android.util.Log.i(javaClass.simpleName, "error create log file")
            }
            val array = getCompressedLog()
            if (array.isEmpty()) {
                android.util.Log.i(javaClass.simpleName, "buffer 0")
                return logFile
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
        buffer = arrayOfNulls(buffer.size)
        indexer = LongArray(buffer.size)
        count = buffer.size
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
            android.util.Log.e(tag, "error", e)
        } finally {
            if (gos != null) {
                try {
                    gos.close()
                } catch (e: IOException) {
                    e(tag, "error close GZIP output stream", e)
                }
            }
            if (os != null) {
                try {
                    os.close()
                } catch (e: IOException) {
                    e(tag, "error close output stream", e)
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



