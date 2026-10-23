package app.simple.positional.dialogs.trail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import app.simple.positional.R
import app.simple.positional.decorations.ripple.DynamicRippleButton
import app.simple.positional.decorations.views.CustomDialogFragment
import app.simple.positional.model.TrailEntry
import app.simple.positional.preferences.TrailPreferences
import app.simple.positional.util.TrailNames
import app.simple.positional.util.ViewUtils.setVisibility
import app.simple.positional.viewmodels.viewmodel.TrailsViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class AddTrail : CustomDialogFragment() {
    private lateinit var nameInputEditText: EditText
    private lateinit var noteInputEditText: EditText
    private lateinit var save: DynamicRippleButton
    private lateinit var cancel: DynamicRippleButton

    private val trailsViewModel: TrailsViewModel by viewModels()
    private var existing: List<String> = emptyList()

    var onNewTrailAddedSuccessfully: (trailEntry: TrailEntry) -> Unit = {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_trail_name, container, false)

        nameInputEditText = view.findViewById(R.id.trail_name_edit_text)
        noteInputEditText = view.findViewById(R.id.note)
        save = view.findViewById(R.id.save)
        cancel = view.findViewById(R.id.cancel)

        nameInputEditText.setText(TrailPreferences.getLastTrailName())
        noteInputEditText.setText(TrailPreferences.getLastTrailNote())

        runCatching {
            nameInputEditText.requestFocus()
            dialog!!.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }.getOrElse {
            nameInputEditText.clearFocus()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateSave()

        viewLifecycleOwner.lifecycleScope.launch {
            trailsViewModel.trails.filterNotNull().collect { trails ->
                existing = trails.map { it.trailName }
                updateSave()
            }
        }

        nameInputEditText.doOnTextChanged { text, _, _, _ ->
            updateSave()
            TrailPreferences.setLastTrailName(text.toString())
        }

        noteInputEditText.doOnTextChanged { text, _, _, _ ->
            TrailPreferences.setLastTrailNote(text.toString())
        }

        save.setOnClickListener {
            val trail = TrailEntry(
                System.currentTimeMillis(),
                nameInputEditText.text.toString().trim(),
                noteInputEditText.text.toString().trim(),
            )

            TrailPreferences.setLastTrailNote("")
            TrailPreferences.setLastTrailName("")

            dismiss()

            onNewTrailAddedSuccessfully.invoke(trail)
        }

        cancel.setOnClickListener {
            dismiss()
        }
    }

    private fun updateSave() {
        save.setVisibility(TrailNames.isValid(nameInputEditText.text.toString(), existing))
    }

    companion object {
        fun newInstance(): AddTrail {
            val args = Bundle()
            val fragment = AddTrail()
            fragment.arguments = args
            return fragment
        }
    }
}
