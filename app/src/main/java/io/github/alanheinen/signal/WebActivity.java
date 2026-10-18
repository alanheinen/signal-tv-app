package io.github.alanheinen.signal;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * A full-screen web view on one Signal site in TV mode (?tv=1). The page does
 * the navigation; this hands it the keys a web view doesn't pass on (Back, the
 * channel and media keys), pauses playback while the app is in the
 * background, and keeps the screen on while someone is using it.
 */
public abstract class WebActivity extends Activity {
    /** The screen may sleep after this long without a key press. */
    private static final long AWAKE_MS = 4 * 60 * 60 * 1000L;

    /** Remote keys a web view doesn't deliver to the page, by DOM key name. */
    private static final SparseArray<String> KEYS = new SparseArray<>();
    static {
        KEYS.put(KeyEvent.KEYCODE_CHANNEL_UP, "ChannelUp");
        KEYS.put(KeyEvent.KEYCODE_CHANNEL_DOWN, "ChannelDown");
        KEYS.put(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "MediaPlayPause");
        KEYS.put(KeyEvent.KEYCODE_MEDIA_PLAY, "MediaPlay");
        KEYS.put(KeyEvent.KEYCODE_MEDIA_PAUSE, "MediaPause");
        KEYS.put(KeyEvent.KEYCODE_MEDIA_STOP, "MediaStop");
        KEYS.put(KeyEvent.KEYCODE_MEDIA_NEXT, "MediaTrackNext");
        KEYS.put(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "MediaTrackPrevious");
        KEYS.put(KeyEvent.KEYCODE_GUIDE, "Guide");
        KEYS.put(KeyEvent.KEYCODE_INFO, "Info");
    }

    private static final String PAUSE_ALL =
        "(() => { const ms = [...document.querySelectorAll('video, audio')];" +
        " const on = ms.some((m) => !m.paused); ms.forEach((m) => m.pause()); return on; })()";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable sleep = () -> getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    private WebView web;
    private boolean showingError;
    private boolean resumePlayback;

    /** The site's address, from the build (see app/build.gradle.kts). */
    protected abstract String siteUrl();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (BuildConfig.WEB_DEBUG) WebView.setWebContentsDebuggingEnabled(true);
        web = new WebView(this);
        web.setBackgroundColor(getColor(R.color.bg));
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString(s.getUserAgentString() + " SignalTVApp/" + BuildConfig.VERSION_NAME);

        web.setWebViewClient(new Client());
        web.setWebChromeClient(new WebChromeClient() {
            // A video with no poster otherwise shows a grey play button while it loads.
            @Override
            public Bitmap getDefaultVideoPoster() {
                return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            }
        });

        if (state == null || web.restoreState(state) == null) load();
        awake();
    }

    private void load() {
        showingError = false;
        web.loadUrl(Uri.parse(siteUrl()).buildUpon().appendQueryParameter("tv", "1").build().toString());
        web.requestFocus();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        awake();
        int code = e.getKeyCode();
        if (code == KeyEvent.KEYCODE_BACK) {
            if (e.getAction() == KeyEvent.ACTION_UP && !e.isCanceled()) back();
            return true;
        }
        String key = KEYS.get(code);
        if (key != null) {
            if (e.getAction() == KeyEvent.ACTION_DOWN && !showingError) send(key);
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    private void send(String key) {
        web.evaluateJavascript("window.dispatchEvent(new KeyboardEvent('keydown', { key: '" + key + "', bubbles: true }))", null);
    }

    /** The page decides what Back means; when it has nothing to go back from, leave. */
    private void back() {
        if (showingError) { finish(); return; }
        web.evaluateJavascript("typeof window.signalBack === 'function' && window.signalBack()", (r) -> {
            if (!"true".equals(r)) finish();
        });
    }

    private void awake() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        handler.removeCallbacks(sleep);
        handler.postDelayed(sleep, AWAKE_MS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Home or another app: stop the sound, and pick up again on return.
        web.evaluateJavascript(PAUSE_ALL, (r) -> resumePlayback = "true".equals(r));
        web.onPause();
        handler.removeCallbacks(sleep);
    }

    @Override
    protected void onStart() {
        super.onStart();
        web.onResume();
        awake();
        if (resumePlayback) {
            resumePlayback = false;
            send("MediaPlay");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(sleep);
        web.destroy();
        super.onDestroy();
    }

    private void showError(String why) {
        showingError = true;
        String host = Html.escapeHtml(Uri.parse(siteUrl()).getHost());
        String html = "<!doctype html><meta name=viewport content='width=device-width'>" +
            "<body style='margin:0;height:100vh;display:grid;place-content:center;gap:1.2rem;text-align:center;" +
            "background:#0d141d;color:#e6ecf2;font:3vw/1.4 system-ui,sans-serif'>" +
            "<div style='font-weight:700;font-size:4.5vw'>Can't reach " + host + "</div>" +
            "<div style='color:#8797ab'>" + Html.escapeHtml(why) + "<br>Check the TV is on your home network.</div>" +
            "<a id=r href='signal-retry:' style='justify-self:center;padding:.6em 1.4em;border-radius:.5em;" +
            "background:#f2a93b;color:#1b1204;text-decoration:none;font-weight:700;outline:none'>Try again</a>" +
            "<div style='color:#5d6d82;font-size:2vw'>OK to try again · Back to leave</div>" +
            "<script>document.getElementById('r').focus()</script></body>";
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    private class Client extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
            Uri u = req.getUrl();
            if ("signal-retry".equals(u.getScheme())) { load(); return true; }
            // Stay on the site: there's no way back from anywhere else.
            return !Uri.parse(siteUrl()).getHost().equals(u.getHost());
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
            if (req.isForMainFrame()) showError(String.valueOf(err.getDescription()));
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest req, WebResourceResponse res) {
            if (req.isForMainFrame() && res.getStatusCode() >= 500) showError("The server answered " + res.getStatusCode() + ".");
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            view.requestFocus();
        }
    }
}
