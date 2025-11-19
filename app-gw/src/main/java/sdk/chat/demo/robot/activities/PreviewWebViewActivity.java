package sdk.chat.demo.robot.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import sdk.chat.demo.pre.R;

public class PreviewWebViewActivity extends BaseActivity implements View.OnClickListener {

    public static final String EXTRA_HTML_CONTENT = "html_content";
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_TITLE = "title";

    private WebView webView;
    private ProgressBar progressBar;
    private TextView titleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        findViewById(R.id.home).setOnClickListener(this);
        titleView = findViewById(R.id.title);
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        setupWebView();
        loadContent();
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();

        // 基础设置
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(false);

        // 缓存设置
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 安全设置
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        // 设置WebViewClient和WebChromeClient
        webView.setWebViewClient(new MyWebViewClient());
        webView.setWebChromeClient(new MyWebChromeClient());

        // 硬件加速
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    private void loadContent() {
        Intent intent = getIntent();

        if (intent.hasExtra(EXTRA_HTML_CONTENT)) {
            // 加载HTML字符串
            String htmlContent = intent.getStringExtra(EXTRA_HTML_CONTENT);
            String mimeType = "text/html";
            String encoding = "UTF-8";
            String baseUrl = "file:///android_asset/";

            webView.loadDataWithBaseURL(baseUrl, htmlContent, mimeType, encoding, null);
        } else if (intent.hasExtra(EXTRA_URL)) {
            // 加载URL
            String url = intent.getStringExtra(EXTRA_URL);
            webView.loadUrl(url);
        }

        // 设置标题
        if (intent.hasExtra(EXTRA_TITLE)) {
            setTitle(intent.getStringExtra(EXTRA_TITLE));
        }
    }

    @Override
    public void onClick(View view) {
        int vid = view.getId();
        if (vid == R.id.home) {
            finish();
        }
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);

            // 动态设置标题
            if (getIntent().getStringExtra(EXTRA_TITLE) == null && view.getTitle() != null) {
                setTitle(view.getTitle());
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            return handleUrlLoading(view, url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return handleUrlLoading(view, url);
        }

        private boolean handleUrlLoading(WebView view, String url) {
            // 处理特殊URL
            if (url.startsWith("tel:")) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                startActivity(intent);
                return true;
            } else if (url.startsWith("mailto:")) {
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                startActivity(intent);
                return true;
            }

            // 其他情况由WebView处理
            return false;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                showError("Error: " + error.getDescription());
            }
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
            showError("HTTP Error: " + errorResponse.getStatusCode());
        }
    }

    private class MyWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            progressBar.setProgress(newProgress);
        }
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
//        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
    protected int getLayout() {
        return 0;
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.setWebViewClient(null);
            webView.setWebChromeClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        titleView.setText(title);
    }

    // 启动Activity的静态方法
    public static void launchWithHtml(Context context, String htmlContent, String title) {
        Intent intent = new Intent(context, PreviewWebViewActivity.class);
        intent.putExtra(EXTRA_HTML_CONTENT, htmlContent);
        intent.putExtra(EXTRA_TITLE, title);
        context.startActivity(intent);
    }

    public static void launchWithUrl(Context context, String url, String title) {
        Intent intent = new Intent(context, PreviewWebViewActivity.class);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_TITLE, title);
        context.startActivity(intent);
    }
}