package ru.af0xdev.androidfilelogger

interface IUploaderCallBack {
    fun onUploadStart()

    fun onUploadFinish()

    fun onUploadError(throwable: Throwable)
}