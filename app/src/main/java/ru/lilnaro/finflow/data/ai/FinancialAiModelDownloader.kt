package ru.lilnaro.finflow.data.ai

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.StatFs
import java.io.File
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import ru.lilnaro.finflow.domain.repository.FinancialAiModelDownloadInfo
import ru.lilnaro.finflow.domain.repository.FinancialAiModelDownloadStartResult
import ru.lilnaro.finflow.domain.repository.FinancialAiModelDownloadState

class FinancialAiModelDownloader(
    private val context: Context,
) {

    private val downloadManager =
        context.getSystemService(
            Context.DOWNLOAD_SERVICE,
        ) as DownloadManager

    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    fun getDownloadInfo():
            FinancialAiModelDownloadInfo {
        return FinancialAiModelDownloadInfo(
            approximateDownloadBytes =
                MODEL_DOWNLOAD_SIZE_BYTES,
            minimumRequiredFreeSpaceBytes =
                MINIMUM_REQUIRED_FREE_SPACE_BYTES,
        )
    }

    fun observeState():
            Flow<FinancialAiModelDownloadState> {
        return flow {
            while (
                currentCoroutineContext()
                    .isActive
            ) {
                val state =
                    readState()

                emit(state)

                if (
                    state !is
                            FinancialAiModelDownloadState.Downloading
                ) {
                    break
                }

                delay(
                    PROGRESS_POLL_INTERVAL_MILLIS,
                )
            }
        }
    }

    suspend fun startDownload():
            FinancialAiModelDownloadStartResult {
        val targetFile =
            resolveTargetFile()
                ?: return FinancialAiModelDownloadStartResult
                    .Failure(
                        message =
                            "Не удалось получить папку для локальной модели.",
                    )

        if (
            resolveInstalledModelFile() != null
        ) {
            clearDownloadId()

            return FinancialAiModelDownloadStartResult
                .AlreadyInstalled
        }

        val existingDownloadId =
            readDownloadId()

        if (
            existingDownloadId !=
            NO_DOWNLOAD_ID
        ) {
            when (
                readDownloadManagerStatus(
                    downloadId =
                        existingDownloadId,
                )
            ) {
                DownloadManager.STATUS_PENDING,
                DownloadManager.STATUS_RUNNING,
                DownloadManager.STATUS_PAUSED,
                    -> {
                    return FinancialAiModelDownloadStartResult
                        .AlreadyRunning
                }

                else -> {
                    clearDownloadId()
                }
            }
        }

        targetFile.parentFile
            ?.mkdirs()

        val availableBytes =
            getAvailableBytes(
                directory =
                    targetFile.parentFile,
            )

        if (
            availableBytes <
            MINIMUM_REQUIRED_FREE_SPACE_BYTES
        ) {
            return FinancialAiModelDownloadStartResult
                .NotEnoughSpace(
                    availableBytes =
                        availableBytes,
                    requiredBytes =
                        MINIMUM_REQUIRED_FREE_SPACE_BYTES,
                )
        }

        return try {
            deleteCompletionMarker()
            deleteStaleModelFiles()

            val request =
                DownloadManager.Request(
                    Uri.parse(
                        MODEL_DOWNLOAD_URL,
                    ),
                )
                    .setTitle(
                        "FinFlow AI",
                    )
                    .setDescription(
                        "Загрузка локального AI-модуля (~3,7 ГБ)",
                    )
                    .setMimeType(
                        "application/octet-stream",
                    )
                    .setNotificationVisibility(
                        DownloadManager.Request
                            .VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
                    )
                    // Пользователь заранее видит размер загрузки
                    // и запускает её вручную, поэтому не блокируем
                    // DownloadManager на metered-сетях. На эмуляторах
                    // виртуальная сеть нередко помечается как metered,
                    // из-за чего загрузка иначе может навсегда остаться на 0%.
                    .setAllowedOverMetered(
                        true,
                    )
                    .setAllowedOverRoaming(
                        false,
                    )
                    .setDestinationInExternalFilesDir(
                        context,
                        null,
                        "$MODEL_DIRECTORY_NAME/$MODEL_FILE_NAME",
                    )

            val downloadId =
                downloadManager.enqueue(
                    request,
                )

            saveDownloadId(
                downloadId = downloadId,
            )

            FinancialAiModelDownloadStartResult
                .Started
        } catch (_: Throwable) {
            FinancialAiModelDownloadStartResult
                .Failure(
                    message =
                        "Не удалось начать загрузку локальной модели.",
                )
        }
    }

    suspend fun cancelDownload() {
        val downloadId =
            readDownloadId()

        if (
            downloadId !=
            NO_DOWNLOAD_ID
        ) {
            downloadManager.remove(
                downloadId,
            )
        }

        clearDownloadId()
        deleteCompletionMarker()

        resolveTargetFile()
            ?.delete()
    }

    private fun readState():
            FinancialAiModelDownloadState {
        val targetFile =
            resolveTargetFile()

        val downloadId =
            readDownloadId()

        if (
            downloadId !=
            NO_DOWNLOAD_ID
        ) {
            return readActiveDownloadState(
                downloadId = downloadId,
                targetFile = targetFile,
            )
        }

        if (
            resolveInstalledModelFile() != null
        ) {
            return FinancialAiModelDownloadState
                .Installed
        }

        deleteCompletionMarker()
        deleteStaleModelFiles()

        return FinancialAiModelDownloadState
            .NotInstalled
    }

    private fun readActiveDownloadState(
        downloadId: Long,
        targetFile: File?,
    ): FinancialAiModelDownloadState {
        val query =
            DownloadManager.Query()
                .setFilterById(
                    downloadId,
                )

        return try {
            downloadManager
                .query(query)
                .use { cursor ->
                    if (
                        cursor == null ||
                        !cursor.moveToFirst()
                    ) {
                        clearDownloadId()
                        deleteCompletionMarker()

                        targetFile
                            ?.delete()

                        return FinancialAiModelDownloadState
                            .NotInstalled
                    }

                    val status =
                        cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                DownloadManager
                                    .COLUMN_STATUS,
                            ),
                        )

                    when (status) {
                        DownloadManager.STATUS_PENDING,
                        DownloadManager.STATUS_RUNNING,
                        DownloadManager.STATUS_PAUSED,
                            -> {
                            val downloadedBytes =
                                cursor.getLong(
                                    cursor.getColumnIndexOrThrow(
                                        DownloadManager
                                            .COLUMN_BYTES_DOWNLOADED_SO_FAR,
                                    ),
                                )
                                    .coerceAtLeast(0L)

                            val rawTotalBytes =
                                cursor.getLong(
                                    cursor.getColumnIndexOrThrow(
                                        DownloadManager
                                            .COLUMN_TOTAL_SIZE_BYTES,
                                    ),
                                )

                            FinancialAiModelDownloadState
                                .Downloading(
                                    downloadedBytes =
                                        downloadedBytes,
                                    totalBytes =
                                        rawTotalBytes
                                            .takeIf { total ->
                                                total > 0L
                                            },
                                )
                        }

                        DownloadManager.STATUS_SUCCESSFUL -> {
                            val downloadedFile =
                                resolveDownloadedFile(
                                    cursor = cursor,
                                )
                                    ?: findValidDownloadedModelCandidate()

                            clearDownloadId()

                            val installedFile =
                                downloadedFile
                                    ?.let { file ->
                                        normalizeDownloadedModelFile(
                                            downloadedFile = file,
                                        )
                                    }

                            if (installedFile != null) {
                                FinancialAiModelDownloadState
                                    .Installed
                            } else {
                                deleteCompletionMarker()
                                deleteStaleModelFiles()

                                FinancialAiModelDownloadState
                                    .Failed(
                                        message =
                                            "Загрузка завершилась, но файл модели повреждён или неполный.",
                                    )
                            }
                        }

                        DownloadManager.STATUS_FAILED -> {
                            clearDownloadId()
                            deleteCompletionMarker()

                            targetFile
                                ?.delete()

                            FinancialAiModelDownloadState
                                .Failed(
                                    message =
                                        "Не удалось скачать локальную модель. Проверьте интернет и свободное место.",
                                )
                        }

                        else -> {
                            FinancialAiModelDownloadState
                                .Downloading(
                                    downloadedBytes = 0L,
                                    totalBytes = null,
                                )
                        }
                    }
                }
        } catch (_: Throwable) {
            FinancialAiModelDownloadState
                .Failed(
                    message =
                        "Не удалось проверить состояние загрузки.",
                )
        }
    }

    private fun readDownloadManagerStatus(
        downloadId: Long,
    ): Int? {
        val query =
            DownloadManager.Query()
                .setFilterById(
                    downloadId,
                )

        return try {
            downloadManager
                .query(query)
                .use { cursor ->
                    if (
                        cursor == null ||
                        !cursor.moveToFirst()
                    ) {
                        null
                    } else {
                        cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                DownloadManager
                                    .COLUMN_STATUS,
                            ),
                        )
                    }
                }
        } catch (_: Throwable) {
            null
        }
    }

    fun resolveInstalledModelFile():
            File? {
        val targetFile =
            resolveTargetFile()
                ?: return null

        if (
            isCompletedModelFile(
                targetFile,
            )
        ) {
            return targetFile
        }

        val recoveredCandidate =
            findValidDownloadedModelCandidate()
                ?: return null

        return normalizeDownloadedModelFile(
            downloadedFile =
                recoveredCandidate,
        )
    }

    private fun resolveDownloadedFile(
        cursor: android.database.Cursor,
    ): File? {
        return try {
            val localUriIndex =
                cursor.getColumnIndex(
                    DownloadManager
                        .COLUMN_LOCAL_URI,
                )

            if (localUriIndex < 0) {
                return null
            }

            val localUriValue =
                cursor.getString(
                    localUriIndex,
                )
                    ?: return null

            val localUri =
                Uri.parse(
                    localUriValue,
                )

            if (
                localUri.scheme !=
                "file"
            ) {
                return null
            }

            localUri.path
                ?.let { path ->
                    File(path)
                }
        } catch (_: Throwable) {
            null
        }
    }

    private fun findValidDownloadedModelCandidate():
            File? {
        val targetFile =
            resolveTargetFile()
                ?: return null

        val directory =
            targetFile.parentFile
                ?: return null

        if (!directory.exists()) {
            return null
        }

        return directory
            .listFiles()
            ?.asSequence()
            ?.filter { file ->
                isModelFileCandidate(
                    file = file,
                )
            }
            ?.filter { file ->
                isValidModelFile(
                    file = file,
                )
            }
            ?.maxByOrNull { file ->
                file.lastModified()
            }
    }

    private fun normalizeDownloadedModelFile(
        downloadedFile: File,
    ): File? {
        if (
            !isValidModelFile(
                downloadedFile,
            )
        ) {
            return null
        }

        val targetFile =
            resolveTargetFile()
                ?: return null

        if (
            downloadedFile.absolutePath !=
            targetFile.absolutePath
        ) {
            if (
                targetFile.exists() &&
                !targetFile.delete()
            ) {
                return null
            }

            if (
                !downloadedFile.renameTo(
                    targetFile,
                )
            ) {
                return null
            }
        }

        if (
            !isValidModelFile(
                targetFile,
            )
        ) {
            return null
        }

        deleteCompletionMarker()

        return if (
            createCompletionMarker()
        ) {
            targetFile
        } else {
            null
        }
    }

    private fun deleteStaleModelFiles() {
        val targetFile =
            resolveTargetFile()
                ?: return

        val directory =
            targetFile.parentFile
                ?: return

        directory
            .listFiles()
            ?.filter { file ->
                isModelFileCandidate(
                    file = file,
                )
            }
            ?.forEach { file ->
                file.delete()
            }
    }

    private fun isModelFileCandidate(
        file: File,
    ): Boolean {
        val fileName =
            file.name

        return file.isFile &&
                fileName.startsWith(
                    MODEL_FILE_BASENAME,
                ) &&
                fileName.endsWith(
                    MODEL_FILE_EXTENSION,
                )
    }

    private fun isCompletedModelFile(
        file: File,
    ): Boolean {
        val completionMarker =
            resolveCompletionMarker()
                ?: return false

        return isValidModelFile(file) &&
                completionMarker.exists() &&
                completionMarker.isFile
    }

    private fun createCompletionMarker():
            Boolean {
        val marker =
            resolveCompletionMarker()
                ?: return false

        return try {
            marker.parentFile
                ?.mkdirs()

            if (marker.exists()) {
                marker.delete()
            }

            marker.createNewFile()
        } catch (_: Throwable) {
            false
        }
    }

    private fun deleteCompletionMarker() {
        resolveCompletionMarker()
            ?.delete()
    }

    private fun resolveCompletionMarker():
            File? {
        val targetFile =
            resolveTargetFile()
                ?: return null

        return File(
            targetFile.parentFile,
            "$MODEL_FILE_NAME$COMPLETION_MARKER_SUFFIX",
        )
    }

    private fun resolveTargetFile():
            File? {
        val externalFilesDirectory =
            context.getExternalFilesDir(
                null,
            )
                ?: return null

        return File(
            File(
                externalFilesDirectory,
                MODEL_DIRECTORY_NAME,
            ),
            MODEL_FILE_NAME,
        )
    }

    private fun getAvailableBytes(
        directory: File?,
    ): Long {
        val path =
            directory
                ?.absolutePath
                ?: context.filesDir.absolutePath

        return try {
            StatFs(path).availableBytes
        } catch (_: Throwable) {
            0L
        }
    }

    private fun isValidModelFile(
        file: File,
    ): Boolean {
        return file.exists() &&
                file.isFile &&
                file.length() >=
                MIN_MODEL_FILE_SIZE_BYTES
    }

    private fun readDownloadId(): Long {
        return preferences.getLong(
            DOWNLOAD_ID_KEY,
            NO_DOWNLOAD_ID,
        )
    }

    private fun saveDownloadId(
        downloadId: Long,
    ) {
        preferences
            .edit()
            .putLong(
                DOWNLOAD_ID_KEY,
                downloadId,
            )
            .apply()
    }

    private fun clearDownloadId() {
        preferences
            .edit()
            .remove(
                DOWNLOAD_ID_KEY,
            )
            .apply()
    }

    companion object {

        const val MODEL_DIRECTORY_NAME =
            "models"

        const val MODEL_FILE_NAME =
            "gemma-4-E4B-it.litertlm"

        private const val MODEL_FILE_BASENAME =
            "gemma-4-E4B-it"

        private const val MODEL_FILE_EXTENSION =
            ".litertlm"

        const val MIN_MODEL_FILE_SIZE_BYTES =
            3_500_000_000L

        const val MODEL_DOWNLOAD_SIZE_BYTES =
            3_700_000_000L

        const val MINIMUM_REQUIRED_FREE_SPACE_BYTES =
            5_700_000_000L

        const val MODEL_DOWNLOAD_URL =
            "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm?download=true"

        private const val COMPLETION_MARKER_SUFFIX =
            ".ready"

        private const val PREFERENCES_NAME =
            "financial_ai_model_download"

        private const val DOWNLOAD_ID_KEY =
            "download_id"

        private const val NO_DOWNLOAD_ID =
            -1L

        private const val PROGRESS_POLL_INTERVAL_MILLIS =
            1_000L
    }
}