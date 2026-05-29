package com.kanakku.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface; // Required import
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    // ── Your hosted web app URL ──
    public static final String APP_URL =
            "https://boobalanashokan.github.io/expense-tracker/index.html";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);

        // Configure Native Google Sign-In
        // IMPORTANT: Replace "YOUR_FIREBASE_WEB_CLIENT_ID" with your Web Client ID from Firebase Console Credentials
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("897105367693-26uao36n1cqqnpn0spcdnv60ghea0bgp.apps.googleusercontent.com")
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Handle widget actions
        handleIntent(getIntent());

        setupWebView();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(APP_URL);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setAllowFileAccess(true);

        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        settings.setMediaPlaybackRequiresUserGesture(false);

        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Cookies
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient());

        // Register the JS Bridge to handle Google clicks securely
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidAuth");

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request
            ) {

                String url = request.getUrl().toString();

                // Open authentication pages externally ONLY if not entering native paths
                if (url.contains("accounts.google.com") ||
                    url.contains("firebaseapp.com") ||
                    url.contains("__/auth") ||
                    url.contains("oauth")) {

                    Intent intent = new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );

                    startActivity(intent);

                    return true;
                }

                // Keep app URLs inside WebView
                if (url.contains("boobalanashokan.github.io") ||
                    url.contains("script.google.com") ||
                    url.contains("googleapis.com")) {

                    return false;
                }

                // Open all other URLs externally
                Intent intent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(url)
                );

                startActivity(intent);

                return true;
            }
        });
    }

    // JS interface inside MainActivity
    public class WebAppInterface {
        @JavascriptInterface
        public void startGoogleSignIn() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Trigger Android Native Login Options Drawer
                    Intent signInIntent = googleSignInClient.getSignInIntent();
                    startActivityForResult(signInIntent, RC_SIGN_IN);
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    String idToken = account.getIdToken();
                    // Inject and send native Web Token to your JS engine
                    passTokenToWeb(idToken);
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void passTokenToWeb(String idToken) {
        if (webView != null) {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    // Call client JS handler hook
                    webView.evaluateJavascript(
                            "if (typeof handleNativeGoogleAuth === 'function') handleNativeGoogleAuth('" + idToken + "');",
                            null
                    );
                }
            });
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        if (intent != null &&
            "com.kanakku.QUICK_ADD".equals(intent.getAction())) {

            if (webView != null) {

                webView.evaluateJavascript(
                        "if(typeof showAdd==='function')showAdd();",
                        null
                );
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {

        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh widget
        Intent refresh = new Intent("com.kanakku.WIDGET_REFRESH");

        refresh.setPackage(getPackageName());

        sendBroadcast(refresh);
    }
}