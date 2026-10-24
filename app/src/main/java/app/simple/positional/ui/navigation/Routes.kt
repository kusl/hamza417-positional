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
    const val SEARCH_RESULT = "search_result"

    fun web(page: HtmlPage) = "web/${page.name}"
}

data class SearchResult(val address: String, val latitude: Double, val longitude: Double) {
    fun encode(): Array<String> = arrayOf(address, latitude.toString(), longitude.toString())

    companion object {
        fun decode(values: Array<String>?): SearchResult? {
            if (values == null || values.size != 3) return null
            val latitude = values[1].toDoubleOrNull() ?: return null
            val longitude = values[2].toDoubleOrNull() ?: return null
            return SearchResult(values[0], latitude, longitude)
        }
    }
}

interface AppNavigator {
    fun navigate(route: String)
    fun back()
    fun deliver(result: SearchResult)
}
