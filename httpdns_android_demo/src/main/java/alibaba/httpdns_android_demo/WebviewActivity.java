package alibaba.httpdns_android_demo;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.alibaba.sdk.android.httpdns.HttpDns;
import com.alibaba.sdk.android.httpdns.HttpDnsService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class WebviewActivity extends AppCompatActivity {
    private WebView webView;
    private static final String targetUrl = "http://www.apple.com";
    private static final String TAG = "WebviewScene";
    private static HttpDnsService httpdns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        getSupportActionBar().setTitle(R.string.webview_scene);

        // 初始化httpdns
        httpdns = HttpDns.getService(getApplicationContext(), MainActivity.accountID);
        // 预解析热点域名
        httpdns.setPreResolveHosts(new ArrayList<>(Arrays.asList("www.apple.com")));

        webView = (WebView) this.findViewById(R.id.wv_container);
        webView.setWebViewClient(new WebViewClient() {
            @SuppressLint("NewApi")
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // 新的API可以拦截POST请求
                if (request == null || request.getUrl() == null ||
                        !request.getMethod().equalsIgnoreCase("get")) {
                    return null;
                }
                String scheme = request.getUrl().getScheme().trim();
                Map<String, String> headerFields = request.getRequestHeaders();
                String url = request.getUrl().toString();
                Log.d(TAG, "request.getUrl().toString(): " + url);

                // 假设我们对所有css文件的网络请求进行httpdns解析
                if (!scheme.equalsIgnoreCase("http") ||
                        !scheme.equalsIgnoreCase("https") ||
                        !request.getUrl().toString().contains(".css")) {
                    return null;
                }

                try {
                    URL oldUrl = new URL(url);
                    URLConnection connection;
                    // 异步获取域名解析结果
                    String ip = httpdns.getIpByHostAsync(oldUrl.getHost());

                    /**
                     * 降级处理逻辑：
                     * HTTPDNS无法返回对应Host的解析结果IP时，不拦截处理该请求，交由其他注册Protocol或系统原生网络库处理。
                     * 基于此，可通过控制台下线域名，动态控制客户端降级。
                     * 【注意】当HTTPDNS不可用时，一定要做好降级处理，减少网络请求处理的无意义干涉，降低风险。
                     */
                    if (ip == null) {
                        return null;
                    }

                    // 通过HTTPDNS获取IP成功，进行URL替换和HOST头设置
                    Log.d(TAG, "Get IP: " + ip + " for host: " + oldUrl.getHost() + " from HTTPDNS successfully!");
                    String newUrl = url.replaceFirst(oldUrl.getHost(), ip);
                    connection = new URL(newUrl).openConnection();
                    // 设置HTTP请求头Host域
                    connection.setRequestProperty("Host", oldUrl.getHost());
                    // copy请求header参数
                    for (String header : headerFields.keySet()) {
                        connection.setRequestProperty(header, headerFields.get(header));
                    }
                    // 注*：对于POST请求的Body数据，WebResourceRequest接口中并没有提供，这里无法处理
                    Log.d(TAG, "ContentType: " + connection.getContentType());
                    return new WebResourceResponse(connection.getContentType(), "UTF-8", connection.getInputStream());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                // API < 21 只能拦截URL参数
                if (TextUtils.isEmpty(url) || Uri.parse(url).getScheme() == null) {
                    return null;
                }

                String scheme = Uri.parse(url).getScheme().trim();
                Log.d(TAG, "url: " + url);
                // 假设我们对所有css文件的网络请求进行httpdns解析
                if (!scheme.equalsIgnoreCase("http") || !scheme.equalsIgnoreCase("https")
                        || !url.toString().contains(".css")) {
                    return null;
                }

                try {
                    URL oldUrl = new URL(url);
                    URLConnection connection = oldUrl.openConnection();
                    // 异步获取域名解析结果
                    String ip = httpdns.getIpByHostAsync(oldUrl.getHost());
                    /**
                     * 降级处理逻辑：
                     * HTTPDNS无法返回对应Host的解析结果IP时，不拦截处理该请求，交由其他注册Protocol或系统原生网络库处理。
                     * 基于此，可通过控制台下线域名，动态控制客户端降级。
                     * 【注意】当HTTPDNS不可用时，一定要做好降级处理，减少网络请求处理的无意义干涉，降低风险。
                     */
                    if (ip == null) {
                        return null;
                    }

                    // 通过HTTPDNS获取IP成功，进行URL替换和HOST头设置
                    Log.d(TAG, "Get IP: " + ip + " for host: " + oldUrl.getHost() + " from HTTPDNS successfully!");
                    String newUrl = url.replaceFirst(oldUrl.getHost(), ip);
                    connection = new URL(newUrl).openConnection();
                    // 设置HTTP请求头Host域
                    connection.setRequestProperty("Host", oldUrl.getHost());
                    Log.d(TAG, "ContentType: " + connection.getContentType());
                    return new WebResourceResponse(connection.getContentType(), "UTF-8", connection.getInputStream());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        webView.loadUrl(targetUrl);
    }
}
