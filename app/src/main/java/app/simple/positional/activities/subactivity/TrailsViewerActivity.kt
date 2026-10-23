package app.simple.positional.activities.subactivity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import app.simple.positional.R
import app.simple.positional.extensions.activity.BaseActivity
import app.simple.positional.model.TrailEntry
import app.simple.positional.preferences.TrailPreferences
import app.simple.positional.ui.compose.Record
import app.simple.positional.ui.compose.RecordActions
import app.simple.positional.ui.compose.RecordsScreen
import app.simple.positional.ui.compose.setPositionalContent
import app.simple.positional.util.TrailNames
import app.simple.positional.viewmodels.viewmodel.TrailsViewModel

class TrailsViewerActivity : BaseActivity(), RecordActions<TrailEntry> {
    private val viewModel: TrailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPositionalContent { topInset ->
            val trails by viewModel.trails.collectAsState()
            val records = remember(trails) {
                val current = TrailPreferences.getCurrentTrail()
                trails?.map { Record(it.keyTag, it, it.trailName.orEmpty(), it.trailNote.orEmpty(), it.dateCreated, it.trailName == current) }
            }
            RecordsScreen(stringResource(R.string.saved_trails), records, topInset, this)
        }
    }

    override fun open(record: TrailEntry) {
        TrailPreferences.setCurrentTrailName(record.trailName)
        finish()
    }

    override fun add(name: String, note: String) =
        viewModel.addTrail(TrailEntry(System.currentTimeMillis(), name, note))

    override fun delete(record: TrailEntry) = viewModel.deleteTrail(record)

    override fun isValidName(name: String) =
        TrailNames.isValid(name, viewModel.trails.value.orEmpty().map { it.trailName })
}
