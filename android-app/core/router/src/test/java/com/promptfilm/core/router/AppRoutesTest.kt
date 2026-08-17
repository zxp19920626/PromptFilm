package com.promptfilm.core.router

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRoutesTest {
    @Test
    fun 所有路由应具备合法且唯一的分组路径() {
        val routes = listOf(AppRoutes.LOGIN, AppRoutes.PAYMENT)

        assertEquals(routes.size, routes.distinct().size)
        assertTrue(routes.all { it.matches(Regex("/[a-z][a-z0-9_-]*/[a-z][a-z0-9_-]*")) })
    }
}

