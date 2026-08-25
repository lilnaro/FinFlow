package ru.lilnaro.finflow.data.ai

import android.app.DownloadManager
import android.content.Context
import android.os.StatFs
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.lilnaro.finflow.domain.repository.FinancialAiModelDownloadInfo
import ru.lilnaro.finflow.domain.repository.FinancialAiModelDownloadStartResult
import ru.lilnaro.finflow.domain.repository.FinancialAiModelDownloadState

class FinancialAiModelDownloader(
    context: Context,
) {

    private val appContext =
        context.applicationContext

    private val downloadScope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.IO,
        )

    private val operationMutex =
        Mutex()

    private val downloadedBytes =
        AtomicLong(0L)

    private val totalBytes =
        AtomicLong(
            MODEL_DOWNLOAD_SIZE_BYTES,
        )

    @Volatile
    private var downloadJob: Job? = null

    @Volatile
    private var activeDownloader:
            ResumableHttpFileDownloader? = null

    @Volatile
    private var lastFailureMessage:
            String? = null

    private val legacyPreferences =
        appContext.getSharedPreferences(
            LEGACY_PREFERENCES_NAME,
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
        return operationMutex.withLock {
            recoverCompletedTargetIfPossible()

            if (
                resolveInstalledModelFile() != null
            ) {
                cleanupLegacySystemDownload()

                return@withLock FinancialAiModelDownloadStartResult
                    .AlreadyInstalled
            }

            if (
                downloadJob?.isActive == true
            ) {
                return@withLock FinancialAiModelDownloadStartResult
                    .AlreadyRunning
            }

            cleanupLegacySystemDownload()

            val targetFile =
                resolveTargetFile()
                    ?: return@withLock FinancialAiModelDownloadStartResult
                        .Failure(
                            message =
                                "Не удалось получить папку для локальной модели.",
                        )

            val partialFile =
                resolvePartialFile()
                    ?: return@withLock FinancialAiModelDownloadStartResult
                        .Failure(
                            message =
                                "Не удалось подготовить временный файл локальной модели.",
                        )

            val directory =
                targetFile.parentFile
                    ?: return@withLock FinancialAiModelDownloadStartResult
                        .Failure(
                            message =
                                "Не удалось получить папку для локальной модели.",
                        )

            if (
                !directory.exists() &&
                !directory.mkdirs()
            ) {
                return@withLock FinancialAiModelDownloadStartResult
                    .Failure(
                        message =
                            "Не удалось создать папку для локальной модели.",
                    )
            }

            deleteInvalidTargetFile()

            if (
                partialFile.exists() &&
                partialFile.length() >
                MODEL_DOWNLOAD_SIZE_BYTES
            ) {
                partialFile.delete()
            }

            val alreadyDownloadedBytes =
                partialFile
                    .takeIf { file ->
                        file.exists()
                    }
                    ?.length()
                    ?.coerceIn(
                        minimumValue = 0L,
                        maximumValue =
                            MODEL_DOWNLOAD_SIZE_BYTES,
                    )
                    ?: 0L

            val requiredFreeBytes =
                calculateRequiredFreeSpace(
                    alreadyDownloadedBytes =
                        alreadyDownloadedBytes,
                )

            val availableBytes =
                getAvailableBytes(
                    directory = directory,
                )

            if (
                availableBytes <
                requiredFreeBytes
            ) {
                return@withLock FinancialAiModelDownloadStartResult
                    .NotEnoughSpace(
                        availableBytes =
                            availableBytes,
                        requiredBytes =
                            requiredFreeBytes,
                    )
            }

            downloadedBytes.set(
                alreadyDownloadedBytes,
            )
            totalBytes.set(
                MODEL_DOWNLOAD_SIZE_BYTES,
            )
            lastFailureMessage = null

            val downloader =
                ResumableHttpFileDownloader(
                    sourceUrl =
                        MODEL_DOWNLOAD_URL,
                    expectedBytes =
                        MODEL_DOWNLOAD_SIZE_BYTES,
                )

            activeDownloader =
                downloader

            downloadJob =
                downloadScope.launch {
                    val result =
                        downloader.download(
                            partialFile =
                                partialFile,
                            onProgress =
                                { currentBytes,
                                  currentTotalBytes ->
                                    downloadedBytes.set(
                                        currentBytes,
                                    )
                                    totalBytes.set(
                                        currentTotalBytes,
                                    )
                                },
                        )

                    when (result) {
                        ResumableHttpFileDownloader
                            .Result.Success -> {
                            if (
                                currentCoroutineContext()
                                    .isActive
                            ) {
                                val installed =
                                    finalizeDownloadedModel(
                                        partialFile =
                                            partialFile,
                                        targetFile =
                                            targetFile,
                                    )

                                if (!installed) {
                                    lastFailureMessage =
                                        "Загрузка завершилась, но файл модели не удалось подготовить к запуску."
                                }
                            }
                        }

                        ResumableHttpFileDownloader
                            .Result.Cancelled -> Unit

                        is ResumableHttpFileDownloader
                        .Result.Failure -> {
                            lastFailureMessage =
                                result.message
                        }
                    }

                    if (
                        activeDownloader ===
                        downloader
                    ) {
                        activeDownloader = null
                    }
                }

            FinancialAiModelDownloadStartResult
                .Started
        }
    }

    suspend fun cancelDownload() {
        operationMutex.withLock {
            cleanupLegacySystemDownload()

            val downloader =
                activeDownloader
            val job =
                downloadJob

            downloader?.cancel()
            job?.cancelAndJoin()

            if (
                activeDownloader ===
                downloader
            ) {
                activeDownloader = null
            }

            downloadJob = null
            lastFailureMessage = null
            downloadedBytes.set(0L)
            totalBytes.set(
                MODEL_DOWNLOAD_SIZE_BYTES,
            )

            recoverCompletedTargetIfPossible()

            if (
                resolveInstalledModelFile() == null
            ) {
                deleteCompletionMarker()
                resolvePartialFile()
                    ?.delete()
                deleteInvalidTargetFile()
            }
        }
    }

    fun resolveInstalledModelFile():
            File? {
        val targetFile =
            resolveTargetFile()
                ?: return null

        return targetFile.takeIf { file ->
            isCompletedModelFile(
                file = file,
            )
        }
    }

    private fun readState():
            FinancialAiModelDownloadState {
        recoverCompletedTargetIfPossible()

        if (
            resolveInstalledModelFile() != null
        ) {
            return FinancialAiModelDownloadState
                .Installed
        }

        if (
            downloadJob?.isActive == true
        ) {
            return FinancialAiModelDownloadState
                .Downloading(
                    downloadedBytes =
                        downloadedBytes
                            .get()
                            .coerceAtLeast(0L),
                    totalBytes =
                        totalBytes
                            .get()
                            .takeIf { bytes ->
                                bytes > 0L
                            },
                )
        }

        lastFailureMessage
            ?.let { message ->
                return FinancialAiModelDownloadState
                    .Failed(
                        message = message,
                    )
            }

        cleanupLegacySystemDownload()
        deleteInvalidTargetFile()

        val partialBytes =
            resolvePartialFile()
                ?.takeIf { file ->
                    file.exists()
                }
                ?.length()
                ?.coerceIn(
                    minimumValue = 0L,
                    maximumValue =
                        MODEL_DOWNLOAD_SIZE_BYTES,
                )
                ?: 0L

        downloadedBytes.set(
            partialBytes,
        )

        return FinancialAiModelDownloadState
            .NotInstalled
    }

    private fun finalizeDownloadedModel(
        partialFile: File,
        targetFile: File,
    ): Boolean {
        if (
            !partialFile.exists() ||
            partialFile.length() !=
            MODEL_DOWNLOAD_SIZE_BYTES
        ) {
            return false
        }

        deleteCompletionMarker()

        if (
            targetFile.exists() &&
            !targetFile.delete()
        ) {
            return false
        }

        if (
            !partialFile.renameTo(
                targetFile,
            )
        ) {
            return false
        }

        if (
            !isValidModelFile(
                file = targetFile,
            )
        ) {
            targetFile.delete()
            return false
        }

        return createCompletionMarker()
    }

    private fun recoverCompletedTargetIfPossible() {
        val targetFile =
            resolveTargetFile()
                ?: return

        if (
            isValidModelFile(
                file = targetFile,
            ) &&
            resolveCompletionMarker()
                ?.exists() != true
        ) {
            createCompletionMarker()
        }

        if (
            resolveCompletionMarker()
                ?.exists() == true &&
            !isValidModelFile(
                file = targetFile,
            )
        ) {
            deleteCompletionMarker()
        }
    }

    private fun deleteInvalidTargetFile() {
        val targetFile =
            resolveTargetFile()
                ?: return

        if (
            targetFile.exists() &&
            !isValidModelFile(
                file = targetFile,
            )
        ) {
            targetFile.delete()
            deleteCompletionMarker()
        }
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

    private fun isValidModelFile(
        file: File,
    ): Boolean {
        return file.exists() &&
                file.isFile &&
                file.length() ==
                MODEL_DOWNLOAD_SIZE_BYTES
    }

    private fun createCompletionMarker():
            Boolean {
        val marker =
            resolveCompletionMarker()
                ?: return false

        return try {
            marker.parentFile
                ?.mkdirs()

            if (
                marker.exists() &&
                !marker.delete()
            ) {
                return false
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

    private fun resolveTargetFile():
            File? {
        val externalFilesDirectory =
            appContext.getExternalFilesDir(
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

    private fun resolvePartialFile():
            File? {
        val targetFile =
            resolveTargetFile()
                ?: return null

        return File(
            targetFile.parentFile,
            "$MODEL_FILE_NAME$PARTIAL_FILE_SUFFIX",
        )
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

    private fun calculateRequiredFreeSpace(
        alreadyDownloadedBytes: Long,
    ): Long {
        val remainingBytes =
            (
                    MODEL_DOWNLOAD_SIZE_BYTES -
                            alreadyDownloadedBytes
                    )
                .coerceAtLeast(0L)

        return remainingBytes +
                DOWNLOAD_FREE_SPACE_HEADROOM_BYTES
    }

    private fun getAvailableBytes(
        directory: File,
    ): Long {
        return try {
            StatFs(
                directory.absolutePath,
            ).availableBytes
        } catch (_: Throwable) {
            0L
        }
    }

    private fun cleanupLegacySystemDownload() {
        val downloadId =
            legacyPreferences.getLong(
                LEGACY_DOWNLOAD_ID_KEY,
                NO_DOWNLOAD_ID,
            )

        if (
            downloadId !=
            NO_DOWNLOAD_ID
        ) {
            try {
                val downloadManager =
                    appContext.getSystemService(
                        Context.DOWNLOAD_SERVICE,
                    ) as? DownloadManager

                downloadManager?.remove(
                    downloadId,
                )
            } catch (_: Throwable) {
                // The legacy system task is best-effort cleanup only.
            }
        }

        legacyPreferences
            .edit()
            .clear()
            .apply()
    }

    companion object {

        const val MODEL_DIRECTORY_NAME =
            "models"

        const val MODEL_FILE_NAME =
            "gemma-4-E4B-it.litertlm"

        const val MIN_MODEL_FILE_SIZE_BYTES =
            3_500_000_000L

        const val MODEL_DOWNLOAD_SIZE_BYTES =
            3_659_530_240L

        private const val DOWNLOAD_FREE_SPACE_HEADROOM_BYTES =
            2_000_000_000L

        const val MINIMUM_REQUIRED_FREE_SPACE_BYTES =
            MODEL_DOWNLOAD_SIZE_BYTES +
                    DOWNLOAD_FREE_SPACE_HEADROOM_BYTES

        const val MODEL_DOWNLOAD_URL =
            "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/28299f30ee4d43294517a4ac93abd6163412f07f/gemma-4-E4B-it.litertlm?download=true"

        private const val PARTIAL_FILE_SUFFIX =
            ".part"

        private const val COMPLETION_MARKER_SUFFIX =
            ".ready"

        private const val LEGACY_PREFERENCES_NAME =
            "financial_ai_model_download"

        private const val LEGACY_DOWNLOAD_ID_KEY =
            "download_id"

        private const val NO_DOWNLOAD_ID =
            -1L

        private const val PROGRESS_POLL_INTERVAL_MILLIS =
            500L
    }
}