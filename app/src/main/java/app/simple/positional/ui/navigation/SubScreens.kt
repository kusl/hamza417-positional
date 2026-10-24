package app.simple.positional.ui.navigation

import android.location.Address
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import app.simple.positional.R
import app.simple.positional.constants.HtmlPage
import app.simple.positional.constants.LocationPins
import app.simple.positional.decorations.views.CustomWebView
import app.simple.positional.model.DirectionModel
import app.simple.positional.model.Locations
import app.simple.positional.model.Measure
import app.simple.positional.model.TrailEntry
import app.simple.positional.preferences.ClockPreferences
import app.simple.positional.preferences.DirectionPreferences
import app.simple.positional.preferences.GPSPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.preferences.MeasurePreferences
import app.simple.positional.preferences.TrailPreferences
import app.simple.positional.ui.compose.AccentColorScreen
import app.simple.positional.ui.compose.AccentOption
import app.simple.positional.ui.compose.AccentPalette
import app.simple.positional.ui.compose.ArtsScreen
import app.simple.positional.ui.compose.CoordinateInput
import app.simple.positional.ui.compose.CustomLocationActions
import app.simple.positional.ui.compose.CustomLocationScreen
import app.simple.positional.ui.compose.DirectionsActions
import app.simple.positional.ui.compose.DirectionsScreen
import app.simple.positional.ui.compose.MapSearchActions
import app.simple.positional.ui.compose.MapSearchScreen
import app.simple.positional.ui.compose.Record
import app.simple.positional.ui.compose.RecordActions
import app.simple.positional.ui.compose.RecordsScreen
import app.simple.positional.ui.compose.TimeZoneScreen
import app.simple.positional.ui.compose.rememberPickerMap
import app.simple.positional.util.LocaleHelper
import app.simple.positional.util.MapIntents
import app.simple.positional.util.TextViewUtils.capitalizeText
import app.simple.positional.util.TrailNames
import app.simple.positional.viewmodels.viewmodel.CustomLocationViewModel
import app.simple.positional.viewmodels.viewmodel.DirectionsViewModel
import app.simple.positional.viewmodels.viewmodel.MapSearchViewModel
import app.simple.positional.viewmodels.viewmodel.MeasureViewModel
import app.simple.positional.viewmodels.viewmodel.TrailsViewModel

@Composable
fun AccentRoute(activity: ComponentActivity, topInset: Dp) {
    val options = remember { AccentPalette.options(activity) }
    AccentColorScreen(
        options = options,
        selected = MainPreferences.getAccentColor(),
        materialYou = MainPreferences.isMaterialYouAccentColor(),
        topInset = topInset,
    ) { option: AccentOption ->
        if (!AccentPalette.isSelected(option, MainPreferences.getAccentColor(), MainPreferences.isMaterialYouAccentColor())) {
            MainPreferences.setMaterialYouAccentColor(option.dynamic)
            MainPreferences.setAccentColor(option.color)
            activity.recreate()
        }
    }
}

@Composable
fun ArtsRoute() {
    val radius = with(LocalDensity.current) { MainPreferences.getCornerRadius().toDp() }
    ArtsScreen(MainPreferences.getCurrentArt(), radius, LocationPins.getLocationPin(), MainPreferences::setCurrentArt)
}

@Composable
fun TimeZoneRoute(navigator: AppNavigator, topInset: Dp) {
    TimeZoneScreen(ClockPreferences.getTimeZone(), ClockPreferences.getDefaultClockTimeFormat(), LocaleHelper.getAppLocale(), topInset) {
        ClockPreferences.setTimeZone(it)
        navigator.back()
    }
}

@Composable
fun MeasuresRoute(navigator: AppNavigator, topInset: Dp) {
    val viewModel: MeasureViewModel = viewModel()
    val measures by viewModel.measures.collectAsState()
    val records = remember(measures) {
        val selected = MeasurePreferences.getLastSelectedMeasure()
        measures?.map { Record(it.dateCreated, it, it.name.orEmpty(), it.note.orEmpty(), it.dateCreated, it.dateCreated == selected) }
    }
    val actions = remember(viewModel) {
        object : RecordActions<Measure> {
            override fun open(record: Measure) {
                MeasurePreferences.setLastSelectedMeasure(record.dateCreated)
                navigator.back()
            }

            override fun add(name: String, note: String) {
                viewModel.addMeasure(name, note)
                navigator.back()
            }

            override fun delete(record: Measure) = viewModel.deleteMeasure(record)

            override fun isValidName(name: String) = name.isNotBlank()
        }
    }
    RecordsScreen(stringResource(R.string.saved_measures), records, topInset, actions)
}

