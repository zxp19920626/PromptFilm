package com.promptfilm.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppResultTest {
    @Test
    fun 失败结果应提供稳定错误码和英文文案() {
        val result = AppResult.Failure(
            code = "NETWORK_TIMEOUT",
            message = "The request timed out. Please try again.",
        )

        assertEquals("NETWORK_TIMEOUT", result.code)
        assertFalse(result.message.contains(Regex("[\\u4E00-\\u9FFF]")))
    }
}

