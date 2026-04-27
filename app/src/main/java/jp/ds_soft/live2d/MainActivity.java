package jp.ds_soft.live2d;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private Live2DCharacterView live2DView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        live2DView = findViewById(R.id.live2d_view);

        // コードから設定を適用 (attrs.xmlを使わないスタイル)
        // ひよりの場合
         live2DView.setModelPath("Hiyori");
         live2DView.setVoiceGender(Live2DCharacterView.VoiceGender.FEMALE);
         live2DView.setPitch(1.3f);
        // マークの場合
        // live2DView.setModelPath("mark_free_jp");
        // live2DView.setVoiceGender(Live2DCharacterView.VoiceGender.MALE);
        // live2DView.setPitch(0.9f);
        live2DView.setBubbleColor(Color.parseColor("#E3F2FD"));
        live2DView.setAutoIdle(true);
        live2DView.setUseTTS(true); // TTSを有効化

        // --- アシスタントのアクション実装 ---

        // 1. 現在時刻を教える
        findViewById(R.id.btn_time).setOnClickListener(v -> {
            String time = new SimpleDateFormat("HH時mm分", Locale.JAPAN).format(new Date());
            live2DView.say("現在は " + time + " ですよ！", "TapBody", "f01");
        });

        // 2. 予定を確認する (デモ用)
        findViewById(R.id.btn_schedule).setOnClickListener(v -> {
            live2DView.say("今日の午後に『Live2Dの動作確認』という予定が入っています。頑張ってくださいね！", "Idle", "f03", 8000);
        });

        // 3. 運勢を占う
        findViewById(R.id.btn_fortune).setOnClickListener(v -> {
            String[] fortunes = { "大吉！最高の一日になりますよ！", "中吉。いいことがありそうです。", "吉。落ち着いて過ごしましょう。", "小吉。一歩ずつ進みましょう。" };
            int rand = (int) (Math.random() * fortunes.length);
            String motion = (rand == 0) ? "TapBody" : "Idle";
            String expression = (rand == 0) ? "f01" : "f02";
            live2DView.say("今日の運勢は..." + fortunes[rand], motion, expression);
        });

        live2DView.setOnTapListener(() -> {
            if (live2DView.isSpeechBubbleVisible()) {
                live2DView.hideSpeechBubble();
            } else {
                // タップするたびに感情付きで喋らせる
                int randomAction = (int) (Math.random() * 3);
                switch (randomAction) {
                    case 0:
                        live2DView.say("こんにちは！今日はいい天気ですね。", "Idle", "f01");
                        break;
                    case 1:
                        live2DView.say("なにか御用でしょうか？", "TapBody", "f03");
                        break;
                    case 2:
                        live2DView.say("あ、あまりジロジロ見ないでください...", "TapBody", "f02");
                        break;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (live2DView != null) {
            live2DView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (live2DView != null) {
            live2DView.onPause();
        }
    }
}