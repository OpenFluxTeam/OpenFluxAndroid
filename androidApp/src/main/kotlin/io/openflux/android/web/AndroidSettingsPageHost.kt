package io.openflux.android.web

import io.openflux.desktop.service.SettingsPageHost
import io.openflux.app.platform.BrowserPage

/** Shows a script's settings wizard in a WebView, answered on its own channel (never the connection's setup requests). */
object AndroidSettingsPageHost : SettingsPageHost {
    override suspend fun open(html: String, onStep: (String) -> Unit, onSubmit: (String) -> Unit): BrowserPage =
        WebPage(html = html, onSubmit = onSubmit)

    // A setup wizard run with the real host: its own server page, which may ask for a sign-in window.
    override suspend fun openOwn(url: String, onStep: (String) -> Unit, onSubmit: (String) -> Unit): BrowserPage =
        WebStack.own(url, onSubmit)

    override fun close(page: BrowserPage) {
        when (page) {
            is WebStack -> page.close()
            is WebPage -> page.close()
        }
    }
}
