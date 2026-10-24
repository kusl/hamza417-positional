package app.simple.positional.ui.panels

import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simple.positional.R
import app.simple.positional.constants.ClockSkinsConstants.clockNeedleSkins
import app.simple.positional.constants.LocationPins
import app.simple.positional.extensions.panel.PanelController
import app.simple.positional.math.ClockMotion
import app.simple.positional.preferences.ClockPreferences
import app.simple.positional.preferences.GPSPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.ui.compose.Choice
import app.simple.positional.ui.compose.ChoiceDialog
import app.simple.positional.ui.compose.ClockNeedleDialog
import app.simple.positional.ui.compose.ClockScreen
import app.simple.positional.ui.compose.ClockState
import app.simple.positional.ui.compose.LocationSummaryDialog
import app.simple.positional.ui.compose.PanelDialog
import app.simple.positional.ui.compose.PanelMenus
import app.simple.positional.ui.navigation.Routes
import app.simple.positional.viewmodels.viewmodel.ClockViewModel

class Time(activity: ComponentActivity) : PanelController(activity) {
    private val state = ClockState()
    private val viewModel = panelModel(ClockViewModel::class, ::ClockViewModel)

    init {
        applyPreferences()
    }

    @Composable
    override fun Content(topInset: Dp) {
        val zone by viewModel.zone.collectAsStateWithLifecycle()
        val time by viewModel.time.collectAsStateWithLifecycle()
        val sky by viewModel.sky.collectAsStateWithLifecycle()
        ClockScreen(
            state,
            zone,
            time,
            sky,
            topInset,
            onTimezone = { navigate(Routes.TIMEZONE) },
            onMenu = ::showMenu,
            onLocation = { dialogs.show(ClockDialog.Location) },
        )
    }

    private fun showMenu() = menuSheet.show {
        PanelMenus.clock(onNeedles = { dialogs.show(ClockDialog.Needles) }, onMotionType = { dialogs.show(ClockDialog.Motion) })
    }

    @Composable
    override fun DialogContent(dialog: PanelDialog) {
        when (dialog) {
            ClockDialog.Location -> {
                val position by viewModel.position.collectAsStateWithLifecycle()
                LocationSummaryDialog(position.latitude, position.longitude, state.customLocation, dialogs::dismiss)
            }
            ClockDialog.Needles -> ClockNeedleDialog(ClockPreferences.getClockNeedleTheme(), dialogs::dismiss) { ClockPreferences.setClockNeedleTheme(it) }
            ClockDialog.Motion -> ChoiceDialog(
                stringResource(R.string.motion_type),
                motionTypes.map { (value, label) -> Choice(value, stringResource(label)) },
                ClockPreferences.getMovementType(),
                onDismiss = dialogs::dismiss,
            ) {
                ClockPreferences.setMovementType(it)
                dialogs.dismiss()
            }
            else -> super.DialogContent(dialog)
        }
    }

    private enum class ClockDialog : PanelDialog { Location, Needles, Motion }

    private fun applyPreferences() {
        state.motion = ClockMotion.of(ClockPreferences.getMovementType())
        state.face24 = ClockPreferences.isClockFace24Hour()
        state.needles = clockNeedleSkins.getOrElse(ClockPreferences.getClockNeedleTheme()) { clockNeedleSkins[0] }.toList()
        state.pin = LocationPins.locationsPins.getOrElse(GPSPreferences.getPinSkin()) { R.drawable.ic_pin_01 }
        state.customLocation = MainPreferences.isCustomCoordinate()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key in clockKeys) applyPreferences()
    }

    companion object {
        private val motionTypes = listOf(
            "smooth" to R.string.motion_type_smooth,
            "tick" to R.string.motion_type_tick,
            "oscillate" to R.string.tick_oscillate,
            "tick_smooth" to R.string.tick_smooth,
            "mechanical" to R.string.mechanical,
        )
        private val clockKeys = setOf(
            ClockPreferences.clockNeedleMovementType,
            ClockPreferences.clockNeedle,
            ClockPreferences.is24HourFace,
            MainPreferences.isCustomCoordinate,
            GPSPreferences.pinSkin,
        )
    }
}
