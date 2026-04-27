/*
 * ============================================================================
 * 🚀 Live2D AssistantView for Android - rev4 (JitPack Ready)
 * ============================================================================
 * 
 * [A] おすすめの導入方法 (JitPack)
 * --------------------------------
 * build.gradle に以下を追加するだけで導入完了です：
 * implementation("com.github.donnadonnalee:Live2D-AssistantView-Android:LATEST_VERSION")
 * (※最新のバージョン番号は README.md を確認してください)
 * 
 * [B] 手動での移植手順 (ファイルをコピーする場合)
 * ----------------------------------------------
 * 1. Live2DCharacterView.java (このファイル) をコピー
 * 2. jp.ds_soft.live2d.lapp / com.live2d.sdk.cubism.framework パッケージをコピー
 * 3. assets/Shaders と libs/Live2DCubismCore.aar を配置
 * 
 * 3. 基本的な使い方
 * -----------------
 *  1. assets/ フォルダにモデルフォルダを配置 (例: assets/Hiyori/)
 *  2. live2DView = findViewById(R.id.live2d_view);
 *  3. live2DView.setModelPath("Hiyori"); // フォルダ名を指定
 *  4. live2DView.setUseTTS(true);
 *  5. live2DView.say("こんにちは！");
 * 
 *  // ※onResume / onPause での呼び出しを忘れずに！
 * ============================================================================
 */
package jp.ds_soft.live2d;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.Locale;

import jp.ds_soft.live2d.lapp.GLRenderer;
import jp.ds_soft.live2d.lapp.LAppDelegate;
import jp.ds_soft.live2d.lapp.LAppLive2DManager;
import jp.ds_soft.live2d.lapp.LAppModel;
import jp.ds_soft.live2d.lapp.LAppView;

/**
 * Live2Dキャラクターの描画と吹き出しUI、制御ロジックを統合したコンポーネント。
 */
public class Live2DCharacterView extends FrameLayout {
    private GLSurfaceView glSurfaceView;
    private GLRenderer renderer;
    private LAppView view;

    private PopupWindow popupWindow;
    private TextView speechText;
    private FrameLayout bubbleView;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isMinimized = false;
    private GestureDetector gestureDetector;
    private Runnable onTapListener;

    public enum VoiceGender { MALE, FEMALE }
    private VoiceGender targetGender = VoiceGender.FEMALE;

    // TTS
    private TextToSpeech tts;
    private boolean useTTS = false;
    private boolean isTTSReady = false;

    // XML Attributes
    private String modelPathAttr = "Hiyori";
    private int bubbleColorAttr = Color.WHITE;
    private boolean autoIdleAttr = false;

    public Live2DCharacterView(Context context) {
        super(context);
        init(context, null);
    }

    public Live2DCharacterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // XML属性の読み込み (attrs.xmlを使わずに文字列で直接取得する)
        if (attrs != null) {
            String namespace = "http://schemas.android.com/apk/res-auto";
            
            // modelPath
            String mPath = attrs.getAttributeValue(namespace, "modelPath");
            if (mPath != null) modelPathAttr = mPath;

            // bubbleColor (文字列またはリソースIDから変換)
            String bColor = attrs.getAttributeValue(namespace, "bubbleColor");
            if (bColor != null) {
                try {
                    if (bColor.startsWith("#")) {
                        bubbleColorAttr = Color.parseColor(bColor);
                    } else if (bColor.startsWith("@")) {
                        // リソースIDの場合は取得を試みる
                        int resId = attrs.getAttributeResourceValue(namespace, "bubbleColor", 0);
                        if (resId != 0) bubbleColorAttr = context.getResources().getColor(resId, null);
                    }
                } catch (Exception e) {
                    bubbleColorAttr = Color.WHITE;
                }
            }

            // autoIdle
            autoIdleAttr = attrs.getAttributeBooleanValue(namespace, "autoIdle", false);
        }

