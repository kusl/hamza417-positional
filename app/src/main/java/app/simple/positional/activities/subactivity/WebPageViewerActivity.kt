package app.simple.positional.activities.subactivity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import app.simple.positional.constants.HtmlPage
import app.simple.positional.decorations.views.CustomWebView
import app.simple.positional.extensions.activity.BaseActivity
import app.simple.positional.ui.compose.PositionalTheme

class WebPageViewerActivity : BaseActivity() {
    private var webView: CustomWebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = HtmlPage.forName(intent.getStringExtra(EXTRA_PAGE)) ?: return finish()
        val view = CustomWebView(this).also { webView = it }
        if (savedInstanceState?.let(view::restoreState) == null) view.loadUrl(page.url)
        setContentView(ComposeView(this).apply {
            setContent {
                PositionalTheme {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        AndroidView(factory = { view }, modifier = Modifier.fillMaxSize().systemBarsPadding())
                    }
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView?.saveState(outState)
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_PAGE = "page"

        fun open(context: Context, page: HtmlPage) {
            context.startActivity(Intent(context, WebPageViewerActivity::class.java).putExtra(EXTRA_PAGE, page.name))
        }
    }
}
