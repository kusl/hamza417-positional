package app.simple.positional.activities.subactivity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import app.simple.positional.R
import app.simple.positional.extensions.activity.BaseActivity
import app.simple.positional.model.Measure
import app.simple.positional.preferences.MeasurePreferences
import app.simple.positional.ui.compose.Record
import app.simple.positional.ui.compose.RecordActions
import app.simple.positional.ui.compose.RecordsScreen
import app.simple.positional.ui.compose.setPositionalContent
import app.simple.positional.viewmodels.viewmodel.MeasureViewModel

class MeasuresActivity : BaseActivity(), RecordActions<Measure> {
    private val viewModel: MeasureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPositionalContent { topInset ->
            val measures by viewModel.measures.collectAsState()
            val records = remember(measures) {
                val selected = MeasurePreferences.getLastSelectedMeasure()
                measures?.map { Record(it.dateCreated, it, it.name.orEmpty(), it.note.orEmpty(), it.dateCreated, it.dateCreated == selected) }
            }
            RecordsScreen(stringResource(R.string.saved_measures), records, topInset, this)
        }
    }

    override fun open(record: Measure) {
        MeasurePreferences.setLastSelectedMeasure(record.dateCreated)
        finish()
    }

    override fun add(name: String, note: String) {
        viewModel.addMeasure(name, note)
        finish()
    }

    override fun delete(record: Measure) = viewModel.deleteMeasure(record)

    override fun isValidName(name: String) = name.isNotBlank()
}
