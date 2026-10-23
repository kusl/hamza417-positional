package app.simple.positional.ui.navigation

import app.simple.positional.constants.HtmlPage

object Routes {
    const val PANELS = "panels"
    const val ACCENT = "accent"
    const val ARTS = "arts"
    const val TIMEZONE = "timezone"
    const val MEASURES = "measures"
    const val TRAILS = "trails"
    const val DIRECTIONS = "directions"
    const val CUSTOM_LOCATIONS = "custom_locations"
    const val MAP_SEARCH = "map_search"
    const val PAGE_ARGUMENT = "page"
    const val WEB = "web/{$PAGE_ARGUMENT}"

    fun web(page: HtmlPage) = "web/${page.name}"
}

data class SearchResult(val address: String, val latitude: Double, val longitude: Double)

interface AppNavigator {
    fun navigate(route: String)
    fun back()
    fun search(onResult: (SearchResult) -> Unit)
    fun deliver(result: SearchResult)
}
