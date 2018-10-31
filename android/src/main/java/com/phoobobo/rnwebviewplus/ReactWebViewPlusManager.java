package com.phoobobo.rnwebviewplus;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.facebook.common.logging.FLog;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.views.webview.ReactWebViewManager;

import com.phoobobo.rnwebviewplus.events.TopOverrideLoadingUrlEvent;

import java.util.ArrayList;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * @author fubaolin
 * 2018-20-26
 */
public class ReactWebViewPlusManager extends ReactWebViewManager {

    public static final String REACT_CLASS = "RNWebViewPlus";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    protected ReactWebView createReactWebViewInstance(ThemedReactContext reactContext) {
        return new ReactWebViewPlus(reactContext);
    }

    @Override
    protected void addEventEmitters(ThemedReactContext reactContext, WebView view) {
        // Do not register default touch emitter and let WebView implementation handle touches
        view.setWebViewClient(new ReactWebViewPlusClient());
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
