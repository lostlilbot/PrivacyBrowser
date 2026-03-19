package com.privacybrowser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

/**
 * Privacy-focused web browser application.
 * Features:
 * - No analytics, tracking, or reporting
 * - Hardware acceleration enabled
 * - Efficient caching
 * - All links load within the app
 */
public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText urlEditText;
    private ProgressBar progressBar;
    private Button btnBack, btnForward, btnRefresh, btnStop, btnHome;
    
    private static final String DEFAULT_HOME_URL = "https://www.duckduckgo.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        initViews();

        // Configure WebView with privacy and performance settings
        configureWebView();

        // Set up navigation button click listeners
        setupNavigationButtons();

        // Handle URL input
        setupUrlInput();

        // Set up back button handling
        setupBackPressedHandler();

        // Load default home page
        loadUrl(DEFAULT_HOME_URL);

        // Handle incoming intents (e.g., from browser intent filter)
        handleIncomingIntent(getIntent());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initViews() {
        webView = findViewById(R.id.webView);
        urlEditText = findViewById(R.id.urlEditText);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);
        btnForward = findViewById(R.id.btnForward);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnStop = findViewById(R.id.btnStop);
        btnHome = findViewById(R.id.btnHome);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        // Enable hardware acceleration for better performance
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        WebSettings webSettings = webView.getSettings();

        // Performance optimizations
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        
        // Cache configuration - use default cache behavior
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // Enable zoom controls
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        
        // Enable viewport for better responsive content
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        // Privacy settings - disable file and content access
        webSettings.setAllowFileAccess(false);
        webSettings.setAllowContentAccess(false);
        webSettings.setAllowFileAccessFromFileURLs(false);
        webSettings.setAllowUniversalAccessFromFileURLs(false);

        // Disable geolocation (privacy)
        webSettings.setGeolocationEnabled(false);

        // Save password/form data is deprecated; explicitly disable form data for privacy
        webSettings.setSaveFormData(false);

        // Modern WebView safe browsing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webSettings.setSafeBrowsingEnabled(true);
        }

        // Disable cookies and third-party cookies by default for privacy
        android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
        cookieManager.setAcceptCookie(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, false);
        }

        // Enable mixed content handling for API 21+
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        // Set custom WebViewClient for internal link handling
        webView.setWebViewClient(new CustomWebViewClient());

        // Set WebChromeClient for progress updates
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                    btnRefresh.setVisibility(View.VISIBLE);
                    btnStop.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    btnRefresh.setVisibility(View.GONE);
                    btnStop.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                // Update title if needed
            }
        });
    }

    private void setupNavigationButtons() {
        // Back button
        btnBack.setOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            }
        });

        // Forward button
        btnForward.setOnClickListener(v -> {
            if (webView.canGoForward()) {
                webView.goForward();
            }
        });

        // Refresh button
        btnRefresh.setOnClickListener(v -> webView.reload());

        // Stop button
        btnStop.setOnClickListener(v -> webView.stopLoading());

        // Home button
        btnHome.setOnClickListener(v -> loadUrl(DEFAULT_HOME_URL));
    }

    private void setupUrlInput() {
        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String input = v.getText().toString().trim();
                if (!input.isEmpty()) {
                    loadUrl(processInput(input));
                }
                return true;
            }
            return false;
        });
    }

    /**
     * Process user input - add https:// prefix if missing
     */
    private String processInput(String input) {
        if (input.isEmpty()) {
            return DEFAULT_HOME_URL;
        }

        // Check if it looks like a URL (contains . or starts with http:// or https://)
        if (!input.startsWith("http://") && !input.startsWith("https://")) {
            if (input.contains(".") && !input.contains(" ")) {
                // Assume it's a domain
                input = "https://" + input;
            } else {
                // Treat as search query - use DuckDuckGo
                input = "https://duckduckgo.com/?q=" + Uri.encode(input);
            }
        }

        return input;
    }

    /**
     * Check network connectivity
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    /**
     * Load URL with network check
     */
    private void loadUrl(String url) {
        if (url == null || url.isEmpty()) {
            url = DEFAULT_HOME_URL;
        }

        if (!isNetworkAvailable()) {
            Toast.makeText(this, R.string.offline_message, Toast.LENGTH_LONG).show();
            showErrorPage();
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("file://")) {
            url = processInput(url);
        }

        urlEditText.setText(url);
        webView.loadUrl(url);
    }

    /**
     * Update navigation button states based on WebView history
     */
    private void updateNavigationButtons() {
        // Update back button
        btnBack.setEnabled(webView.canGoBack());
        btnBack.setAlpha(webView.canGoBack() ? 1.0f : 0.5f);

        // Update forward button
        btnForward.setEnabled(webView.canGoForward());
        btnForward.setAlpha(webView.canGoForward() ? 1.0f : 0.5f);
    }

    /**
     * Update URL in address bar
     */
    private void updateUrlBar() {
        String currentUrl = webView.getUrl();
        if (currentUrl != null && !currentUrl.isEmpty()) {
            urlEditText.setText(currentUrl);
        }
    }

    /**
     * Handle incoming intents (e.g., from browser intent filter)
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            String url = intent.getDataString();
            if (url != null && !url.isEmpty()) {
                loadUrl(url);
            }
        }
    }

    /**
     * Handle back button press - navigate history if possible
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Set up modern back button handling using OnBackPressedCallback
     */
    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    // If can't go back, finish the activity
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNavigationButtons();
    }

    @Override
    protected void onDestroy() {
        // Destroy WebView to free resources
        if (webView != null) {
            webView.stopLoading();
            webView.clearHistory();
            webView.clearCache(true);
            webView.loadUrl("about:blank");
            webView.removeAllViews();
            webView.destroy();
        }
        super.onDestroy();
    }

    /**
     * Custom WebViewClient to load all links within the app
     */
    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Load all URLs within the WebView (internal link handling)
            return false;
        }

        // For API 24+
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
            btnRefresh.setVisibility(View.GONE);
            btnStop.setVisibility(View.VISIBLE);
            urlEditText.setText(url);
            setTitle(url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                showErrorPage();
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            showErrorPage();
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, android.webkit.WebResourceResponse errorResponse) {
            if (request.isForMainFrame()) {
                showErrorPage();
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            btnRefresh.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.GONE);
            updateNavigationButtons();
            updateUrlBar();
            setTitle(view.getTitle());
        }
    }

    private void showErrorPage() {
        String errorHtml = "<html><body><h2>" + getString(R.string.error_loading_page) + "</h2><p>" + getString(R.string.offline_message) + "</p></body></html>";
        webView.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
    }
}

