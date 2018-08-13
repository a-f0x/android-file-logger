package ru.af0xdev.androidfilelogger

import java.io.File

internal interface ILogUploader {
    fun uploadLog(logFile: File, callBack: IUploaderCallBack? = null)

}

