package com.phoobobo.rnwebviewplus;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.common.build.ReactBuildConfig;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.views.webview.ReactWebViewManager;
import com.facebook.react.views.webview.WebViewConfig;
import com.phoobobo.rnwebviewplus.events.TopOverrideLoadingUrlEvent;

import java.util.ArrayList;
import java.util.Map;

import javax.annotation.Nullable;

import static android.app.Activity.RESULT_OK;

/**
 * @author fubaolin
 * 2018-20-26
 */
public class ReactWebViewPlusManager extends ReactWebViewManager implements ActivityResultInterface {

    public static final String REACT_CLASS = "RNWebViewPlus";
    private ReactApplicationContext reactApplicationContext;
    private ValueCallback mUploadMessage;
    private Uri imageUri;
    private static final int CHOOSE_PHOTO = 10002;

    class PickerActivityEventListener extends BaseActivityEventListener {

        private ActivityResultInterface mCallback;

        public PickerActivityEventListener(ReactApplicationContext reactContext, ActivityResultInterface callback) {
            reactContext.addActivityEventListener(this);
            mCallback = callback;
        }

        // < RN 0.33.0
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            mCallback.callback(requestCode, resultCode, data);
        }

        // >= RN 0.33.0
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            mCallback.callback(requestCode, resultCode, data);
        }
    }

    public ReactWebViewPlusManager(ReactApplicationContext reactApplicationContext) {
        this.reactApplicationContext = reactApplicationContext;
        new PickerActivityEventListener(reactApplicationContext, this);
        mWebViewConfig = new WebViewConfig() {
            public void configWebView(WebView webView) {
            }
        };
    }


    @Override
    public void callback(int requestCode, int resultCode, Intent data) {
        if (mUploadMessage == null) {
            return;
        }
        switch (requestCode) {
            case CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mUploadMessage.onReceiveValue(new Uri[]{data.getData()});
                    } else {
                        mUploadMessage.onReceiveValue(data.getData());
                    }
                    mUploadMessage = null;
                } else {
                    mUploadMessage.onReceiveValue(null);
                    mUploadMessage = null;
                    return;
                }
                break;
            default:
                break;
        }
    }


    @Override
    public String getName() {
        return REACT_CLASS;
    }

    protected ReactWebView createReactWebViewInstance(ThemedReactContext reactContext) {
        return new ReactWebViewPlus(reactContext);
    }

    @Override
    protected WebView createViewInstance(ThemedReactContext reactContext) {
        WebView webView = super.createViewInstance(reactContext);
        webView.setWebChromeClient(new WebChromeClient() {
            // For Android 4.1
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                if (mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(null);
                }
                mUploadMessage = uploadMsg;
                showPopSelectPic();
            }

            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(null);
                }
                mUploadMessage = filePathCallback;
                showPopSelectPic();
                return true;
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage message) {
                if (ReactBuildConfig.DEBUG) {
                    return super.onConsoleMessage(message);
                }
                // Ignore console logs in non debug builds.
                return true;
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });
        return webView;
    }

    @Override
    protected void addEventEmitters(ThemedReactContext reactContext, WebView view) {
        // Do not register default touch emitter and let WebView implementation handle touches
        ReactWebViewPlusClient mReactWebViewPlusClient = new ReactWebViewPlusClient();
        view.setWebViewClient(mReactWebViewPlusClient);

    }

    private void openAlbum() {
        if (reactApplicationContext != null) {
            Intent intent = new Intent("android.intent.action.GET_CONTENT");
            intent.setType("image/*");
            Activity currentActivity = reactApplicationContext.getCurrentActivity();
            currentActivity.startActivityForResult(intent, CHOOSE_PHOTO);
        }
    }

    private void showPopSelectPic() {
        if (reactApplicationContext == null) {
            return;
        }
        if (ContextCompat.checkSelfPermission(reactApplicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//          ActivityCompat.requestPermissions(Context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);  //请求文件权限
        } else {
            openAlbum();
        }
    }


    @Override
    public Map getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.of(
                TopOverrideLoadingUrlEvent.EVENT_NAME, MapBuilder.of("registrationName", "onOverrideLoadingUrl")
        );
    }

    protected static class ReactWebViewPlusClient extends ReactWebViewManager.ReactWebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // 分发
            dispatchEvent(view, new TopOverrideLoadingUrlEvent(view.getId(), url));

            boolean useDefaultIntent = false;
            if (mUrlPrefixesForDefaultIntent != null && mUrlPrefixesForDefaultIntent.size() > 0) {
                ArrayList<Object> urlPrefixesForDefaultIntent =
                        mUrlPrefixesForDefaultIntent.toArrayList();
                for (Object urlPrefix : urlPrefixesForDefaultIntent) {
                    if (url.startsWith((String) urlPrefix)) {
                        useDefaultIntent = true;
                        break;
                    }
                }
            }

            if (!useDefaultIntent &&
                    (url.startsWith("http://") || url.startsWith("https://") ||
                            url.startsWith("file://") || url.equals("about:blank"))) {
                return false;
            } else {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    view.getContext().startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    FLog.w(ReactConstants.TAG, "activity not found to handle uri scheme for: " + url, e);
                }
                return true;
            }
        }


    }

    protected static class ReactWebViewPlus extends ReactWebViewManager.ReactWebView {

        protected @Nullable
        ReactWebViewPlusClient mReactWebViewPlusClient;

        /**
         * WebView must be created with an context of the current activity
         * <p>
         * Activity Context is required for creation of dialogs internally by WebView
         * Reactive Native needed for access to ReactNative internal system functionality
         *
         * @param reactContext
         */
        public ReactWebViewPlus(ThemedReactContext reactContext) {
            super(reactContext);
        }

        public @Nullable
        ReactWebViewPlusClient getReactWebViewClient() {
            return mReactWebViewPlusClient;
        }

        @Override
        public void setWebViewClient(WebViewClient client) {
            super.setWebViewClient(client);
            mReactWebViewPlusClient = (ReactWebViewPlusClient) client;
        }
    }
}