        // 1. GLSurfaceViewのセットアップ
        glSurfaceView = new GLSurfaceView(context);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        glSurfaceView.setZOrderOnTop(true);

        renderer = new GLRenderer();
        renderer.setModelName(modelPathAttr); // レンダラーに初期モデル名を渡す
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // 2. LAppDelegateの初期化
        LAppDelegate.getInstance().init(context);
        view = new LAppView();
        LAppDelegate.getInstance().setView(view);

        // 3. UIの組み立て
        addView(glSurfaceView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // 4. 吹き出しUIの準備
        setupSpeechBubble(context);

        if (autoIdleAttr) {
            startAutoIdleLoop();
        }

        // TTSの初期化
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.JAPANESE);
                applyVoiceSelection();
                isTTSReady = true;
            }
        });

        // 5. ジェスチャーの準備 (長押しで最小化)
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                toggleMinimize();
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (isMinimized) {
                    toggleMinimize();
                    return true;
                }
                return false;
            }
        });

        glSurfaceView.setOnTouchListener((v, event) -> {
            if (gestureDetector.onTouchEvent(event)) {
                return true;
            }
            if (isMinimized) return true;

            float x = event.getX();
            float y = event.getY();

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                LAppLive2DManager.getInstance().onTap(x, y);
                if (onTapListener != null) onTapListener.run();
            }
            LAppDelegate.getInstance().onTouchBegan(x, y);
            LAppDelegate.getInstance().onTouchMoved(x, y);
            LAppDelegate.getInstance().onTouchEnd(x, y);
            return true;
        });
    }

    private void setupSpeechBubble(Context context) {
        bubbleView = new FrameLayout(context);
        int padding = 16;
        bubbleView.setPadding(padding, padding, padding, padding);

        // 背景をプログラムで生成 (指定された色、黒枠、角丸)
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(bubbleColorAttr);
        shape.setCornerRadius(24f);
        shape.setStroke(4, Color.BLACK);
        bubbleView.setBackground(shape);

        speechText = new TextView(context);
        speechText.setTextColor(Color.BLACK);
        speechText.setTextSize(16f);
        // 吹き出しが広がりすぎないように制限
        speechText.setMaxWidth((int)(context.getResources().getDisplayMetrics().widthPixels * 0.7));
        bubbleView.addView(speechText);

        popupWindow = new PopupWindow(bubbleView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
    }

    /**
     * キャラクターに喋らせる (全引数指定)
     */
    public void say(String text, String motionGroup, String expressionId, int durationMillis) {
        if (speechText != null) {
            speechText.setText(text);
        }

        if (popupWindow != null && !popupWindow.isShowing() && !isMinimized) {
            int[] location = new int[2];
            this.getLocationInWindow(location);
            
            // 吹き出しの最大幅を制限 (画面幅の80%)
            int maxWidth = (int)(getContext().getResources().getDisplayMetrics().widthPixels * 0.8);
            bubbleView.measure(View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST), 
                             View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            
            int bubbleWidth = bubbleView.getMeasuredWidth();
            int bubbleHeight = bubbleView.getMeasuredHeight();

            // 基準点: キャラクターの右下から少し内側 (-10dp程度)
            // 吹き出しの「右下」が常にこの位置に来るように計算する
            int x = location[0] + this.getWidth() - bubbleWidth - 10;
            int y = location[1] + this.getHeight() - bubbleHeight - 10;

            popupWindow.showAtLocation(this, Gravity.NO_GRAVITY, x, y);
            bubbleView.setAlpha(0f);
            bubbleView.animate().alpha(1f).setDuration(300).start();
        }

        LAppLive2DManager manager = LAppLive2DManager.getInstance();
        LAppModel model = manager.getModel(0);
        if (model != null) {
            if (expressionId != null) model.setExpression(expressionId);
            if (motionGroup != null) model.startRandomMotion(motionGroup, 2, null, null);
        }

        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> {
            bubbleView.animate().alpha(0f).setDuration(500).withEndAction(() -> {
                if (popupWindow != null) popupWindow.dismiss();
            }).start();
        }, durationMillis);

        // 音声合成
        if (useTTS && isTTSReady && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "say_" + System.currentTimeMillis());
        }
    }

    public void say(String text) {
        say(text, null, null, 5000);
    }

    public void say(String text, String motionGroup, String expressionId) {
        say(text, motionGroup, expressionId, 5000);
    }

    public boolean isSpeechBubbleVisible() {
        return popupWindow != null && popupWindow.isShowing();
    }

    public void hideSpeechBubble() {
        if (popupWindow != null && popupWindow.isShowing()) {
            bubbleView.animate().alpha(0f).setDuration(500).withEndAction(() -> {
                popupWindow.dismiss();
            }).start();
        }
    }

    public void setOnTapListener(Runnable listener) {
        this.onTapListener = listener;
    }

    private void startAutoIdleLoop() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isMinimized) {
                    LAppLive2DManager manager = LAppLive2DManager.getInstance();
                    LAppModel model = manager.getModel(0);
                    if (model != null && !model.isMotionFinished()) {
                        // モーション再生中でなければランダムに再生
                    } else if (model != null) {
                        model.startRandomMotion("Idle", 1, null, null);
                    }
                }
                handler.postDelayed(this, 5000 + (int)(Math.random() * 5000));
            }
        }, 5000);
    }

    // --- 設定用セッターメソッド ---

    public void setModelPath(final String path) {
        this.modelPathAttr = path;
        if (renderer != null) {
            renderer.setModelName(path);
        }
        if (glSurfaceView != null) {
            glSurfaceView.queueEvent(() -> {
                LAppLive2DManager.getInstance().changeModelByName(path);
            });
        }
    }

    public void setBubbleColor(int color) {
        this.bubbleColorAttr = color;
        setupSpeechBubble(getContext()); // 背景色を再適用
    }

    public void setAutoIdle(boolean enable) {
        this.autoIdleAttr = enable;
        if (enable) startAutoIdleLoop();
    }

    public void setUseTTS(boolean use) {
        this.useTTS = use;
    }

    public void showAssistant() {
        this.setVisibility(View.VISIBLE);
    }

    public void hideAssistant() {
        hideSpeechBubble();
        this.setVisibility(View.GONE);
    }

    public void setPitch(float pitch) {
        if (tts != null) tts.setPitch(pitch);
    }

    public void setSpeechRate(float rate) {
        if (tts != null) tts.setSpeechRate(rate);
    }

    public void setVoiceGender(VoiceGender gender) {
        this.targetGender = gender;
        if (isTTSReady) applyVoiceSelection();
    }

    private void applyVoiceSelection() {
        if (tts == null) return;
        try {
            for (android.speech.tts.Voice voice : tts.getVoices()) {
                String name = voice.getName().toLowerCase();
                if (targetGender == VoiceGender.FEMALE) {
                    if (name.contains("female") || name.contains("woman") || name.contains("soft")) {
                        tts.setVoice(voice);
                        break;
                    }
                } else {
                    if (name.contains("male") || name.contains("man") || name.contains("guy") || name.contains("low")) {
                        tts.setVoice(voice);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // エラー時はデフォルト
        }
    }

    private void toggleMinimize() {
        isMinimized = !isMinimized;
        if (isMinimized) {
            this.animate()
                .scaleX(0.3f).scaleY(0.3f)
                .translationX(this.getWidth() * 0.35f)
                .translationY(this.getHeight() * 0.35f)
                .alpha(0.5f).setDuration(300).start();
            if (popupWindow != null) popupWindow.dismiss();
        } else {
            this.animate()
                .scaleX(1.0f).scaleY(1.0f)
                .translationX(0).translationY(0)
                .alpha(1.0f).setDuration(300).start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    public void onResume() { if (glSurfaceView != null) glSurfaceView.onResume(); }
    public void onPause() { if (glSurfaceView != null) glSurfaceView.onPause(); }
}
