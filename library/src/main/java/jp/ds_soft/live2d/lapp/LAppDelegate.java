package jp.ds_soft.live2d.lapp;

import android.app.Activity;
import android.content.Context;
import jp.ds_soft.live2d.lapp.LAppView;
import jp.ds_soft.live2d.lapp.LAppTextureManager;
import jp.ds_soft.live2d.lapp.LAppLive2DManager;
import com.live2d.sdk.cubism.framework.CubismFramework;

public class LAppDelegate {
    private static LAppDelegate s_instance;
    private Context context;
    private LAppView view;
    private LAppTextureManager textureManager;
    private int windowWidth;
    private int windowHeight;
    private boolean isInitialized = false;

    private LAppDelegate() {}

    public static LAppDelegate getInstance() {
        if (s_instance == null) {
            s_instance = new LAppDelegate();
        }
        return s_instance;
    }

    public void init(Context context) {
        this.context = context;
        if (!isInitialized) {
            CubismFramework.Option option = new CubismFramework.Option();
            option.logFunction = new LAppPal.PrintLogFunction();
            option.loggingLevel = LAppDefine.cubismLoggingLevel;
            CubismFramework.startUp(option);
            CubismFramework.initialize();
            textureManager = new LAppTextureManager();
            isInitialized = true;
        }
    }

    public Context getContext() {
        return context;
    }

    public Activity getActivity() {
        if (context instanceof Activity) {
            return (Activity) context;
        }
        return null;
    }

    public void setView(LAppView view) {
        this.view = view;
    }

    public LAppView getView() {
        return view;
    }

    public LAppTextureManager getTextureManager() {
        return textureManager;
    }

    public void onSurfaceCreated(String initialModelName) {
        if (textureManager != null) {
            textureManager.releaseTextures();
        }
        LAppLive2DManager manager = LAppLive2DManager.getInstance();
        if (initialModelName != null) {
            manager.changeModelByName(initialModelName);
        }
    }

    public void onSurfaceChanged(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        if (view != null) {
            view.onSurfaceChanged(width, height);
            view.initialize();
            view.initializeSprite();
        }
    }

    public void run() {
        LAppPal.updateTime();
        if (view != null) {
            view.render();
        }
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void deactivateApp() {}
    public void onPause() {}
    public void onResume() {
        LAppPal.resetTime();
    }
    public void onStop() {}
    public void onDestroy() {
        LAppLive2DManager.releaseInstance();
        CubismFramework.dispose();
    }

    public void onTouchBegan(float x, float y) {
        if (view != null) {
            view.onTouchesBegan(x, y);
        }
    }

    public void onTouchEnd(float x, float y) {
        if (view != null) {
            view.onTouchesEnded(x, y);
        }
    }

    public void onTouchMoved(float x, float y) {
        if (view != null) {
            view.onTouchesMoved(x, y);
        }
    }
}
