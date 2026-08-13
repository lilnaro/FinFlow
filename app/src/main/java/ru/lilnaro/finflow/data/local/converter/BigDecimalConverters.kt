package ru.lilnaro.finflow.data.local.converter

import androidx.room.TypeConverter
import java.math.BigDecimal

class BigDecimalConverters {

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.toPlainString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it) }
    }
}