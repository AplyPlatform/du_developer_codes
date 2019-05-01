package io.droneplay.droneplaymission.Activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import io.droneplay.droneplaymission.R;
import io.droneplay.droneplaymission.utils.HelperUtils;


public class LoginActivity extends AppCompatActivity {

    private static ProgressDialog spinner = null;
    private WebView webView;
    private Context mContext;
    public static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 4.1.1; Galaxy Nexus Build/JRO03C) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mContext = getApplicationContext();
        UIInit();
    }


    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

    }

    private void showLoader() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.show();
            }
        });
    }

    private void hideLoader() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.hide();
            }
        });
    }

    private void UIInit() {
        spinner = new ProgressDialog(this);
        spinner.setCancelable(false);

        webView = (WebView) findViewById(R.id.webviewLogin);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setAppCacheEnabled(true);
//        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
//        webSettings.setSupportMultipleWindows(true);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(webView, true);

        }

        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        webView.setWebViewClient(new MyCustomWebViewClient());


        webSettings.setUserAgentString(USER_AGENT);

        webView.loadUrl("https://dev.droneplay.io/center/index.html?fromapp=yes");
    }


    private class MyCustomWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            showLoader();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            view.loadUrl(request.getUrl().toString());
            return super.shouldOverrideUrlLoading(view, request);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            hideLoader();

            if (url.contains("https://accounts.google.com/signin/oauth/consent")) {
                if (webView != null) {
                    webView.loadUrl("https://dev.droneplay.io/center/index.html?fromapp=yes");
                    showToast("확인되었습니다, 구글로 다시 로그인 해 주세요!");
                }
            }
        }
    }

    public class WebAppInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }

        /** Show a toast from the web page */
        @JavascriptInterface
        public void setToken(String token, String email) {
            HelperUtils.getInstance().clientid = email;
            HelperUtils.getInstance().dronePlayToken = token;
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

    }

    private void showToast(final String toastMsg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();

            }
        });
    }


}
