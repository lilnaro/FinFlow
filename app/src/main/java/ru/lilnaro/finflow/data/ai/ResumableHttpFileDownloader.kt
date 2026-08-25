package ru.lilnaro.finflow.data.ai

import java.io.EOFException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.security.cert.CertificateException
import javax.net.ssl.SSLHandshakeException

internal class ResumableHttpFileDownloader(
    private val sourceUrl: String,
    private val expectedBytes: Long,
    private val connectTimeoutMillis: Int = DEFAULT_CONNECT_TIMEOUT_MILLIS,
    private val readTimeoutMillis: Int = DEFAULT_READ_TIMEOUT_MILLIS,
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val retryBaseDelayMillis: Long = DEFAULT_RETRY_BASE_DELAY_MILLIS,
) {

    sealed interface Result {

        data object Success : Result

        data object Cancelled : Result

        data class Failure(
            val message: String,
        ) : Result
    }

    private class RetryableDownloadException(
        message: String,
        cause: Throwable? = null,
    ) : IOException(message, cause)

    private class PermanentDownloadException(
        message: String,
        cause: Throwable? = null,
    ) : IOException(message, cause)

    private val cancelled =
        AtomicBoolean(false)

    @Volatile
    private var activeConnection:
            HttpURLConnection? = null

    fun cancel() {
        cancelled.set(true)
        activeConnection?.disconnect()
    }

    fun download(
        partialFile: File,
        onProgress: (
            downloadedBytes: Long,
            totalBytes: Long,
        ) -> Unit,
    ): Result {
        partialFile.parentFile
            ?.mkdirs()

        if (
            partialFile.exists() &&
            partialFile.length() > expectedBytes
        ) {
            partialFile.delete()
        }

        if (
            partialFile.exists() &&
            partialFile.length() == expectedBytes
        ) {
            onProgress(
                expectedBytes,
                expectedBytes,
            )

            return Result.Success
        }

        var attempt = 0
        var lastFailure: Throwable? = null

        while (
            attempt < maxAttempts &&
            !cancelled.get()
        ) {
            attempt += 1

            try {
                downloadAttempt(
                    partialFile = partialFile,
                    onProgress = onProgress,
                )

                if (cancelled.get()) {
                    return Result.Cancelled
                }

                if (
                    partialFile.length() ==
                    expectedBytes
                ) {
                    return Result.Success
                }

                throw RetryableDownloadException(
                    "Сервер завершил ответ раньше ожидаемого размера файла.",
                )
            } catch (throwable: Throwable) {
                if (cancelled.get()) {
                    return Result.Cancelled
                }

                if (!isRetryable(throwable)) {
                    return Result.Failure(
                        message =
                            throwable.toUserMessage(),
                    )
                }

                lastFailure = throwable

                if (attempt < maxAttempts) {
                    waitBeforeRetry(
                        attempt = attempt,
                    )
                }
            }
        }

        if (cancelled.get()) {
            return Result.Cancelled
        }

        return Result.Failure(
            message =
                lastFailure
                    ?.toUserMessage()
                    ?: "Не удалось скачать локальную AI-модель.",
        )
    }

    private fun downloadAttempt(
        partialFile: File,
        onProgress: (
            downloadedBytes: Long,
            totalBytes: Long,
        ) -> Unit,
    ) {
        if (cancelled.get()) {
            return
        }

        var resumeOffset =
            partialFile
                .takeIf { file ->
                    file.exists()
                }
                ?.length()
                ?.coerceAtLeast(0L)
                ?: 0L

        onProgress(
            resumeOffset,
            expectedBytes,
        )

        var connection =
            openFollowingRedirects(
                resumeOffset = resumeOffset,
            )

        activeConnection = connection

        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    if (resumeOffset > 0L) {
                        if (
                            !partialFile.delete() &&
                            partialFile.exists()
                        ) {
                            throw PermanentDownloadException(
                                "Не удалось очистить частично скачанный файл.",
                            )
                        }

                        resumeOffset = 0L

                        onProgress(
                            0L,
                            expectedBytes,
                        )
                    }
                }

                HttpURLConnection.HTTP_PARTIAL -> {
                    validatePartialResponse(
                        connection = connection,
                        expectedOffset = resumeOffset,
                    )
                }

                HTTP_RANGE_NOT_SATISFIABLE -> {
                    if (
                        partialFile.exists() &&
                        partialFile.length() ==
                        expectedBytes
                    ) {
                        return
                    }

                    if (
                        partialFile.exists() &&
                        !partialFile.delete()
                    ) {
                        throw PermanentDownloadException(
                            "Не удалось сбросить некорректный частичный файл.",
                        )
                    }

                    throw RetryableDownloadException(
                        "Сервер отклонил позицию продолжения загрузки.",
                    )
                }

                HTTP_FORBIDDEN,
                HTTP_REQUEST_TIMEOUT,
                HTTP_TOO_MANY_REQUESTS,
                    -> {
                    throw RetryableDownloadException(
                        "Сервер временно не готов продолжить загрузку (HTTP ${connection.responseCode}).",
                    )
                }

                in HTTP_SERVER_ERROR_MIN..HTTP_SERVER_ERROR_MAX -> {
                    throw RetryableDownloadException(
                        "Сервер временно недоступен (HTTP ${connection.responseCode}).",
                    )
                }

                else -> {
                    throw PermanentDownloadException(
                        "Сервер модели вернул HTTP ${connection.responseCode}.",
                    )
                }
            }

            val append =
                connection.responseCode ==
                        HttpURLConnection.HTTP_PARTIAL &&
                        resumeOffset > 0L

            connection.inputStream
                .buffered(
                    DEFAULT_BUFFER_SIZE_BYTES,
                )
                .use { input ->
                    FileOutputStream(
                        partialFile,
                        append,
                    )
                        .buffered(
                            DEFAULT_BUFFER_SIZE_BYTES,
                        )
                        .use { output ->
                            val buffer =
                                ByteArray(
                                    DEFAULT_BUFFER_SIZE_BYTES,
                                )

                            var downloadedBytes =
                                if (append) {
                                    resumeOffset
                                } else {
                                    0L
                                }

                            while (!cancelled.get()) {
                                val read =
                                    input.read(buffer)

                                if (read < 0) {
                                    break
                                }

                                if (read == 0) {
                                    continue
                                }

                                output.write(
                                    buffer,
                                    0,
                                    read,
                                )

                                downloadedBytes +=
                                    read.toLong()

                                if (
                                    downloadedBytes >
                                    expectedBytes
                                ) {
                                    throw PermanentDownloadException(
                                        "Размер скачиваемого файла больше ожидаемого.",
                                    )
                                }

                                onProgress(
                                    downloadedBytes,
                                    expectedBytes,
                                )
                            }

                            output.flush()
                        }
                }

            if (cancelled.get()) {
                return
            }

            val actualBytes =
                partialFile.length()

            if (actualBytes < expectedBytes) {
                throw EOFException(
                    "Получено $actualBytes из $expectedBytes байт.",
                )
            }

            if (actualBytes > expectedBytes) {
                throw PermanentDownloadException(
                    "Полученный файл имеет неожиданный размер.",
                )
            }
        } catch (throwable: Throwable) {
            if (cancelled.get()) {
                return
            }

            if (throwable is IOException) {
                throw throwable
            }

            throw RetryableDownloadException(
                message =
                    "Сетевая ошибка при загрузке AI-модели.",
                cause = throwable,
            )
        } finally {
            activeConnection = null
            connection.disconnect()
        }
    }

    private fun openFollowingRedirects(
        resumeOffset: Long,
    ): HttpURLConnection {
        var currentUrl =
            URI(sourceUrl)
                .toURL()

        repeat(MAX_REDIRECTS + 1) { redirectIndex ->
            if (cancelled.get()) {
                throw RetryableDownloadException(
                    "Загрузка отменена.",
                )
            }

            val connection =
                (currentUrl.openConnection()
                        as HttpURLConnection)
                    .apply {
                        instanceFollowRedirects = false
                        requestMethod = "GET"
                        connectTimeout =
                            connectTimeoutMillis
                        readTimeout =
                            readTimeoutMillis
                        useCaches = false
                        setRequestProperty(
                            "User-Agent",
                            USER_AGENT,
                        )

                        // Match the behavior used by Google AI Edge Gallery:
                        // a fresh download is a normal GET. Range is only sent
                        // when there are bytes that can actually be resumed.
                        // Sending `Range: bytes=0-` on the first request needlessly
                        // forces the Xet/CDN path through range-request handling.
                        if (resumeOffset > 0L) {
                            setRequestProperty(
                                "Accept-Encoding",
                                "identity",
                            )
                            setRequestProperty(
                                "Range",
                                "bytes=$resumeOffset-",
                            )
                        }
                    }

            activeConnection = connection

            val responseCode =
                try {
                    connection.responseCode
                } catch (throwable: Throwable) {
                    connection.disconnect()
                    activeConnection = null

                    if (throwable.isTlsCertificateFailure()) {
                        throw PermanentDownloadException(
                            message =
                                buildTlsFailureMessage(
                                    host = currentUrl.host,
                                    throwable = throwable,
                                ),
                            cause = throwable,
                        )
                    }

                    if (throwable is IOException) {
                        throw throwable
                    }

                    throw RetryableDownloadException(
                        "Не удалось подключиться к серверу модели.",
                        throwable,
                    )
                }

            if (
                responseCode !in
                REDIRECT_STATUS_CODES
            ) {
                return connection
            }

            val location =
                connection.getHeaderField(
                    "Location",
                )
                    ?.takeIf { value ->
                        value.isNotBlank()
                    }

            connection.disconnect()
            activeConnection = null

            if (location == null) {
                throw PermanentDownloadException(
                    "Сервер вернул redirect без адреса загрузки.",
                )
            }

            if (redirectIndex >= MAX_REDIRECTS) {
                throw PermanentDownloadException(
                    "Слишком много перенаправлений при загрузке модели.",
                )
            }

            currentUrl =
                currentUrl
                    .toURI()
                    .resolve(location)
                    .toURL()
        }

        throw PermanentDownloadException(
            "Не удалось определить адрес файла модели.",
        )
    }

    private fun validatePartialResponse(
        connection: HttpURLConnection,
        expectedOffset: Long,
    ) {
        val contentRange =
            connection.getHeaderField(
                "Content-Range",
            )
                ?: return

        val match =
            CONTENT_RANGE_PATTERN
                .matchEntire(
                    contentRange.trim(),
                )
                ?: return

        val start =
            match.groupValues[1]
                .toLongOrNull()
                ?: return

        val total =
            match.groupValues[3]
                .toLongOrNull()

        if (start != expectedOffset) {
            throw PermanentDownloadException(
                "Сервер продолжил загрузку с неправильной позиции.",
            )
        }

        if (
            total != null &&
            total != expectedBytes
        ) {
            throw PermanentDownloadException(
                "Размер модели на сервере изменился. Обновите приложение перед загрузкой.",
            )
        }
    }

    private fun isRetryable(
        throwable: Throwable,
    ): Boolean {
        return throwable is IOException &&
                throwable !is PermanentDownloadException
    }

    private fun waitBeforeRetry(
        attempt: Int,
    ) {
        val multiplier =
            1L shl
                    (attempt - 1)
                        .coerceIn(
                            minimumValue = 0,
                            maximumValue = 3,
                        )

        var remainingMillis =
            retryBaseDelayMillis *
                    multiplier

        while (
            remainingMillis > 0L &&
            !cancelled.get()
        ) {
            val sleepMillis =
                minOf(
                    remainingMillis,
                    RETRY_SLEEP_SLICE_MILLIS,
                )

            try {
                Thread.sleep(
                    sleepMillis,
                )
            } catch (_: InterruptedException) {
                Thread.currentThread()
                    .interrupt()
                return
            }

            remainingMillis -=
                sleepMillis
        }
    }

    private fun Throwable.isTlsCertificateFailure():
            Boolean {
        var current: Throwable? = this

        while (current != null) {
            if (
                current is SSLHandshakeException ||
                current is CertificateException ||
                current.javaClass.name ==
                "java.security.cert.CertPathValidatorException"
            ) {
                return true
            }

            current = current.cause
        }

        return false
    }

    private fun buildTlsFailureMessage(
        host: String?,
        throwable: Throwable,
    ): String {
        val causeMessages =
            generateSequence<Throwable>(throwable) { current ->
                current.cause
            }
                .mapNotNull { current ->
                    current.message
                        ?.trim()
                        ?.takeIf { message ->
                            message.isNotBlank()
                        }
                }
                .distinct()
                .take(3)
                .toList()

        val hostLabel =
            host
                ?.takeIf { value ->
                    value.isNotBlank()
                }
                ?: "сервером модели"

        val detail =
            causeMessages
                .joinToString(
                    separator = " → ",
                )
                .takeIf { value ->
                    value.isNotBlank()
                }

        return buildString {
            append(
                "Не удалось проверить TLS-сертификат для $hostLabel.",
            )

            if (detail != null) {
                append(' ')
                append(detail)
            }

            append(
                " Проверьте автоматические дату и время Android, а также VPN/proxy/HTTPS-фильтрацию.",
            )
        }
    }

    private fun Throwable.toUserMessage():
            String {
        val detail =
            message
                ?.takeIf { value ->
                    value.isNotBlank()
                }

        return if (detail != null) {
            "Не удалось скачать AI-модель. $detail"
        } else {
            "Не удалось скачать AI-модель. Проверьте подключение к интернету и повторите попытку."
        }
    }

    private companion object {

        const val DEFAULT_CONNECT_TIMEOUT_MILLIS =
            15_000

        const val DEFAULT_READ_TIMEOUT_MILLIS =
            30_000

        const val DEFAULT_MAX_ATTEMPTS =
            5

        const val DEFAULT_RETRY_BASE_DELAY_MILLIS =
            1_000L

        const val DEFAULT_BUFFER_SIZE_BYTES =
            1024 * 1024

        const val RETRY_SLEEP_SLICE_MILLIS =
            100L

        const val MAX_REDIRECTS =
            8

        const val HTTP_FORBIDDEN =
            403

        const val HTTP_REQUEST_TIMEOUT =
            408

        const val HTTP_TOO_MANY_REQUESTS =
            429

        const val HTTP_RANGE_NOT_SATISFIABLE =
            416

        const val HTTP_SERVER_ERROR_MIN =
            500

        const val HTTP_SERVER_ERROR_MAX =
            599

        val REDIRECT_STATUS_CODES =
            setOf(
                HttpURLConnection.HTTP_MOVED_PERM,
                HttpURLConnection.HTTP_MOVED_TEMP,
                HttpURLConnection.HTTP_SEE_OTHER,
                307,
                308,
            )

        val CONTENT_RANGE_PATTERN =
            Regex(
                pattern =
                    "bytes\\s+(\\d+)-(\\d+)/(\\d+|\\*)",
                option = RegexOption.IGNORE_CASE,
            )

        const val USER_AGENT =
            "FinFlow-Android/1.0"
    }
}