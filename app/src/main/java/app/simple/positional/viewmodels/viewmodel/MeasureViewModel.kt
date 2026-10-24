package app.simple.positional.viewmodels.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.simple.positional.database.instances.MeasureDatabase
import app.simple.positional.extensions.viewmodel.WrappedViewModel
import app.simple.positional.model.Measure
import app.simple.positional.model.MeasurePoint
import app.simple.positional.preferences.MeasurePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MeasureViewModel(application: Application) : WrappedViewModel(application) {
    private val dao = MeasureDatabase.getInstance(application).measureDao()

    val measures: StateFlow<List<Measure>?> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    private val measure: MutableLiveData<Measure> by lazy {
        MutableLiveData<Measure>().also {
            loadMeasure(MeasurePreferences.getLastSelectedMeasure())
        }
    }

    fun getMeasure(): LiveData<Measure> {
        return measure
    }

    private fun loadMeasure(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            measure.postValue(dao.getMeasureById(id))
        }
    }

    fun addMeasure(name: String, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val created = Measure(System.currentTimeMillis(), name, note)
            withContext(NonCancellable) {
                dao.insertMeasure(created)
                MeasurePreferences.setLastSelectedMeasure(created.dateCreated)
            }
            loadMeasure(created.dateCreated)
        }
    }

    fun addMeasurePoint(measurePoint: MeasurePoint, onAdded: (Measure) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            measure.value?.let {
                it.points.add(measurePoint)
                dao.updateMeasure(it)
                onAdded(it)
            }
        }
    }

    fun removeMeasurePoint(measurePoint: MeasurePoint?, onRemoved: (Measure) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = measure.value
            if (measurePoint != null && current != null) {
                current.points.remove(measurePoint)
                dao.updateMeasure(current)
                onRemoved(current)
            }
        }
    }

    fun deleteMeasure(measure: Measure) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                dao.deleteMeasure(measure)
                if (MeasurePreferences.getLastSelectedMeasure() == measure.dateCreated) MeasurePreferences.setLastSelectedMeasure(-1)
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        super.onSharedPreferenceChanged(sharedPreferences, key)
        when (key) {
            MeasurePreferences.LAST_SELECTED_MEASURE -> {
                loadMeasure(MeasurePreferences.getLastSelectedMeasure())
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
