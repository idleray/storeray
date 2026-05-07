package com.rayject.storeray.validator

import com.rayject.storeray.config.IapLocalizationConfig

object LocalizationValidator {
    private const val NAME_MAX_LENGTH = 30
    private const val DESCRIPTION_MAX_LENGTH = 55

    /**
     * 验证单个产品的本地化数据
     * @return 错误列表，为空表示验证通过
     */
    fun validate(
        productId: String,
        localizations: Map<String, IapLocalizationConfig>
    ): List<String> {
        val errors = mutableListOf<String>()

        // 检查每个语言的字段
        for ((locale, data) in localizations) {
            errors.addAll(validateLocale(locale, data))
        }

        return errors
    }

    private fun validateLocale(locale: String, data: IapLocalizationConfig): List<String> {
        val errors = mutableListOf<String>()

        // 检查 name 字段
        if (data.name.isBlank()) {
            errors.add("$locale: name 不能为空")
        } else if (data.name.length > NAME_MAX_LENGTH) {
            errors.add("$locale: name 长度超过 $NAME_MAX_LENGTH 字符 (当前: ${data.name.length})")
        }

        // 检查 description 字段
        if (data.description.isBlank()) {
            errors.add("$locale: description 不能为空")
        } else if (data.description.length > DESCRIPTION_MAX_LENGTH) {
            errors.add("$locale: description 长度超过 $DESCRIPTION_MAX_LENGTH 字符 (当前: ${data.description.length})")
        }

        return errors
    }
}
