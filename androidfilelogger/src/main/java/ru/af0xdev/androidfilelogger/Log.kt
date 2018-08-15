package ru.af0xdev.androidfilelogger

import android.os.Build
import java.io.File

object Log {
    private var isInitialized = false
    private lateinit var logger: FileLogger
    private var uploader: ILogUploader? = null
    private const val DEFAULT_BUFFER_LENGTH = 5000


    /**
     *
     * @param directoryPath  log file location
     * @param fileName name of file without extension
     * @param enableAndroidUtilLogger - enable duplication log messages to [android.util.Log]
     * @param bufferSize size of in memory storage, default value is 5000
     * @param logUploader implementation of [ILogUploader] may be null
     *
     * */

    fun initialize(directoryPath: String, fileName: String,
                   enableAndroidUtilLogger: Boolean = true,
                   bufferSize: Int = DEFAULT_BUFFER_LENGTH,
                   logUploader: ILogUploader? = null) {

        val fName = "${fileName.substringBefore(".")}.gz"
        val logFile = File(directoryPath, fName)

        logger = FileLogger(bufferSize, logFile, enableAndroidUtilLogger)
        logger.i("Init LOG ", "FileLogger dir: ${logFile.absolutePath}\n")
        logger.i("System info ", "-----------------------------------------------------")
        logger.i("Manufacturer ", Build.MANUFACTURER ?: "Unknown")
        logger.i("Model  ", Build.MODEL ?: "Unknown")
        logger.i("SDK version ", "" + Build.VERSION.SDK_INT)
        logger.i("System info ", "-----------------------------------------------------\n")
        uploader = logUploader
        isInitialized = true
    }

    /**
     * Send a {@link #DEBUG} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param throwable An exception to log
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        check(isInitialized, { "Log is not initialized! Call Log.fun initialize(...)" })
        logger.d(tag, message, throwable)
    }

    /**
     * Log a info message
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * */
    fun i(tag: String, message: String) {
        check(isInitialized, { "Log is not initialized! Call Log.fun initialize(...)" })
        logger.i(tag, message)
    }

    /**
     * Log a error message
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param throwable An exception to log
     * */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        check(isInitialized, { "Log is not initialized! Call Log.fun initialize(...)" })
        logger.e(tag, message, throwable)
    }

    /**
     * Log a warning message
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * */
    fun w(tag: String, message: String) {
        check(isInitialized, { "Log is not initialized! Call Log.fun initialize(...)" })
        logger.w(tag, message)
    }

    /**
     * Flush all logged info into file, and return file compressed in gzip
     * @return [File]
     *
     * */
    fun flushLog(): File = logger.writeLog()

    /**
     * Upload log in another thread
     * @param
     * */
    fun uploadLog(callBack: IUploaderCallBack?) {
        uploader?.uploadLog(logger.writeLog(), callBack)
    }
}