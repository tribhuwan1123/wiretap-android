package com.wiretap.core

data class RedactionRule(val pattern: Regex, val replacement: String = "[redacted]")

data class WireTapRedactionConfig(
    val sensitiveHeaders: Set<String> = DEFAULT_SENSITIVE_HEADERS,
    val sensitiveBodyKeys: Set<String> = DEFAULT_SENSITIVE_BODY_KEYS,
    val customRules: List<RedactionRule> = emptyList()
) {
    companion object {
        val DEFAULT_SENSITIVE_HEADERS = setOf(
            "authorization", "cookie", "set-cookie",
            "x-api-key", "x-auth-token", "x-access-token",
            "proxy-authorization", "www-authenticate"
        )
        val DEFAULT_SENSITIVE_BODY_KEYS = setOf(
            "password", "passwd", "secret", "token",
            "access_token", "refresh_token", "api_key",
            "apikey", "private_key", "client_secret"
        )
    }
}

object Redactor {
    fun redactHeaders(
        headers: Map<String, String>,
        config: WireTapRedactionConfig
    ): Map<String, String> = headers.mapValues { (k, v) ->
        if (config.sensitiveHeaders.any { k.lowercase() == it }) "[redacted]" else v
    }

    fun redactBody(body: String?, config: WireTapRedactionConfig): String? {
        var result: String = body ?: return null
        config.sensitiveBodyKeys.forEach { key ->
            result = result.replace(
                Regex("(\"${Regex.escape(key)}\"\\s*:\\s*)\"[^\"]*\"", RegexOption.IGNORE_CASE),
                "$1\"[redacted]\""
            )
        }
        config.customRules.forEach { rule ->
            result = result.replace(rule.pattern, rule.replacement)
        }
        return result
    }
}
