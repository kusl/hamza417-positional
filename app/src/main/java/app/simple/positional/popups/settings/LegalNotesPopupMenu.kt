package app.simple.positional.popups.settings

import android.view.View
import android.view.ViewGroup
import app.simple.positional.R
import app.simple.positional.decorations.popup.BasePopupWindow
import app.simple.positional.decorations.popup.PopupMenuCallback
import app.simple.positional.decorations.ripple.DynamicRippleTextView

class LegalNotesPopupMenu(contentView: View,
        viewGroup: ViewGroup,
        xOff: Float,
        yOff: Float) : BasePopupWindow() {
    lateinit var popupMenuCallback: PopupMenuCallback

    init {
        init(contentView, viewGroup, xOff, yOff)

        contentView.findViewById<DynamicRippleTextView>(R.id.menu_disclaimer).onClick()
        contentView.findViewById<DynamicRippleTextView>(R.id.menu_privacy_policy).onClick()
        contentView.findViewById<DynamicRippleTextView>(R.id.menu_terms_of_use).onClick()
        contentView.findViewById<DynamicRippleTextView>(R.id.menu_permissions).onClick()
        contentView.findViewById<DynamicRippleTextView>(R.id.menu_credits).onClick()
        contentView.findViewById<DynamicRippleTextView>(R.id.menu_internet_uses).onClick()
    }

    private fun DynamicRippleTextView.onClick() {
        this.setOnClickListener {
            popupMenuCallback.onMenuItemClicked(this.text.toString())
            dismiss()
        }
    }
}
