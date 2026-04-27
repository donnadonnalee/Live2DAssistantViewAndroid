package jp.ds_soft.live2d.lapp;

import com.live2d.sdk.cubism.framework.math.CubismMatrix44;
import com.live2d.sdk.cubism.framework.math.CubismViewMatrix;

public class LAppView {
    public LAppView() {
        viewMatrix = new CubismViewMatrix();
        deviceToScreen = CubismMatrix44.create();
    }

    public void render() {
        LAppLive2DManager live2dManager = LAppLive2DManager.getInstance();
        live2dManager.setViewMatrix(viewMatrix);
        live2dManager.onUpdate();
    }

    public void initialize() {
    }

    public void initializeSprite() {
    }

    public void onTouchesBegan(float x, float y) {
        float touchX = deviceToScreen.transformX(x);
        float touchY = deviceToScreen.transformY(y);
        LAppLive2DManager.getInstance().onTap(touchX, touchY);
    }

    public void onTouchesMoved(float x, float y) {
        float touchX = deviceToScreen.transformX(x);
        float touchY = deviceToScreen.transformY(y);
        LAppLive2DManager.getInstance().onDrag(touchX, touchY);
    }

    public void onTouchesEnded(float x, float y) {
        LAppLive2DManager.getInstance().onDrag(0.0f, 0.0f);
    }

    public void onSurfaceChanged(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        float ratio = (float) width / (float) height;
        float left = -ratio;
        float right = ratio;
        float bottom = -1.0f;
        float top = 1.0f;

        deviceToScreen.loadIdentity();
        if (width > height) {
            deviceToScreen.scale(right * 2.0f / width, -top * 2.0f / height);
        } else {
            deviceToScreen.scale(right * 2.0f / width, -top * 2.0f / height);
        }
        deviceToScreen.translate(-width * 0.5f, -height * 0.5f);

        // 表示範囲の設定 (安定した定数を使用)
        viewMatrix.setMaxScale(2.0f);
        viewMatrix.setMinScale(0.8f);
        viewMatrix.setScreenRect(left, right, bottom, top);
        viewMatrix.setMaxScreenRect(-2.0f, 2.0f, -2.0f, 2.0f);
    }

    public void preModelDraw(LAppModel model) {}
    public void postModelDraw(LAppModel model) {}

    private final CubismViewMatrix viewMatrix;
    private final CubismMatrix44 deviceToScreen;
}
