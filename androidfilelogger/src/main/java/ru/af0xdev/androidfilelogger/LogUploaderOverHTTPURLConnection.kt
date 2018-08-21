package ru.af0xdev.androidfilelogger

import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean


class LogUploaderOverHTTPURLConnection(private val serverUrl: String) : ILogUploader {
    private var inProgress = AtomicBoolean(false)

    private val thread = Executors.newSingleThreadExecutor()


    override fun uploadLog(logFile: File, callBack: IUploaderCallBack?) {
        if (inProgress.get())
            return
        thread.submit {
            inProgress.set(true)
            callBack?.onUploadStart()
            var connection: HttpURLConnection? = null
            try {
                val boundary = "---------------------------boundary"
                val tail = "\r\n--$boundary--\r\n"
                val url = URL(serverUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connection.doOutput = true

                val metadataPart = "--$boundary\r\nContent-Disposition: form-data; name=\"metadata\"\r\n\r\n\r\n"
                val fileHeader1 = ("--$boundary\r\n"
                        + "Content-Disposition: form-data; name=\"${logFile.name.substringBefore(".")}\"; filename=\""
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
                var bytesRead: Int
                val buf = ByteArray(1024)
                val bufInput = BufferedInputStream(FileInputStream(logFile))
                while (true) {
                    bytesRead = bufInput.read(buf)
                    if (bytesRead == -1)
                        break
                    out.write(buf, 0, bytesRead)
                    out.flush()
                    progress += bytesRead
                }
                out.writeBytes(tail)
                out.flush()
                out.close()
                if (connection.responseCode in 200..299)
                    callBack?.onUploadFinish()
                else {
                    val error = readResponse(connection.errorStream)
                    callBack?.onUploadError(UploadLogException(error))
                }
            } catch (t: Throwable) {
                callBack?.onUploadError(t)
            } finally {
                inProgress.set(false)
                connection?.disconnect()
            }
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