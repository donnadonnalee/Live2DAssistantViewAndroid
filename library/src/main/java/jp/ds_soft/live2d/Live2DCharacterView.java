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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
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
import android.widget.ScrollView;
import android.widget.TextView;
import io.noties.markwon.Markwon;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.html.HtmlPlugin;

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
    private Markwon markwon;

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
        glSurfaceView.setPreserveEGLContextOnPause(true);

        renderer = new GLRenderer();
        renderer.setModelName(modelPathAttr); // レンダラーに初期モデル名を渡す
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // 2. LAppDelegateの初期化
        LAppDelegate.getInstance().init(context);
        view = new LAppView();
        LAppDelegate.getInstance().setView(view);

        // 3. Markdown初期化 (Core + HTMLプラグイン)
        markwon = Markwon.builder(context)
                .usePlugin(CorePlugin.create())
                .usePlugin(HtmlPlugin.create())
                .build();

        // 4. UIの組み立て
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
        // 影と尻尾の分、余白を広く取る (上下左右に影の逃げ道を作る)
        int p = 30;
        bubbleView.setPadding(p, p, p, p + 40);

        // カスタム描画の吹き出し背景を設定
        bubbleView.setBackground(new BubbleDrawable(bubbleColorAttr));

        // 長文対応のためのScrollView
        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        speechText = new TextView(context);
        speechText.setTextColor(Color.BLACK);
        speechText.setTextSize(16f);
        // テキスト自体は左寄せ
        speechText.setGravity(Gravity.START);
        
        scrollView.addView(speechText);
        
        // 吹き出しの幅を画面一杯に設定 (100%)
        int bubbleWidthPx = context.getResources().getDisplayMetrics().widthPixels;
        
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                bubbleWidthPx,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        scrollView.setLayoutParams(scrollParams);
        
        // タップとスクロールを判別するためのジェスチャー検出
        GestureDetector bubbleGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                hideSpeechBubble();
                return true;
            }
        });
        
        scrollView.setOnTouchListener((v, event) -> {
            bubbleGestureDetector.onTouchEvent(event);
            return false; // ScrollViewの本来の挙動(スクロール)を妨げない
        });

        bubbleView.addView(scrollView);

        // 背景部分のタップでも閉じるように設定
        bubbleView.setOnClickListener(v -> hideSpeechBubble());

        popupWindow = new PopupWindow(bubbleView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
    }

    /**
     * キャラクターに喋らせる (全引数指定)
     */
    public void say(String text, String motionGroup, String expressionId, int durationMillis) {
        String displayText = text;
        int targetColor = Color.BLACK;
        boolean hasCustomColor = false;

        // カラーコードの検知 (#RRGGBB 形式)
        if (text.length() >= 8 && text.startsWith("#") && text.substring(1, 7).matches("[0-9a-fA-F]{6}") && Character.isWhitespace(text.charAt(7))) {
            try {
                targetColor = Color.parseColor(text.substring(0, 7));
                displayText = text.substring(8).trim();
                hasCustomColor = true;
            } catch (Exception e) {
                targetColor = Color.BLACK;
            }
        }

        if (speechText != null) {
            if (markwon != null) {
                // 1. タグを一時的なマーカーに置き換える (Markwonによる除去を防ぐため)
                String processed = displayText.replaceAll("<font color=['\"](#?[a-zA-Z0-9]+)['\"]>", "\uE000$1\uE001")
                                             .replaceAll("</font>", "\uE002");

                // 2. Markwonでマークダウンを解析 (render + parse)
                Spannable rendered = (Spannable) markwon.render(markwon.parse(processed));
                SpannableStringBuilder builder = new SpannableStringBuilder(rendered);
                
                // 3. マーカーをスキャンして着色
                // \uE000(color)\uE001(content)\uE002
                while (true) {
                    String current = builder.toString();
                    int startMarker = current.indexOf('\uE000');
                    if (startMarker == -1) break;
                    
                    int midMarker = current.indexOf('\uE001', startMarker);
                    int endMarker = current.indexOf('\uE002', midMarker);
                    if (midMarker == -1 || endMarker == -1) break;
                    
                    String colorStr = current.substring(startMarker + 1, midMarker);
                    try {
                        int color = Color.parseColor(colorStr);
                        // マーカーを取り除きながら着色
                        // 先に中身のテキストを取り出す
                        builder.delete(endMarker, endMarker + 1); // \uE002 除去
                        builder.delete(startMarker, midMarker + 1); // \uE000(color)\uE001 除去
                        
                        int contentStart = startMarker;
                        int contentEnd = endMarker - (midMarker - startMarker + 1);
                        builder.setSpan(new ForegroundColorSpan(color), contentStart, contentEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } catch (Exception e) {
                        // 失敗した場合はマーカーだけ消す
                        builder.delete(endMarker, endMarker + 1);
                        builder.delete(startMarker, midMarker + 1);
                    }
                }
                
                speechText.setText(builder);
                
                // 音声合成用にクリーンなテキストを抽出 (タグやマーカーを除去)
                String ttsText = builder.toString()
                        .replace("\uE000", "")
                        .replace("\uE001", "")
                        .replace("\uE002", "")
                        .replaceAll("<[^>]*>", ""); // 残ったHTMLタグを除去

                if (useTTS && isTTSReady && tts != null) {
                    tts.speak(ttsText, TextToSpeech.QUEUE_FLUSH, null, "say_" + System.currentTimeMillis());
                }
            } else {
                speechText.setText(displayText);
                if (useTTS && isTTSReady && tts != null) {
                    tts.speak(displayText, TextToSpeech.QUEUE_FLUSH, null, "say_" + System.currentTimeMillis());
                }
            }
            // 全体の色設定 (カスタムカラー指定がある場合)
            speechText.setTextColor(targetColor);
        }

        if (popupWindow != null && !popupWindow.isShowing() && !isMinimized) {
            int[] location = new int[2];
            this.getLocationInWindow(location);
            
            // 吹き出しの幅を画面幅一杯に設定
            int screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
            int maxWidth = screenWidth;
            bubbleView.measure(View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.EXACTLY), 
                             View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            
            int bubbleWidth = bubbleView.getMeasuredWidth();
            int bubbleHeight = bubbleView.getMeasuredHeight();

            // 画面の左端 (x=0) から開始して横幅一杯に広げる
            int x = 0;
            // 基準点: キャラクタの頭の上 (ビューの上端) に吹き出しの下端が来るように配置
            int y = location[1] - bubbleHeight - 20;

            popupWindow.setWidth(screenWidth);
            popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
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
        if (durationMillis > 0) {
            handler.postDelayed(() -> {
                if (popupWindow != null) popupWindow.dismiss();
            }, durationMillis);
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
            popupWindow.dismiss();
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

    public void onResume() {
        if (glSurfaceView != null) glSurfaceView.onResume();
        LAppDelegate.getInstance().onResume();
    }
    public void onPause() { if (glSurfaceView != null) glSurfaceView.onPause(); }

    /**
     * 尻尾付きの吹き出しを描画するDrawable
     */
    private static class BubbleDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final int color;

        public BubbleDrawable(int color) {
            this.color = color;
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            // 影の設定 (半径, dx, dy, 色)
            paint.setShadowLayer(15f, 6f, 6f, Color.parseColor("#44000000"));
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float width = bounds.width();
            float height = bounds.height();
            float tailHeight = 30f;
            float tailWidth = 40f;
            float cornerRadius = 24f;
            
            // 尻尾の位置 (右端から80px程度の位置)
            float tailCenterX = width - 80f;

            path.reset();
            // 角丸長方形の本体 (下端は尻尾の高さ分上げる)
            RectF rectF = new RectF(4, 4, width - 4, height - tailHeight - 4);
            path.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW);
            
            // 尻尾 (三角形) を追加
            path.moveTo(tailCenterX - tailWidth / 2, height - tailHeight - 4);
            path.lineTo(tailCenterX, height - 4);
            path.lineTo(tailCenterX + tailWidth / 2, height - tailHeight - 4);
            
            // 塗りつぶし (影と一緒に描画)
            canvas.drawPath(path, paint);
        }

        @Override
        public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) { paint.setColorFilter(colorFilter); }
        @Override
        public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
