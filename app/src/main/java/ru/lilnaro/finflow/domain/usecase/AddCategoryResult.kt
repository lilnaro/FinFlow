package ru.lilnaro.finflow.domain.usecase

import ru.lilnaro.finflow.domain.model.TransactionType

sealed interface AddCategoryResult {

    data class Success(
        val categoryId: Long,
    ) : AddCategoryResult

    data class NameTooShort(
        val actualLength: Int,
        val minimumLength: Int,
    ) : AddCategoryResult

    data class NameTooLong(
        val actualLength: Int,
        val maximumLength: Int,
    ) : AddCategoryResult

    data object NameMustContainLetter : AddCategoryResult

    data class NameContainsInvalidCharacters(
        val invalidCharacters: Set<Char>,
    ) : AddCategoryResult

    data class InvalidParentCategoryId(
        val parentCategoryId: Long,
    ) : AddCategoryResult

    data class ParentCategoryNotFound(
        val parentCategoryId: Long,
    ) : AddCategoryResult

    data class ParentCategoryMustBeBuiltIn(
        val parentCategoryId: Long,
    ) : AddCategoryResult

    data class ParentCategoryTypeMismatch(
        val categoryType: TransactionType,
        val parentCategoryType: TransactionType,
    ) : AddCategoryResult

    data class CategoryAlreadyExists(
        val existingCategoryId: Long,
        val existingCategoryName: String,
    ) : AddCategoryResult
}