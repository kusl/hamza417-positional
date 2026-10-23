package app.simple.positional.viewmodels.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.location.Geocoder
import android.location.Location
import android.text.Spanned
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.simple.positional.R
import app.simple.positional.extensions.viewmodel.WrappedViewModel
import app.simple.positional.math.MathExtensions.round
import app.simple.positional.math.UnitConverter.toFeet
import app.simple.positional.math.UnitConverter.toKilometers
import app.simple.positional.math.UnitConverter.toMiles
import app.simple.positional.preferences.GPSPreferences
import app.simple.positional.services.LocationBus
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.util.ArrayHelper.isLastValueSame
import app.simple.positional.util.ConditionUtils.invert
import app.simple.positional.util.ConditionUtils.isNull
import app.simple.positional.util.Direction
import app.simple.positional.util.HtmlHelper
import app.simple.positional.util.LocationExtension
import app.simple.positional.util.NetworkCheck
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

class LocationViewModel(application: Application) : WrappedViewModel(application) {
    private val listener = object : LocationBus.Listener {
        override fun onLocation(location: Location) = onNewLocation(location)

        override fun onProvider(provider: String?) {
            this@LocationViewModel.provider.postValue(provider?.uppercase(Locale.getDefault()))
        }
    }

    private val accuracyData = ArrayList<Float>()
    private val altitudeData = ArrayList<Float>()

    private val location = MutableLiveData<Location>()
    private val provider = MutableLiveData<String>()

    val latency = MutableLiveData<Pair<Number, Boolean>>()
    val targetData = MutableLiveData<Spanned>()

    val accuracyGraphData = MutableLiveData<ArrayList<Float>>()
    val altitudeGraphData = MutableLiveData<ArrayList<Float>>()

    private var lastLatencyInMilliseconds: Number = System.currentTimeMillis().toDouble()
    private var targetAddress: String? = null

    init {
        LocationBus.register(listener)
    }

    private fun onNewLocation(newLocation: Location) {
        location.postValue(newLocation)
        measureLatency()
        graphData(newLocation)
        if (GPSPreferences.isTargetMarkerSet().invert()) {
            GPSPreferences.setTargetMarkerStartLatitude(newLocation.latitude.toFloat())
            GPSPreferences.setTargetMarkerStartLongitude(newLocation.longitude.toFloat())
        }
        val target = GPSPreferences.getTargetMarkerCoordinates()
        val origin = if (MainPreferences.isCustomCoordinate()) {
            MainPreferences.getCoordinates().let { LatLng(it[0].toDouble(), it[1].toDouble()) }
        } else {
            LatLng(newLocation.latitude, newLocation.longitude)
        }
        targetData(LatLng(target[0].toDouble(), target[1].toDouble()), origin, newLocation.speed)
    }

    fun getLocation(): LiveData<Location> {
        return location
    }

    fun getProvider(): LiveData<String> {
        return provider
    }

    private fun measureLatency() {
        kotlin.runCatching {
            var currentLatency: Number

            with(System.currentTimeMillis()) {
                currentLatency = this.toDouble() - lastLatencyInMilliseconds.toDouble()
                lastLatencyInMilliseconds = this.toDouble()
            }

            val inSeconds: Boolean

            currentLatency = if (currentLatency.toDouble() > 999) {
                inSeconds = true
                round(currentLatency.toDouble() / 1000.0, 3)
            } else {
                inSeconds = false
                currentLatency.toInt()
            }

            latency.postValue(Pair(currentLatency, inSeconds))
        }
    }

