package ru.lilnaro.finflow.domain.model

data class TransactionCategory(
    val id: Long = 0L,
    val name: String,
    val type: TransactionType,
    val isCustom: Boolean,
    val parentCategoryId: Long? = null,
) {

    init {
        require(name == name.trim()) {
            "Название категории не должно начинаться или заканчиваться пробелами"
        }

        require(name.length in MIN_NAME_LENGTH..MAX_NAME_LENGTH) {
            "Название категории должно содержать от $MIN_NAME_LENGTH до $MAX_NAME_LENGTH символов"
        }

        require(name.any { character -> character.isLetter() }) {
            "Название категории должно содержать хотя бы одну букву"
        }

        require(
            name.all { character ->
                character.isLetterOrDigit() ||
                        character in ALLOWED_NAME_CHARACTERS
            },
        ) {
            "Название категории содержит недопустимые символы"
        }

        require(DOUBLE_SPACE !in name) {
            "Название категории не должно содержать несколько пробелов подряд"
        }

        require(
            parentCategoryId == null ||
                    parentCategoryId > 0L,
        ) {
            "Идентификатор родительской категории должен быть положительным"
        }

        require(
            !isCustom ||
                    parentCategoryId != null,
        ) {
            "Пользовательская категория должна быть связана с базовой категорией"
        }

        require(
            isCustom ||
                    parentCategoryId == null,
        ) {
            "Встроенная категория не должна иметь родительскую категорию"
        }
    }

    companion object {

        const val MIN_NAME_LENGTH = 2

        const val MAX_NAME_LENGTH = 40

        val ALLOWED_NAME_CHARACTERS: Set<Char> = setOf(
            ' ',
            '-',
            '&',
            '/',
            '(',
            ')',
        )

        private const val DOUBLE_SPACE = "  "
    }
}