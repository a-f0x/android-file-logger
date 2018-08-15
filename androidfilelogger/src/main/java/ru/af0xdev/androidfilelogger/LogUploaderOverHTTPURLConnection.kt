package ru.af0xdev.androidfilelogger

import java.io.*
import java.net.HttpURLConnection
import java.net.URL


class LogUploaderOverHTTPURLConnection(private val serverUrl: String) : ILogUploader {

    override fun uploadLog(logFile: File, callBack: IUploaderCallBack?) {
        callBack?.onUploadStart()
        var connection: HttpURLConnection? = null
        try {
            val boundary = "---------------------------boundary"
            val tail = "\r\n--$boundary--\r\n"
            val url = URL(serverUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.doOutput = true
            val metadataPart = ("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"metadata\"\r\n\r\n"
                    + "" + "\r\n")
            val fileHeader1 = ("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"uploadfile\"; filename=\""
                    + logFile.name + "\"\r\n"
                    + "Content-Type: application/octet-stream\r\n"
                    + "Content-Transfer-Encoding: binary\r\n")
            val fileLength = logFile.length() + tail.length
            val fileHeader2 = "Content-length: $fileLength\r\n"
            val fileHeader = fileHeader1 + fileHeader2 + "\r\n"
            val stringData = metadataPart + fileHeader
            val requestLength = stringData.length + fileLength
            connection.setRequestProperty("Content-length", "" + requestLength)
            connection.connect()
            val out = DataOutputStream(connection.outputStream)
            out.writeBytes(stringData)
            out.flush()
            var progress = 0
            var bytesRead = 0
            val buf = ByteArray(1024)
            val bufInput = BufferedInputStream(FileInputStream(logFile))
            var lastPercent = 0
            while (bytesRead != -1) {
                bytesRead = bufInput.read(buf)
                out.write(buf, 0, bytesRead)
                out.flush()
                progress += bytesRead
                val percent = ((fileLength / progress) * 100).toInt()
                if (lastPercent != percent)
                    callBack?.onUploadProgress(percent)
                lastPercent = percent
            }
            out.writeBytes(tail)
            out.flush()
            out.close()
            if (connection.responseCode in 200..299)
                callBack?.onUploadFinish()
            else {
                val error = readResponse(connection.inputStream)
                callBack?.onUploadError(UploadLogException(error))
            }
        } catch (t: Throwable) {
            callBack?.onUploadError(t)
        } finally {
            connection?.disconnect()
        }
    }

    private fun readResponse(stream: InputStream): String {
        BufferedReader(InputStreamReader(stream)).use { reader ->
            var line: String? = ""
            return buildString {
                while (line != null) {
                    line = reader.readLine()
                    append(line)
                }
            }
        }
    }
}