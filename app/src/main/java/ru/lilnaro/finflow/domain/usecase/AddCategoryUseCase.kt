package ru.lilnaro.finflow.domain.usecase

import kotlinx.coroutines.flow.first
import ru.lilnaro.finflow.domain.model.TransactionCategory
import ru.lilnaro.finflow.domain.model.TransactionType
import ru.lilnaro.finflow.domain.model.result.AddCategoryResult
import ru.lilnaro.finflow.domain.repository.FinanceRepository

class AddCategoryUseCase(
    private val financeRepository: FinanceRepository,
) {

    suspend operator fun invoke(
        rawName: String,
        type: TransactionType,
        parentCategoryId: Long,
    ): AddCategoryResult {
        val normalizedDisplayName =
            normalizeDisplayName(rawName)

        if (
            normalizedDisplayName.length <
            TransactionCategory.MIN_NAME_LENGTH
        ) {
            return AddCategoryResult.NameTooShort(
                actualLength = normalizedDisplayName.length,
                minimumLength =
                    TransactionCategory.MIN_NAME_LENGTH,
            )
        }

        if (
            normalizedDisplayName.length >
            TransactionCategory.MAX_NAME_LENGTH
        ) {
            return AddCategoryResult.NameTooLong(
                actualLength = normalizedDisplayName.length,
                maximumLength =
                    TransactionCategory.MAX_NAME_LENGTH,
            )
        }

        if (
            normalizedDisplayName.none { character ->
                character.isLetter()
            }
        ) {
            return AddCategoryResult.NameMustContainLetter
        }

        val invalidCharacters =
            normalizedDisplayName
                .filterNot { character ->
                    character.isLetterOrDigit() ||
                            character in
                            TransactionCategory
                                .ALLOWED_NAME_CHARACTERS
                }
                .toSet()

        if (invalidCharacters.isNotEmpty()) {
            return AddCategoryResult
                .NameContainsInvalidCharacters(
                    invalidCharacters = invalidCharacters,
                )
        }

        if (parentCategoryId <= 0L) {
            return AddCategoryResult.InvalidParentCategoryId(
                parentCategoryId = parentCategoryId,
            )
        }

        val parentCategory =
            financeRepository.getCategoryById(
                categoryId = parentCategoryId,
            )
                ?: return AddCategoryResult
                    .ParentCategoryNotFound(
                        parentCategoryId = parentCategoryId,
                    )

        if (parentCategory.isCustom) {
            return AddCategoryResult
                .ParentCategoryMustBeBuiltIn(
                    parentCategoryId = parentCategory.id,
                )
        }

        if (parentCategory.type != type) {
            return AddCategoryResult
                .ParentCategoryTypeMismatch(
                    categoryType = type,
                    parentCategoryType = parentCategory.type,
                )
        }

        val normalizedComparisonName =
            normalizeForComparison(
                name = normalizedDisplayName,
            )

        val categoriesOfSameType =
            financeRepository
                .observeCategoriesByType(type)
                .first()

        val existingCategory =
            categoriesOfSameType.firstOrNull { category ->
                normalizeForComparison(category.name) ==
                        normalizedComparisonName
            }

        if (existingCategory != null) {
            return AddCategoryResult.CategoryAlreadyExists(
                existingCategoryId = existingCategory.id,
                existingCategoryName = existingCategory.name,
            )
        }

        val category = TransactionCategory(
            name = normalizedDisplayName,
            type = type,
            isCustom = true,
            parentCategoryId = parentCategory.id,
        )

        val categoryId =
            financeRepository.addCategory(
                category = category,
            )

        return AddCategoryResult.Success(
            categoryId = categoryId,
        )
    }

    private fun normalizeDisplayName(
        rawName: String,
    ): String {
        return rawName
            .trim()
            .replace(
                regex = MULTIPLE_WHITESPACE_REGEX,
                replacement = " ",
            )
    }

    private fun normalizeForComparison(
        name: String,
    ): String {
        return normalizeDisplayName(name)
            .lowercase()
    }

    private companion object {

        val MULTIPLE_WHITESPACE_REGEX =
            Regex("\\s+")
    }
}