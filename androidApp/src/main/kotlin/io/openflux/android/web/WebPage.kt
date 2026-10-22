package io.openflux.android.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import io.openflux.desktop.model.SetupPages
import io.openflux.app.platform.BrowserPage
import io.openflux.app.platform.BrowserViews
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * One page of the built-in browser: a WebView the UI shows with
 * [WebBrowserViews]. It is created the first time it is shown and kept until
 * [close], so hiding the dialog does not lose the page. [proxy] ("host:port")
 * sends it through the tunnel, for a check the exit node must pass from its
 * own address.
 *
 * A script transport's own page comes two ways: [html], instead of
 * [startUrl], is its inline page; with [own] the [startUrl] is on the script's
 * own loopback server (http://127.0.0.1:port, httpserver.listen() in the
 * core). Either submits itself through [onSubmit] (window.openfluxSubmit)
 * rather than through cookies. The submit channel belongs to the page's own
 * address: once the WebView is anywhere else (a link out of the page) what it
 * sends is dropped.
 *
 * With [onLogin] the same page may also ask for a sign-in window
 * (window.openfluxLogin): the service opens it as another [WebPage] and
 * answers through [deliver].
 */
class WebPage(
    private val startUrl: String = "",
    private val proxy: String = "",
    private val scripts: Boolean = false,
    /** null keeps WebView's own; Yandex pages get the core's (see [USER_AGENT]). */
    private val userAgent: String? = USER_AGENT,
    private val html: String? = null,
    private val onSubmit: ((String) -> Unit)? = null,
    private val own: Boolean = false,
    /** window.openfluxLogin(spec) from the page: the request's JSON (see [SetupPages.LoginRequest]); only own pages have it. */
    private val onLogin: ((String) -> Unit)? = null,
    /** localStorage keys of the site that are removed before its own scripts run, once: the page starts signed out ([SetupPages.clearStorageScript]). */
    private val clearStorage: List<String> = emptyList(),
) : BrowserPage {
    /** The address an own-server page's frame must keep for the bridge to stay with it; null otherwise. */
    private val ownOrigin: String? = if (own) SetupPages.loopbackOrigin(startUrl) else null

    init {
        require(!own || ownOrigin != null) { "Страница настройки скрипта открывается только с адреса 127.0.0.1 его собственного сервера" }
    }

    private val setupPage: Boolean get() = html != null || own

    /** The page's bridge snippet: with openfluxLogin only when someone listens for it. */
    private val bridgeJs: String = if (onLogin != null) SUBMIT_LOGIN_BRIDGE_JS else SUBMIT_BRIDGE_JS

    @Volatile var url: String = startUrl
        private set

    /** Every main-frame address since the page opened, oldest first (a fragment the page then clears stays here). */
    val history = java.util.concurrent.CopyOnWriteArrayList<String>()
    @Volatile var loading = true
        private set
    @Volatile var closed = false
        private set

    private var view: WebView? = null
    private var proxyOverridden = false
    private val results = ConcurrentHashMap<String, CompletableDeferred<String>>()
    private val ids = AtomicLong()

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    internal fun view(context: Context): WebView {
        view?.let { existing ->
            (existing.parent as? ViewGroup)?.removeView(existing)
            return existing
        }
        // Debug builds: chrome://inspect can look inside the sign-in pages.
        if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        val web = WebView(context)
        // AndroidView would give it WRAP_CONTENT: the WebView then sizes to
        // its content and pages see a viewport 0 px high (VK ID stayed blank,
        // Yandex's bottom sheets closed as soon as they opened).
        web.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // The UA the core fetches the document with: a check's pass may be bound to it.
            if (userAgent != null) userAgentString = userAgent
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(web, true)
        }
        if (scripts) web.addJavascriptInterface(Results(), BRIDGE)
        if (setupPage) web.addJavascriptInterface(Submit(), SUBMIT_BRIDGE)
        // An own-server page cannot be edited on the way: the bridge goes in before its scripts where the
        // WebView can do that, and again when it has loaded where it cannot (idempotent).
        val origin = ownOrigin
        if (origin != null && WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            runCatching { WebViewCompat.addDocumentStartJavaScript(web, bridgeJs, setOf(origin.trimEnd('/'))) }
        }
        // A sign-in window starts signed out: what an earlier window kept is removed before the site reads it.
        if (clearStorage.isNotEmpty()) {
            val site = SetupPages.httpsOrigin(startUrl)
            if (site != null && WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                runCatching { WebViewCompat.addDocumentStartJavaScript(web, SetupPages.clearStorageScript(clearStorage), setOf(site)) }
            } else {
                // No document-start script in this WebView: all it has stored goes, the one way left to start signed out.
                WebStorage.getInstance().deleteAllData()
            }
        }
        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                this@WebPage.url = url
                history.add(url)
                loading = true
                // Where a sign-in goes, for adb logcat -s OpenFluxWeb; no query (tokens live there).
                if (view.context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                    android.util.Log.d("OpenFluxWeb", url.substringBefore('?').substringBefore('#'))
                }
            }

            // Addresses a page changes without loading (a fragment, a single-page app's route) count too.
            override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                this@WebPage.url = url
                if (history.lastOrNull() != url) history.add(url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                this@WebPage.url = url
                loading = false
                if (setupPage && bridgeAllowedAt(url)) view.evaluateJavascript(bridgeJs, null)
            }
        }
        view = web
        if (html != null) {
            // Never through the tunnel's proxy: a script's own page is loopback-only,
            // like httpserver.listen() on the core side it usually talks to.
            web.loadDataWithBaseURL(null, SetupPages.inject(html, bridgeJs), "text/html", "utf-8", null)
        } else if (proxy.isEmpty()) {
            web.loadUrl(startUrl)
        } else if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            proxyOverridden = true
            val config = ProxyConfig.Builder().addProxyRule(proxy).build()
            ProxyController.getInstance().setProxyOverride(config, Runnable::run) { if (!closed) web.loadUrl(startUrl) }
        } else {
            web.loadData(
                "<p style='font:16px sans-serif;padding:16px'>WebView не умеет работать через прокси: обновите «Android System WebView» в Google Play.</p>",
                "text/html; charset=utf-8", "utf-8",
            )
        }
        return web
    }

    /** Runs [expression] (it may return a Promise) on the page; its value as a string. */
    suspend fun evaluate(expression: String, timeoutMs: Long = 90_000): String {
        check(scripts) { "This page does not run scripts" }
        val id = ids.incrementAndGet().toString()
        val result = CompletableDeferred<String>().also { results[id] = it }
        try {
            withContext(Dispatchers.Main) {
                val web = view ?: throw IllegalStateException("Страница ещё не открыта")
                web.evaluateJavascript(
                    """
                    (async () => {
                      let v;
                      try { v = await ($expression); } catch (e) { v = JSON.stringify({state: 'fail', error: String((e && e.message) || e)}); }
                      window.$BRIDGE.result('$id', typeof v === 'string' ? v : JSON.stringify(v));
                    })();
                    """.trimIndent(),
                    null,
                )
            }
            return withTimeout(timeoutMs) { result.await() }
        } finally {
            results.remove(id)
        }
    }

    /** Runs [script] on the page (the answer to its openfluxLogin, see [SetupPages.loginResultScript]); nothing if it is gone. */
    fun deliver(script: String) {
        val web = view ?: return
        web.post { if (!closed) web.evaluateJavascript(script, null) }
    }

    /** Sends the page to [url] once it is on screen. */
    fun load(url: String) {
        val web = view
        if (web != null) web.post { if (!closed) web.loadUrl(url) }
    }

    /** The Cookie header the page's cookie jar sends to [url]. */
    fun cookieHeader(url: String): String = CookieManager.getInstance().getCookie(url).orEmpty()

    fun close() {
        if (closed) return
        closed = true
        results.values.forEach { it.cancel() }
        val web = view
        view = null
        val overridden = proxyOverridden
        web?.post {
            (web.parent as? ViewGroup)?.removeView(web)
            web.destroy()
            // The override applies to every WebView in the process.
            if (overridden) ProxyController.getInstance().clearProxyOverride(Runnable::run) {}
        }
    }

    private inner class Results {
        @JavascriptInterface
        fun result(id: String, value: String) {
            results[id]?.complete(value)
        }
    }

    /** Whether [at] is the page's own address: an inline page has none (about:blank), a server page keeps its origin. */
    private fun bridgeAllowedAt(at: String): Boolean = when {
        html != null -> at.isBlank() || at.startsWith("about:") || at.startsWith("data:")
        ownOrigin != null -> at.startsWith(ownOrigin, ignoreCase = true)
        else -> false
    }

    private inner class Submit {
        @JavascriptInterface
        fun submit(json: String) {
            // @JavascriptInterface calls come from a WebView thread; url is volatile.
            if (!bridgeAllowedAt(this@WebPage.url)) return
            onSubmit?.invoke(json)
        }

        @JavascriptInterface
        fun login(json: String) {
            if (!bridgeAllowedAt(this@WebPage.url)) return
            onLogin?.invoke(json)
        }
    }

    companion object {
        const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:153.0) Gecko/20100101 Firefox/153.0"
        private const val BRIDGE = "OpenFluxBridge"
        private const val SUBMIT_BRIDGE = "OpenFluxSubmit"
        private val SUBMIT_BRIDGE_JS = SetupPages.bridgeScript("window.$SUBMIT_BRIDGE.submit(j)")
        private val SUBMIT_LOGIN_BRIDGE_JS = SetupPages.bridgeScript("window.$SUBMIT_BRIDGE.submit(j)", "window.$SUBMIT_BRIDGE.login(j)")

        /** A desktop Chrome: a messenger's web client (MAX) gives a phone's browser an "open the app" page instead of its sign-in. */
        const val SIGN_IN_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36"

        /**
         * Forgets what the built-in browser keeps in JavaScript storage for the site at [origin]
         * ("https://host"), so a sign-in window for it starts signed out and what it reads afterwards
         * is the account the person just signed in with, not an earlier one. Main thread only.
         */
        fun forgetSite(origin: String) {
            WebStorage.getInstance().deleteOrigin(origin)
            // Its cookies too (the ones this page may set; others are the site's own business): a site may sign in by them.
            val cookies = CookieManager.getInstance()
            cookies.getCookie(origin)?.split(';')?.map { it.substringBefore('=').trim() }?.filter { it.isNotEmpty() }?.forEach {
                cookies.setCookie(origin, "$it=; Max-Age=0; Path=/")
            }
            cookies.flush()
        }

        /** Every cookie of the built-in browser, the Yandex sign-in among them. */
        fun clearCookies() {
            CookieManager.getInstance().apply {
                removeAllCookies(null)
                flush()
            }
        }
    }
}

/** Shows [WebPage]s and [WebStack]s inside the Compose UI. */
object WebBrowserViews : BrowserViews {
    @Composable
    override fun Page(page: BrowserPage, modifier: Modifier) {
        when (page) {
            is WebPage -> key(page) {
                AndroidView(factory = { context -> page.view(context) }, modifier = modifier)
            }
            // A script's own page; while it asks for a sign-in window that window is what is shown, with its way back.
            is WebStack -> Column(modifier) {
                page.signInTitle?.let { SignInBar(it, page.signInHint, page.signInCanTake, page::take, page::back, page::reload) }
                val web = page.top
                key(web) {
                    AndroidView(factory = { context -> web.view(context) }, modifier = Modifier.weight(1f).fillMaxWidth())
                }
            }
            else -> Unit
        }
    }
}