    fun targetData(target: LatLng, current: LatLng, speed: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            with(StringBuilder()) {
                append(targetDisplacement(target, current))
                append(targetDisplacementFromOrigin(current))
                append(targetBearing(target, current))
                append(targetDirection(target, current))
                append(targetETA(target, current, speed))
                append(targetAddress(target))

                targetData.postValue(HtmlHelper.fromHtml(this.toString()))
            }
        }
    }

    private fun targetDisplacement(target: LatLng, current: LatLng): StringBuilder {
        return StringBuilder().also {
            it.append("<b>${getString(R.string.gps_displacement_from_dest)} </b>")

            if (GPSPreferences.isTargetMarkerSet()) {
                val p0 = LocationExtension.measureDisplacement(arrayOf(target, current))

                if (MainPreferences.isMetric()) {
                    if (p0 < 1000) {
                        it.append(p0.round(2))
                        it.append(" ")
                        it.append(getContext().getString(R.string.meter))
                    } else {
                        it.append(p0.toKilometers().round(2))
                        it.append(" ")
                        it.append(getContext().getString(R.string.kilometer))
                    }
                } else {
                    if (p0 < 1000) {
                        it.append(p0.toDouble().toFeet().toFloat().round(2))
                        it.append(" ")
                        it.append(getContext().getString(R.string.feet))
                    } else {
                        it.append(p0.toMiles().round(2))
                        it.append(" ")
                        it.append(getContext().getString(R.string.miles))
                    }
                }
            } else {
                it.append(getString(R.string.not_available))
            }

            it.append("<br>")
        }
    }

    private fun targetDisplacementFromOrigin(current: LatLng): StringBuilder {
        return StringBuilder().also {
            val target = LatLng(GPSPreferences.getTargetMarkerStartCoordinates()[0].toDouble(),
                GPSPreferences.getTargetMarkerStartCoordinates()[1].toDouble())

            it.append("<b>${getString(R.string.gps_displacement_from_origin)} </b>")

            if (GPSPreferences.isTargetMarkerSet()) {
                val p0 = LocationExtension.measureDisplacement(arrayOf(target, current))

                if (MainPreferences.isMetric()) {
                    if (p0 < 1000) {
                        it.append(p0.round(2))
                        it.append(" ")
                        it.append(getContext().getString(R.string.meter))
                    } else {
                        it.append(p0.toKilometers().round(2))
                        it.append(" ")
                        it.append(getContext().getString(R.string.kilometer))
                    }
                } else {
                    if (p0 < 1000) {
                        it.append(p0.toDouble().toFeet().toFloat().round(2))
                        it.append(" ")
                        it.append(getContext().getString(R.string.feet))
                    } else {
                        it.append(p0.toMiles().round(2))
                        it.append(" ")
                        it.append(getContext().getString(R.string.miles))
                    }
                }
            } else {
                it.append(getString(R.string.not_available))
            }

            it.append("<br>")
        }
    }

    private fun targetDirection(target: LatLng, current: LatLng): StringBuilder {
        return StringBuilder().also {
            it.append("<b>${getString(R.string.gps_direction)} </b>")

            if (GPSPreferences.isTargetMarkerSet()) {
                val p0 = LocationExtension.calculateBearingAngle(
                    current.latitude,
                    current.longitude,
                    target.latitude,
                    target.longitude
                )

                it.append(Direction.getDirectionNameFromAzimuth(getContext(), (p0 % 360 + 360) % 360))
            } else {
                it.append(getString(R.string.not_available))
            }

            it.append("<br>")
        }
    }

    private fun targetBearing(target: LatLng, current: LatLng): StringBuilder {
        return StringBuilder().also {
            it.append("<b>${getString(R.string.gps_bearing)} </b>")

            if (GPSPreferences.isTargetMarkerSet()) {
                val p0 = LocationExtension.calculateBearingAngle(
                    current.latitude,
                    current.longitude,
                    target.latitude,
                    target.longitude
                )

                it.append(round((p0 % 360 + 360) % 360, 2))
                it.append("°")
            } else {
                it.append(getString(R.string.not_available))
            }

            it.append("<br>")
        }
    }

    private fun targetETA(target: LatLng, current: LatLng, speed: Float): StringBuilder {
        return StringBuilder().also {
            it.append("<b>${getString(R.string.eta)} </b>")

            if (GPSPreferences.isTargetMarkerSet() && speed != 0.0F) {
                val p0 = LocationExtension.measureDisplacement(arrayOf(target, current))

                val time = (p0 / speed).toLong()
                println("time: $time")
                val txt = when {
                    TimeUnit.SECONDS.toSeconds(time) < 60 -> {
                        getString(R.string.eta_seconds, TimeUnit.SECONDS.toSeconds(time).toString())
                    }

                    TimeUnit.SECONDS.toMinutes(time) < 60 -> {
                        getString(R.string.eta_minutes, TimeUnit.SECONDS.toMinutes(time).toString())
                    }

                    TimeUnit.SECONDS.toHours(time) < 24 -> {
                        getString(R.string.eta_hours,
                            TimeUnit.SECONDS.toHours(time).toString(),
                            (TimeUnit.SECONDS.toMinutes(time) % 60).toString())
                    }

                    else -> {
                        getString(R.string.eta_days,
                            TimeUnit.SECONDS.toDays(time).toString(),
                            (TimeUnit.SECONDS.toHours(time) % 24).toString(),
                            (TimeUnit.SECONDS.toMinutes(time) % 60).toString())
                    }
                }

                it.append(txt)
            } else {
                it.append(getString(R.string.not_available))
            }

            it.append("<br>")
        }
    }

    private fun targetAddress(target: LatLng): StringBuilder {
        return StringBuilder().also {
            it.append("<b>${getString(R.string.gps_address)}: </b>")

            if (targetAddress.isNull()) {
                if (GPSPreferences.isTargetMarkerSet()) {
                    targetAddress = try {
                        if (!NetworkCheck.isNetworkAvailable(getApplication())) {
                            null
                        } else {
                            val geocoder = Geocoder(getApplication(), Locale.getDefault())

                            @Suppress("DEPRECATION")
                            with(geocoder.getFromLocation(target.latitude, target.longitude, 1)) {
                                if (!this.isNullOrEmpty()) {
                                    this[0].getAddressLine(0)
                                } else {
                                    getString(R.string.not_available)
                                }
                            }
                        }
                    } catch (e: IOException) {
                        null
                    } catch (e: NullPointerException) {
                        null
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }

                if (targetAddress.isNull()) {
                    it.append(getString(R.string.not_available))
                } else {
                    it.append(targetAddress)
                }
            } else {
                it.append(targetAddress)
            }
        }
    }

    private fun graphData(location: Location) {
        accuracyGraphData.postValue(ArrayList(manipulateDataForGraph(accuracyData, location.accuracy)))
        altitudeGraphData.postValue(ArrayList(manipulateDataForGraph(altitudeData, location.altitude.toFloat())))
    }

    private fun manipulateDataForGraph(arrayList: ArrayList<Float>, value: Float): ArrayList<Float> {
        if (arrayList.isLastValueSame(value))
            return arrayList

        if (arrayList.size >= 45)
            arrayList.removeAt(0)

        arrayList.add(value)
        return arrayList
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        super.onSharedPreferenceChanged(sharedPreferences, key)
        when (key) {
            GPSPreferences.isTargetMarkerMode,
            GPSPreferences.mapTargetMarkerLatitude,
            GPSPreferences.mapTargetMarkerLongitude -> {
                targetAddress = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        LocationBus.unregister(listener)
    }
}
