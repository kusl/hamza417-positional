package app.simple.positional.ui.navigation

import app.simple.positional.constants.HtmlPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesTest {
    @Test
    fun webRouteMatchesTemplate() {
        HtmlPage.entries.forEach { page ->
            val route = Routes.web(page)
            assertEquals(Routes.WEB.replace("{${Routes.PAGE_ARGUMENT}}", page.name), route)
            assertEquals(page, HtmlPage.forName(route.removePrefix("web/")))
        }
    }

    @Test
    fun routesAreUnique() {
        val routes = listOf(
            Routes.PANELS, Routes.ACCENT, Routes.ARTS, Routes.TIMEZONE, Routes.MEASURES,
            Routes.TRAILS, Routes.DIRECTIONS, Routes.CUSTOM_LOCATIONS, Routes.MAP_SEARCH, Routes.WEB,
        )
        assertEquals(routes.size, routes.toSet().size)
        assertTrue(routes.none { it.contains(' ') })
    }
}
