package com.pi.cartubesafe;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final String HOME_URL = "https://m.youtube.com/";
    private static final Set<String> TRACKING_PARAMETERS = new HashSet<>(Arrays.asList(
            "fbclid", "gclid", "dclid", "msclkid", "mc_cid", "mc_eid", "igshid"));
    private static final String[] BLOCKED_HOSTS = {
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "google-analytics.com", "googletagmanager.com", "googletagservices.com",
            "adnxs.com", "adsrvr.org", "amazon-adsystem.com", "criteo.com",
            "criteo.net", "demdex.net", "scorecardresearch.com", "taboola.com",
            "outbrain.com", "hotjar.com", "segment.io", "mixpanel.com",
            "appsflyer.com", "branch.io"
    };

    private FrameLayout root;
    private FrameLayout browser;
    private WebView webView;
    private TextView audioOnlyOverlay;
    private boolean videoAllowed;
    private int blockedRequests;
    private View fullScreenView;
    private WebChromeClient.CustomViewCallback fullScreenCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogStore.i("MainActivity", "onCreate");
        buildUi();
        configureWebView();
        if (savedInstanceState == null) webView.loadUrl(HOME_URL);
        else webView.restoreState(savedInstanceState);
    }

    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        browser = new FrameLayout(this);
        browser.setBackgroundColor(Color.BLACK);
        browser.setVisibility(View.GONE);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        browser.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(browser, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        audioOnlyOverlay = new TextView(this);
        audioOnlyOverlay.setText("Modo de condução seguro\nApenas áudio");
        audioOnlyOverlay.setTextColor(Color.WHITE);
        audioOnlyOverlay.setTextSize(22f);
        audioOnlyOverlay.setGravity(android.view.Gravity.CENTER);
        audioOnlyOverlay.setBackgroundColor(Color.BLACK);
        root.addView(audioOnlyOverlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setGeolocationEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        if (android.os.Build.VERSION.SDK_INT >= 26) settings.setSafeBrowsingEnabled(true);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                installContentProtection(view);
                applyVideoPolicy();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme)) {
                    view.loadUrl(uri.buildUpon().scheme("https").build().toString());
                    return true;
                }
                if (!"https".equalsIgnoreCase(scheme)) return true;
                if (request.isForMainFrame()) {
                    Uri cleaned = removeTrackingParameters(uri);
                    if (!cleaned.equals(uri)) {
                        view.loadUrl(cleaned.toString());
                        return true;
                    }
                }
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("http".equalsIgnoreCase(uri.getScheme()) || isBlockedHost(uri.getHost())) {
                    noteBlockedRequest();
                    return new WebResourceResponse("text/plain", "UTF-8", 403,
                            "Blocked by Escudo Lite", Collections.emptyMap(),
                            new ByteArrayInputStream(new byte[0]));
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (!videoAllowed || fullScreenView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                fullScreenView = view;
                fullScreenCallback = callback;
                browser.setVisibility(View.GONE);
                root.addView(fullScreenView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                keepImmersive();
            }

            @Override
            public void onHideCustomView() {
                exitVideoFullScreen();
            }
        });
    }

    private boolean isBlockedHost(String host) {
        if (host == null) return false;
        String normalized = host.toLowerCase(Locale.ROOT);
        for (String blocked : BLOCKED_HOSTS) {
            if (normalized.equals(blocked) || normalized.endsWith("." + blocked)) return true;
        }
        return false;
    }

    private Uri removeTrackingParameters(Uri uri) {
        if (uri.getQuery() == null) return uri;
        Uri.Builder builder = uri.buildUpon().clearQuery();
        for (String name : uri.getQueryParameterNames()) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.startsWith("utm_") || TRACKING_PARAMETERS.contains(lower)) continue;
            for (String value : uri.getQueryParameters(name)) builder.appendQueryParameter(name, value);
        }
        return builder.build();
    }

    private void noteBlockedRequest() {
        blockedRequests++;
        if (blockedRequests == 1 || blockedRequests % 25 == 0) {
            LogStore.i("Shield", "blockedRequests=" + blockedRequests);
            LogStore.syncDriveBestEffort();
        }
    }

    private void installContentProtection(WebView view) {
        String script = "(function(){if(window.__carTubeShield)return;window.__carTubeShield=true;"
                + "const css='html.cartube-compact body{zoom:.82;width:121.9512%;}"
                + "html.cartube-compact ytm-rich-grid-row{gap:8px!important;}"
                + "html.cartube-compact ytm-rich-item-renderer{margin:0!important;}"
                + "ytd-display-ad-renderer,ytd-ad-slot-renderer,ytm-promoted-sparkles-web-renderer,"
                + "ytm-companion-ad-renderer,ytm-promoted-video-renderer,ytm-statement-banner-renderer,"
                + "#player-ads,.video-ads,.ytp-ad-overlay-container,.ytp-ad-image-overlay,"
                + "ytd-mealbar-promo-renderer{display:none!important}';"
                + "const st=document.createElement('style');st.textContent=css;document.documentElement.appendChild(st);"
                + "const compact=()=>{const p=location.pathname;document.documentElement.classList.toggle('cartube-compact',!p.startsWith('/watch')&&!p.startsWith('/shorts'));};"
                + "const clean=()=>{compact();document.querySelectorAll('ytd-display-ad-renderer,ytd-ad-slot-renderer,"
                + "ytm-promoted-sparkles-web-renderer,ytm-companion-ad-renderer,ytm-promoted-video-renderer,"
                + "ytm-statement-banner-renderer,#player-ads,.video-ads,.ytp-ad-overlay-container').forEach(e=>e.remove());"
                + "const skip=document.querySelector('.ytp-ad-skip-button-modern,.ytp-skip-ad-button');if(skip)skip.click();};"
                + "new MutationObserver(clean).observe(document.documentElement,{childList:true,subtree:true});setInterval(clean,1000);clean();})();";
        view.evaluateJavascript(script, null);
    }

    private void setVideoAllowed(boolean allowed, String reason) {
        if (videoAllowed == allowed) return;
        videoAllowed = allowed;
        LogStore.i("MainActivity", "videoAllowed=" + allowed + "; reason=" + reason);
        applyVideoPolicy();
    }

    private void applyVideoPolicy() {
        if (browser == null || audioOnlyOverlay == null) return;
        if (videoAllowed) {
            browser.setVisibility(View.VISIBLE);
            audioOnlyOverlay.setVisibility(View.GONE);
        } else {
            exitVideoFullScreen();
            browser.setVisibility(View.GONE);
            audioOnlyOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void exitVideoFullScreen() {
        if (fullScreenView == null) return;
        root.removeView(fullScreenView);
        fullScreenView = null;
        browser.setVisibility(videoAllowed ? View.VISIBLE : View.GONE);
        if (fullScreenCallback != null) {
            fullScreenCallback.onCustomViewHidden();
            fullScreenCallback = null;
        }
        keepImmersive();
    }

    private void keepImmersive() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            android.view.WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void pauseVideo() {
        if (webView == null) return;
        try {
            webView.evaluateJavascript("(function(){document.querySelectorAll('video').forEach(function(v){v.pause();});})()", null);
        } catch (Exception ignored) { }
        webView.onPause();
        webView.pauseTimers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.resumeTimers();
            webView.onResume();
        }
        setVideoAllowed(false, "resumed_waiting_for_window_focus");
        LogStore.syncDriveBestEffort();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        setVideoAllowed(hasFocus, hasFocus ? "window_focused" : "window_focus_lost");
        if (hasFocus) root.post(this::keepImmersive);
    }

    @Override
    protected void onPause() {
        setVideoAllowed(false, "activity_paused");
        LogStore.syncDriveBestEffort();
        super.onPause();
    }

    @Override
    protected void onStop() {
        setVideoAllowed(false, "activity_stopped");
        LogStore.syncDriveBestEffort();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        LogStore.i("MainActivity", "onDestroy");
        if (webView != null) {
            pauseVideo();
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        LogStore.syncDriveBestEffort();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (LogStore.handleDriveLinkResult(this, requestCode, resultCode, data)) {
            Toast.makeText(this, "Log ligado ao Drive.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (fullScreenView != null) exitVideoFullScreen();
        else if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