@Composable
fun TrailsRoute(navigator: AppNavigator, topInset: Dp) {
    val viewModel: TrailsViewModel = viewModel()
    val trails by viewModel.trails.collectAsState()
    val records = remember(trails) {
        val current = TrailPreferences.getCurrentTrail()
        trails?.map { Record(it.keyTag, it, it.trailName.orEmpty(), it.trailNote.orEmpty(), it.dateCreated, it.trailName == current) }
    }
    val actions = remember(viewModel) {
        object : RecordActions<TrailEntry> {
            override fun open(record: TrailEntry) {
                TrailPreferences.setCurrentTrailName(record.trailName.orEmpty())
                navigator.back()
            }

            override fun add(name: String, note: String) = viewModel.addTrail(TrailEntry(System.currentTimeMillis(), name, note))

            override fun delete(record: TrailEntry) = viewModel.deleteTrail(record)

            override fun isValidName(name: String) = TrailNames.isValid(name, viewModel.trails.value.orEmpty().mapNotNull { it.trailName })
        }
    }
    RecordsScreen(stringResource(R.string.saved_trails), records, topInset, actions)
}

@Composable
fun DirectionsRoute(navigator: AppNavigator, topInset: Dp) {
    val context = LocalContext.current
    val viewModel: DirectionsViewModel = viewModel()
    val directions by viewModel.directions.collectAsState()
    val actions = remember(viewModel) {
        object : DirectionsActions {
            override fun create() {
                val target = DirectionPreferences.getTargetCoordinates()
                viewModel.open(DirectionPreferences.getTargetLabel(), target[0].toDouble(), target[1].toDouble())
            }

            override fun createFromTarget() {
                val target = GPSPreferences.getTargetMarkerCoordinates()
                viewModel.open(null, target[0].toDouble(), target[1].toDouble())
            }

            override fun set(direction: DirectionModel) = select(direction, useMapsTarget = false)

            override fun edit(direction: DirectionModel) =
                viewModel.open(direction.name, direction.latitude, direction.longitude, direction.dateAdded)

            override fun useAsTarget(direction: DirectionModel) {
                val last = MainPreferences.getLastCoordinates()
                GPSPreferences.setTargetMarker(true)
                GPSPreferences.setTargetMarkerLatitude(direction.latitude.toFloat())
                GPSPreferences.setTargetMarkerLongitude(direction.longitude.toFloat())
                GPSPreferences.setTargetMarkerStartLatitude(last[0])
                GPSPreferences.setTargetMarkerStartLongitude(last[1])
                select(direction, useMapsTarget = true)
            }

            override fun navigate(direction: DirectionModel) = MapIntents.navigate(context, direction.latitude, direction.longitude)

            override fun delete(direction: DirectionModel) = viewModel.delete(direction)

            override fun search() = navigator.search { viewModel.form.fill(it) }

            override fun save(latitude: Double, longitude: Double) = viewModel.save(latitude, longitude)

            override fun dismissEditor() = viewModel.close()

            private fun select(direction: DirectionModel, useMapsTarget: Boolean) {
                DirectionPreferences.setTargetLatitude(direction.latitude.toFloat())
                DirectionPreferences.setTargetLongitude(direction.longitude.toFloat())
                DirectionPreferences.setTargetLabel(direction.name.orEmpty())
                DirectionPreferences.setUseMapsTarget(useMapsTarget)
                navigator.back()
            }
        }
    }
    DirectionsScreen(directions, viewModel.editing != null, viewModel.form, topInset, actions)
}

