package app.simple.positional.ui.compose

import app.simple.positional.R
import app.simple.positional.preferences.ClockPreferences
import app.simple.positional.preferences.CompassPreferences
import app.simple.positional.preferences.DirectionPreferences
import app.simple.positional.preferences.GPSPreferences
import app.simple.positional.preferences.MeasurePreferences
import app.simple.positional.preferences.TrailPreferences
import app.simple.positional.ui.compose.MenuItem.Action
import app.simple.positional.ui.compose.MenuItem.Toggle

object PanelMenus {
    fun gps(onPin: () -> Unit, onHelp: () -> Unit): List<MenuItem> = with(GPSPreferences) {
        val satellite = isSatelliteOn()
        listOf(
            Toggle(R.string.labels, isLabelOn()) { setLabelMode(it) },
            Toggle(R.string.satellite_mode, satellite) { setSatelliteMode(it) },
            Toggle(R.string.buildings, getShowBuildingsOnMap(), !satellite) { setShowBuildingsOnMap(it) },
            Toggle(R.string.high_contrast_mode, getHighContrastMap(), !satellite) { setHighContrastMap(it) },
            Toggle(R.string.traffic, isTrafficShown()) { setTrafficMode(it) },
            Toggle(R.string.use_media_keys, isUsingVolumeKeys()) { setUseVolumeKeys(it) },
            Action(R.string.customize_pin, onPin),
            Action(R.string.help, onHelp),
        )
    }

    fun trail(onHelp: () -> Unit): List<MenuItem> = with(TrailPreferences) {
        val satellite = isSatelliteOn()
        listOf(
            Toggle(R.string.labels, isLabelOn()) { setLabelMode(it) },
            Toggle(R.string.satellite_mode, satellite) { setSatelliteMode(it) },
            Toggle(R.string.buildings, getShowBuildingsOnMap(), !satellite) { setShowBuildingsOnMap(it) },
            Toggle(R.string.high_contrast_mode, getHighContrastMap(), !satellite) { setHighContrastMap(it) },
            Toggle(R.string.auto_center_map, getMapAutoCenter()) { setMapAutoCenter(it) },
            Toggle(R.string.geodesic_trail, isTrailGeodesic()) { setGeodesic(it) },
            Action(R.string.help, onHelp),
        )
    }

    fun measure(): List<MenuItem> = with(MeasurePreferences) {
        val satellite = isSatelliteOn()
        listOf(
            Toggle(R.string.labels, isLabelOn()) { setLabelMode(it) },
            Toggle(R.string.satellite_mode, satellite) { setSatelliteMode(it) },
            Toggle(R.string.buildings, getShowBuildingsOnMap(), !satellite) { setShowBuildingsOnMap(it) },
            Toggle(R.string.high_contrast_mode, getHighContrastMap(), !satellite) { setHighContrastMap(it) },
        )
    }

    fun compass(onBloomSkins: () -> Unit, onPhysicalProperties: () -> Unit): List<MenuItem> = with(CompassPreferences) {
        listOf(
            Toggle(R.string.bloom, isFlowerBloomOn()) { setFlowerBloom(it) },
            Action(R.string.bloom_skins, onBloomSkins),
            Toggle(R.string.gimbal_lock, isUsingGimbalLock()) { setUseGimbalLock(it) },
            Toggle(R.string.show_code, getDirectionCode()) { setDirectionCode(it) },
            Action(R.string.physical_properties, onPhysicalProperties),
        )
    }

    fun clock(onNeedles: () -> Unit, onMotionType: () -> Unit): List<MenuItem> = with(ClockPreferences) {
        listOf(
            Action(R.string.needles, onNeedles),
            Action(R.string.motion_type, onMotionType),
            Toggle(R.string.twenty_four_hours_clock, isClockFace24Hour()) { setClockFaceType(it) },
            Toggle(R.string.time_format, getDefaultClockTimeFormat()) { setDefaultClockTime(it) },
            Toggle(R.string.seconds_precision, isUsingSecondsPrecision()) { setUseSecondsPrecision(it) },
        )
    }

    fun direction(onTargets: () -> Unit): List<MenuItem> = with(DirectionPreferences) {
        listOf(
            Action(R.string.targets, onTargets),
            Toggle(R.string.gimbal_lock, isGimbalLock()) { setGimbalLock(it) },
            Toggle(R.string.use_maps_target, isUsingMapsTarget(), GPSPreferences.isTargetMarkerSet()) { setUseMapsTarget(it) },
        )
    }
}
