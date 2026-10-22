package io.openflux.android.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.openflux.app.components.TextAction
import io.openflux.app.platform.BrowserPage
import io.openflux.app.theme.AppTheme
import io.openflux.desktop.model.SetupPages
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * A script's own page with room for the sign-in window it may ask for (window.openfluxLogin, see
 * [SetupPages.LoginRequest]): the window for the site opens on top of the page, the page stays alive underneath,
 * and when the person has signed in (or goes back) the page is shown again and its promise settled. Whoever shows a
 * script's own page - the connection's setup request, a node's setup wizard - shows a [WebStack] and gets this.
 *
 * The window starts from a site with nothing stored ([WebPage.forgetSite]), so what it reads is the account just
 * signed in, reads only what the page asked for (one localStorage key, or the site's cookies once it answers as signed
 * in) and only while the window is on the site it was opened for. A window that was asked for a page reads nothing of
 * the site at all but its address (and, on the site it was opened for, the page's title): it ends when the address
 * matches the pattern the page gave, or when the person says "this is the page".
 */
@Stable
class WebStack private constructor() : BrowserPage {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var base: WebPage
    private var topPage by mutableStateOf<WebPage?>(null)

    /** The page the person sees now: the script's, or the sign-in window over it. */
    val top: WebPage get() = topPage ?: base

    /** The title of the sign-in window while one is open; null while the script's own page is shown. */
    var signInTitle by mutableStateOf<String?>(null)
        private set

    /** The line the sign-in window's bar shows instead of the usual one (the page asked for a page and said what to open); null for the usual. */
    var signInHint by mutableStateOf<String?>(null)
        private set

    /** Whether the open sign-in window can be told "this is the page": its request was for a page. */
    var signInCanTake by mutableStateOf(false)
        private set

    @Volatile var closed = false
        private set

    private class SignIn(val request: SetupPages.LoginRequest, val origin: String, val window: WebPage) {
        var job: Job? = null
    }

    @Volatile private var signIn: SignIn? = null

    /** The script's own page, for callers that read its address or reload it. */
    val page: WebPage get() = base

    private fun attach(page: WebPage) {
        base = page
        topPage = page
    }

    /** Reloads what is shown: the sign-in window, else the script's page. */
    fun reload() {
        val run = signIn
        if (run != null) run.window.load(run.request.url) else base.load(base.url)
    }

    /** Back from the sign-in window to the script's page; the page's promise is rejected. */
    fun back() {
        signIn?.let { end(it, null, "Вход отменён") }
    }

    /** window.openfluxLogin(spec) from the page: a window for the site on top of it. */
    private fun requestLogin(json: String) {
        scope.launch {
            fun refuse(id: String, why: String) = base.deliver(SetupPages.loginResultScript(id, false, error = why))
            if (closed) return@launch
            if (signIn != null) return@launch refuse(SetupPages.loginId(json), "Окно входа уже открыто")
            val request = try {
                SetupPages.parseLogin(json)
            } catch (e: IllegalArgumentException) {
                return@launch refuse(SetupPages.loginId(json), e.message.orEmpty())
            }
            val origin = SetupPages.httpsOrigin(request.url) ?: return@launch refuse(request.id, "Окно входа открывает только https-сайт")
            WebPage.forgetSite(origin)
            val window = WebPage(
                request.url, scripts = true, userAgent = WebPage.SIGN_IN_USER_AGENT,
                clearStorage = if (request.storageKey.isEmpty()) emptyList() else listOf(request.storageKey),
            )
            val run = SignIn(request, origin, window)
            signIn = run
            signInTitle = request.title
            signInHint = request.pageHint.ifEmpty { null }
            signInCanTake = request.capturesPage
            topPage = window
            run.job = scope.launch { if (request.capturesPage) watchPage(run) else watch(run) }
        }
    }

    /**
     * While the window is on the site it was opened for, asks it whether someone has signed in: for a localStorage
     * request that is the asked key being there, for a cookie request the site's own "who am I" path answering
     * JSON (then the window's cookies go back with it, see [SetupPages.cookieCapture]).
     */
    private suspend fun watch(run: SignIn) {
        val cookies = run.request.signedInPath.isNotEmpty()
        val read = if (cookies) SetupPages.signedInProbe(run.request.signedInPath)
        else "(function(){try{var v=localStorage.getItem(${JSONObject.quote(run.request.storageKey)});return v===null?'':'v:'+v}catch(e){return ''}})()"
        val deadline = System.currentTimeMillis() + SIGN_IN_TIMEOUT_MS
        while (signIn === run && !run.window.closed) {
            if (System.currentTimeMillis() > deadline) return end(run, null, "Время на вход вышло")
            if (SetupPages.isOn(run.origin, run.window.url)) {
                val stored = try {
                    run.window.evaluate(read, if (cookies) SIGN_IN_PROBE_TIMEOUT_MS else SIGN_IN_POLL_TIMEOUT_MS)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    "" // the page is not up yet, or is changing: ask again
                }
                if (stored.startsWith("v:") && stored.length > 2) {
                    val value = if (cookies) {
                        SetupPages.cookieCapture(run.window.cookieHeader(run.origin + "/"), WebPage.SIGN_IN_USER_AGENT, stored.substring(2))
                    } else {
                        stored.substring(2)
                    }
                    return end(run, value, "")
                }
            }
            // A cookie site is asked over the network each time: not as often as a key in the page's own storage.
            delay(if (cookies) SIGN_IN_COOKIE_POLL_MS else SIGN_IN_POLL_MS)
        }
    }

    /**
     * A window that was asked for a page: waits until its address has matched the request's pattern twice running (a
     * redirect or a single-page app passes through addresses on its way), then hands the address back.
     */
    private suspend fun watchPage(run: SignIn) {
        val regex = run.request.pageRegex
        val deadline = System.currentTimeMillis() + SIGN_IN_TIMEOUT_MS
        var last = ""
        var steady = 0
        while (signIn === run && !run.window.closed) {
            if (System.currentTimeMillis() > deadline) return end(run, null, "Время на вход вышло")
            val url = run.window.url
            if (regex != null && SetupPages.httpsOrigin(url) != null && regex.containsMatchIn(url)) {
                steady = if (url == last) steady + 1 else 1
                last = url
                if (steady >= PAGE_STEADY_POLLS) return end(run, captureFor(run, url), "")
            } else {
                last = ""
                steady = 0
            }
            delay(SIGN_IN_POLL_MS)
        }
    }

    /** "Это она": the page the window is on goes back to the script's page, whether or not its address matches (https only). */
    fun take() {
        val run = signIn ?: return
        if (!run.request.capturesPage) return
        val url = run.window.url
        if (SetupPages.httpsOrigin(url) == null) return
        scope.launch { end(run, captureFor(run, url), "") }
    }

    /**
     * What a page-completing window hands back: the site's whole session ([SetupPages.cookieCapture], no probe) when
     * the request asked for cookies, else the address of the page ([SetupPages.pageCapture]). The cookies are the ones
     * the site the window was opened for would get, whatever address the window ended on.
     */
    private suspend fun captureFor(run: SignIn, url: String): String =
        if (run.request.cookies)
            SetupPages.cookieCapture(run.window.cookieHeader(run.origin + "/"), WebPage.SIGN_IN_USER_AGENT, "")
        else
            SetupPages.pageCapture(url, pageTitle(run, url))

    /** The title of the page the window shows, only while it is on the site it was opened for; "" otherwise or when the page does not answer. */
    private suspend fun pageTitle(run: SignIn, url: String): String {
        if (!SetupPages.isOn(run.origin, url)) return ""
        return try {
            val v = run.window.evaluate("(function(){try{return 'v:'+document.title}catch(e){return ''}})()", SIGN_IN_POLL_TIMEOUT_MS)
            if (v.startsWith("v:")) v.substring(2).take(MAX_PAGE_TITLE) else ""
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ""
        }
    }

    /** Closes the sign-in window, shows the script's page again and settles its promise: [value] if the site had a sign-in, else [error]. */
    private fun end(run: SignIn, value: String?, error: String) {
        if (signIn !== run) return
        signIn = null
        run.job?.cancel()
        scope.launch {
            topPage = base
            signInTitle = null
            signInHint = null
            signInCanTake = false
            run.window.close()
            base.deliver(
                if (value != null) SetupPages.loginResultScript(run.request.id, true, value)
                else SetupPages.loginResultScript(run.request.id, false, error = error),
            )
        }
    }

    fun close() {
        if (closed) return
        closed = true
        signIn?.let { it.window.close() }
        signIn = null
        scope.cancel()
        base.close()
    }

    companion object {
        /** A sign-in window waits this long for the person, looks at the site this often, and gives a look this long. */
        private const val SIGN_IN_TIMEOUT_MS = 10 * 60_000L
        private const val SIGN_IN_POLL_MS = 700L
        private const val SIGN_IN_POLL_TIMEOUT_MS = 4_000L
        private const val SIGN_IN_COOKIE_POLL_MS = 2_000L
        private const val SIGN_IN_PROBE_TIMEOUT_MS = 10_000L
        private const val PAGE_STEADY_POLLS = 2
        private const val MAX_PAGE_TITLE = 200

        /** The script's own server page (an address on 127.0.0.1) that [onSubmit] answers. */
        fun own(url: String, onSubmit: (String) -> Unit): WebStack = WebStack().also { s ->
            s.attach(WebPage(url, own = true, onSubmit = onSubmit, onLogin = s::requestLogin))
        }

        /** The script's inline page. */
        fun inline(html: String, onSubmit: (String) -> Unit): WebStack = WebStack().also { s ->
            s.attach(WebPage(html = html, onSubmit = onSubmit, onLogin = s::requestLogin))
        }
    }
}

/** The strip above a sign-in window: where the person is, what to do there, "this is the page" when a page was asked for, and the way back to the script's page. */
@Composable
internal fun SignInBar(title: String, hint: String?, canTake: Boolean, onTake: () -> Unit, onBack: () -> Unit, onReload: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.l, vertical = AppTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = AppTheme.type.body, color = AppTheme.colors.text)
            val line = hint ?: if (canTake) "Откройте нужную страницу: окно закроется само." else "Войдите в аккаунт: окно закроется само."
            Text(line, style = AppTheme.type.bodySmall, color = AppTheme.colors.textSecondary)
        }
        if (canTake) TextAction("Это она", onTake)
        TextAction("Заново", onReload, color = AppTheme.colors.textSecondary)
        TextAction("Назад", onBack, color = AppTheme.colors.textSecondary)
    }
}
