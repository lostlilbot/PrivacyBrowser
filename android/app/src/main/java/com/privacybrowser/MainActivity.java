package com.privacybrowser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

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
        
        // Disable geolocation (privacy)
        webSettings.setGeolocationEnabled(false);
        
        // Disable save password dialog (privacy)
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);

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
        if (!isNetworkAvailable()) {
            Toast.makeText(this, R.string.offline_message, Toast.LENGTH_LONG).show();
            return;
        }

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
            // Return false to let WebView handle the URL
            return false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            updateNavigationButtons();
            updateUrlBar();
        }
    }
}
