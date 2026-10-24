package app.simple.positional.viewmodels.viewmodel

import androidx.lifecycle.ViewModel
import app.simple.positional.preferences.LevelPreferences
import kotlinx.coroutines.flow.StateFlow

class LevelViewModel : ViewModel() {
    val square: StateFlow<Boolean> = preference(LevelPreferences.isSquareStyle) { LevelPreferences.isSquareStyle() }

    fun toggleStyle() = LevelPreferences.setSquareStyle(!LevelPreferences.isSquareStyle())
}