@Composable
fun CustomLocationsRoute(navigator: AppNavigator, topInset: Dp) {
    val context = LocalContext.current
    val viewModel: CustomLocationViewModel = viewModel()
    LaunchedEffect(viewModel) {
        if (viewModel.form.latitude.isEmpty() && MainPreferences.isCustomCoordinate()) {
            val coordinates = MainPreferences.getCoordinates()
            viewModel.form.fill(MainPreferences.getAddress(), coordinates[0].toString(), coordinates[1].toString())
        }
    }
    val locations by viewModel.locations.collectAsState()
    val actions = remember(viewModel) {
        object : CustomLocationActions {
            override fun search() = navigator.search { viewModel.form.fill(it) }

            override fun save(latitude: Double, longitude: Double) = viewModel.save(newLocation(latitude, longitude))

            override fun setLocation(latitude: Double, longitude: Double, save: Boolean) {
                if (save) viewModel.save(newLocation(latitude, longitude))
                use(viewModel.form.address, latitude, longitude)
            }

            override fun select(location: Locations) =
                viewModel.form.fill(location.address, location.latitude.toString(), location.longitude.toString())

            override fun useLocation(location: Locations) = use(location.address, location.latitude, location.longitude)

            override fun delete(location: Locations) = viewModel.delete(location)

            override fun deleteAll() = viewModel.deleteAll()

            override fun help() = navigator.navigate(Routes.web(HtmlPage.CUSTOM_COORDINATES_HELP))

            override fun failed() = Toast.makeText(context, R.string.failed, Toast.LENGTH_SHORT).show()

            private fun newLocation(latitude: Double, longitude: Double) = Locations().apply {
                address = viewModel.form.address.trim().ifEmpty { "----" }.capitalizeText()
                this.latitude = latitude
                this.longitude = longitude
                date = System.currentTimeMillis()
            }

            private fun use(address: String, latitude: Double, longitude: Double) {
                MainPreferences.setCustomCoordinates(true)
                MainPreferences.setLatitude(latitude.toFloat())
                MainPreferences.setLongitude(longitude.toFloat())
                MainPreferences.setAddress(address)
                navigator.back()
            }
        }
    }
    CustomLocationScreen(viewModel.form, locations, topInset, actions)
}

@Composable
fun MapSearchRoute(navigator: AppNavigator) {
    val context = LocalContext.current
    val viewModel: MapSearchViewModel = viewModel()
    val suggestions by viewModel.suggestions.collectAsState()
    val map = rememberPickerMap()
    LaunchedEffect(map) {
        CoordinateInput.parse(viewModel.form.latitude, viewModel.form.longitude)?.let { (lat, lon) -> map.moveTo(lat, lon, animate = false) }
        map.onIdle = viewModel::onCameraIdle
    }
    val actions = remember(viewModel) {
        object : MapSearchActions {
            override fun addressChanged(text: String) = viewModel.onAddressChange(text)

            override fun pick(address: Address) {
                viewModel.pick(address)
                map.moveTo(address.latitude, address.longitude, animate = true)
            }

            override fun cancel() = navigator.back()

            override fun confirm() {
                val form = viewModel.form
                val coordinates = CoordinateInput.parse(form.latitude, form.longitude)
                if (coordinates == null) {
                    Toast.makeText(context, R.string.failed, Toast.LENGTH_SHORT).show()
                    return
                }
                navigator.deliver(SearchResult(form.address.trim(), coordinates.first, coordinates.second))
                navigator.back()
            }
        }
    }
    MapSearchScreen(viewModel.form, suggestions, map.view, actions)
}

@Composable
fun WebRoute(pageName: String?, navigator: AppNavigator) {
    val page = remember(pageName) { HtmlPage.forName(pageName) }
    if (page == null) {
        LaunchedEffect(Unit) { navigator.back() }
        return
    }
    val context = LocalContext.current
    val webView = remember(page) { CustomWebView(context).apply { loadUrl(page.url) } }
    DisposableEffect(webView) { onDispose { webView.destroy() } }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize().systemBarsPadding())
    }
}

private fun app.simple.positional.ui.compose.CustomLocationForm.fill(result: SearchResult) = fill(
    address = result.address.ifEmpty { address },
    latitude = result.latitude.toString(),
    longitude = result.longitude.toString(),
)

