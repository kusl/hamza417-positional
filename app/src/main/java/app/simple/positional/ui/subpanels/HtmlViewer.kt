package app.simple.positional.ui.subpanels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import app.simple.positional.R
import app.simple.positional.decorations.fastscroll.FastScrollWebView
import app.simple.positional.decorations.fastscroll.FastScrollerBuilder
import app.simple.positional.extensions.fragment.ScopedFragment

class HtmlViewer : ScopedFragment() {

    private lateinit var webView: FastScrollWebView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_webpage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = view.findViewById(R.id.web_view)
        FastScrollerBuilder(webView).useMd2Style().build()

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
            return
        }

        pageFor(arguments?.getString("source"))?.let {
            webView.loadUrl("file:///android_asset/html/$it.html")
        }
    }

    private fun pageFor(source: String?): String? = when (source) {
        getString(R.string.privacy_policy) -> "privacy_policy"
        getString(R.string.disclaimer) -> "disclaimer"
        getString(R.string.terms_of_use) -> "terms_and_conditions"
        getString(R.string.permissions) -> "required_permissions"
        getString(R.string.credits) -> "credits"
        getString(R.string.internet_uses) -> "internet_uses"
        getString(R.string.physical_properties) -> "physical_properties"
        "Custom Coordinates Help" -> "custom_coordinates_help"
        "Media Keys" -> "media_keys"
        "trail_help" -> "trail_help"
        "null" -> "null"
        else -> null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    companion object {
        fun newInstance(string: String): HtmlViewer {
            val fragment = HtmlViewer()
            fragment.arguments = Bundle().apply { putString("source", string) }
            return fragment
        }
    }
}