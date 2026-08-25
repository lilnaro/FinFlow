package ru.lilnaro.finflow.data.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.lilnaro.finflow.domain.model.FinancialAiAnswerResult
import ru.lilnaro.finflow.domain.model.FinancialAiInitializationResult
import ru.lilnaro.finflow.domain.model.FinancialAiMessage
import ru.lilnaro.finflow.domain.model.FinancialAiMessageRole
import ru.lilnaro.finflow.domain.model.FinancialAnalysisContext
import ru.lilnaro.finflow.domain.model.FinancialAnalyticsSnapshot
import ru.lilnaro.finflow.domain.repository.FinancialAiModelDownloadInfo
import ru.lilnaro.finflow.domain.repository.FinancialAiModelDownloadStartResult
import ru.lilnaro.finflow.domain.repository.FinancialAiModelDownloadState
import ru.lilnaro.finflow.domain.repository.FinancialAiRepository

class GemmaFinancialAiRepository(
    private val context: Context,
    private val promptBuilder:
    FinancialAiPromptBuilder,
    private val modelDownloader:
    FinancialAiModelDownloader,
) : FinancialAiRepository {

    private val engineMutex =
        Mutex()

    private val generationMutex =
        Mutex()

    @Volatile
    private var engine: Engine? = null

    override fun isModelAvailable(): Boolean {
        return resolveModelFile() != null
    }

    override fun getModelDownloadInfo():
            FinancialAiModelDownloadInfo {
        return modelDownloader
            .getDownloadInfo()
    }

    override fun observeModelDownloadState():
            Flow<FinancialAiModelDownloadState> {
        if (isModelAvailable()) {
            return flowOf(
                FinancialAiModelDownloadState
                    .Installed,
            )
        }

        return modelDownloader
            .observeState()
    }

    override suspend fun startModelDownload():
            FinancialAiModelDownloadStartResult {
        if (isModelAvailable()) {
            return FinancialAiModelDownloadStartResult
                .AlreadyInstalled
        }

        return modelDownloader
            .startDownload()
    }

    override suspend fun cancelModelDownload() {
        modelDownloader.cancelDownload()
    }

    override suspend fun initialize():
            FinancialAiInitializationResult {
        val modelFile =
            resolveModelFile()
                ?: return FinancialAiInitializationResult
                    .ModelMissing(
                        expectedFileName =
                            MODEL_FILE_NAME,
                    )

        return try {
            ensureEngine(
                modelFile = modelFile,
            )

            FinancialAiInitializationResult.Ready
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            FinancialAiInitializationResult.Failure(
                message =
                    "Не удалось запустить локальную модель.",
            )
        }
    }

    override suspend fun ask(
        question: String,
        analysisContext: FinancialAnalysisContext,
        analyticsSnapshot: FinancialAnalyticsSnapshot,
        conversationHistory:
        List<FinancialAiMessage>,
    ): FinancialAiAnswerResult {
        val normalizedQuestion =
            question.trim()

        if (normalizedQuestion.isEmpty()) {
            return FinancialAiAnswerResult.Failure(
                message =
                    "Вопрос не должен быть пустым.",
            )
        }

        val modelFile =
            resolveModelFile()
                ?: return FinancialAiAnswerResult.Failure(
                    message =
                        "Файл локальной модели не найден.",
                )

        return generationMutex.withLock {
            try {
                val currentEngine =
                    ensureEngine(
                        modelFile = modelFile,
                    )

                val systemInstruction =
                    promptBuilder
                        .buildSystemInstruction(
                            question =
                                normalizedQuestion,
                            analysisContext =
                                analysisContext,
                            analyticsSnapshot =
                                analyticsSnapshot,
                        )

                val initialMessages =
                    promptBuilder
                        .buildConversationHistory(
                            history =
                                conversationHistory,
                        )
                        .map { message ->
                            when (message.role) {
                                FinancialAiMessageRole.USER -> {
                                    Message.user(
                                        message.text,
                                    )
                                }

                                FinancialAiMessageRole.ASSISTANT -> {
                                    Message.model(
                                        message.text,
                                    )
                                }
                            }
                        }

                val conversationConfig =
                    ConversationConfig(
                        systemInstruction =
                            Contents.of(
                                systemInstruction,
                            ),
                        initialMessages =
                            initialMessages,
                        samplerConfig =
                            SamplerConfig(
                                topK = 40,
                                topP = 0.90,
                                temperature = 0.35,
                                seed = 42,
                            ),
                    )

                val answer =
                    withContext(
                        Dispatchers.Default,
                    ) {
                        currentEngine
                            .createConversation(
                                conversationConfig,
                            )
                            .use { conversation ->
                                conversation
                                    .sendMessage(
                                        normalizedQuestion,
                                    )
                                    .contents
                                    .toString()
                                    .trim()
                            }
                    }

                if (answer.isBlank()) {
                    FinancialAiAnswerResult.Failure(
                        message =
                            "Локальная модель вернула пустой ответ.",
                    )
                } else {
                    FinancialAiAnswerResult.Success(
                        answer = answer,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                FinancialAiAnswerResult.Failure(
                    message =
                        "Не удалось получить ответ от локальной модели.",
                )
            }
        }
    }

    override fun close() {
        engine?.close()
        engine = null
    }

    private suspend fun ensureEngine(
        modelFile: File,
    ): Engine {
        engine?.let { currentEngine ->
            return currentEngine
        }

        return engineMutex.withLock {
            engine?.let { currentEngine ->
                return@withLock currentEngine
            }

            val initializedEngine =
                withContext(
                    Dispatchers.Default,
                ) {
                    initializeEngine(
                        modelFile = modelFile,
                    )
                }

            engine =
                initializedEngine

            initializedEngine
        }
    }

    private fun initializeEngine(
        modelFile: File,
    ): Engine {
        Engine.setNativeMinLogSeverity(
            LogSeverity.ERROR,
        )

        return try {
            createAndInitializeEngine(
                modelFile = modelFile,
                backend = Backend.GPU(),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            createAndInitializeEngine(
                modelFile = modelFile,
                backend =
                    Backend.CPU(),
            )
        }
    }

    private fun createAndInitializeEngine(
        modelFile: File,
        backend: Backend,
    ): Engine {
        val newEngine =
            Engine(
                EngineConfig(
                    modelPath =
                        modelFile.absolutePath,
                    backend = backend,
                    maxNumTokens =
                        MAX_CONTEXT_TOKENS,
                    cacheDir =
                        context.cacheDir
                            .absolutePath,
                ),
            )

        return try {
            newEngine.initialize()
            newEngine
        } catch (throwable: Throwable) {
            newEngine.close()
            throw throwable
        }
    }

    private fun resolveModelFile(): File? {
        val privateModel =
            File(
                File(
                    context.filesDir,
                    MODEL_DIRECTORY_NAME,
                ),
                MODEL_FILE_NAME,
            )

        if (
            privateModel.exists() &&
            privateModel.length() >=
            MIN_MODEL_FILE_SIZE_BYTES
        ) {
            return privateModel
        }

        val installedExternalModel =
            modelDownloader
                .resolveInstalledModelFile()

        if (installedExternalModel != null) {
            return installedExternalModel
        }

        val stagedModel =
            File(
                DEBUG_STAGED_MODEL_PATH,
            )

        if (
            stagedModel.exists() &&
            stagedModel.length() >=
            MIN_MODEL_FILE_SIZE_BYTES
        ) {
            return stagedModel
        }

        return null
    }

    private companion object {

        const val MODEL_DIRECTORY_NAME =
            FinancialAiModelDownloader.MODEL_DIRECTORY_NAME

        const val MODEL_FILE_NAME =
            FinancialAiModelDownloader.MODEL_FILE_NAME

        const val DEBUG_STAGED_MODEL_PATH =
            "/data/local/tmp/llm/gemma-4-E4B-it.litertlm"

        const val MIN_MODEL_FILE_SIZE_BYTES =
            FinancialAiModelDownloader.MIN_MODEL_FILE_SIZE_BYTES

        const val MAX_CONTEXT_TOKENS =
            8_192

    }
}