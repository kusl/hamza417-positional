package app.simple.positional.viewmodels.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.simple.positional.preferences.LevelPreferences
import app.simple.positional.singleton.SharedPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LevelViewModel : ViewModel() {
    val square: StateFlow<Boolean> = SharedPreferences.changes(LevelPreferences.isSquareStyle)
        .map { LevelPreferences.isSquareStyle() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), LevelPreferences.isSquareStyle())

    fun toggleStyle() = LevelPreferences.setSquareStyle(!LevelPreferences.isSquareStyle())

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
