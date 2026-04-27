# Live2D Assistant View for Android 🚀

[![License](https://img.shields.io/badge/license-Live2D%20Open%20Software-blue.svg)](http://live2d.com/eula/live2d-open-software-license-agreement_en.html)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://developer.android.com/)

[English](#english) | [日本語](#japanese)

---

<a name="english"></a>
## English

A highly portable, self-contained Android View component that allows you to integrate a Live2D character as an interactive assistant in minutes.

### ✨ Features
- **📦 Zero-XML Attributes**: Configure everything via Java code. No `attrs.xml` required for porting.
- **💬 Smart Speech Bubble**: Auto-wrapping, bottom-right anchored, and perfectly positioned relative to the character.
- **🗣️ Integrated TTS**: Built-in Android Text-To-Speech support with gender and pitch tuning.
- **🤌 Gesture Support**: Long-press to minimize (shrink to corner), tap to interact.
- **🤖 Auto-Idle**: Automatically plays random motions to keep the character alive.
- **👁️ Visibility Control**: High-level `showAssistant()` / `hideAssistant()` methods for contextual guidance.
- **🎨 Custom Styling**: Programmatically set bubble colors and duration.

### 🚀 Quick Start
```java
Live2DCharacterView live2DView = findViewById(R.id.live2d_view);

// 1. Setup
live2DView.setModelPath("Hiyori");
live2DView.setUseTTS(true);
live2DView.setVoiceGender(Live2DCharacterView.VoiceGender.FEMALE);
live2DView.setPitch(1.3f);

// 2. Interaction
live2DView.say("Hello! I am your AI assistant.", "TapBody", "f01");

// 3. Visibility (e.g. show only in settings)
live2DView.hideAssistant();
live2DView.showAssistant();
```

---

<a name="japanese"></a>
## 日本語

あらゆる Android アプリに、数分で「喋る Live2D アシスタント」を導入するための、ポータビリティを極めたカスタムビューコンポーネントです。

### ✨ 主な機能
- **📦 Java 完結型**: `attrs.xml` 等のリソースファイル不要。Javaファイルのコピーだけで動作します。
- **💬 インテリジェント吹き出し**: 自動改行、右下固定アンカー、キャラクターに重なる最適な配置を自動計算。
- **🗣️ TTS 音声合成内蔵**: Android 標準 TTS を利用した自動読み上げ。性別指定やピッチ調整も可能。
- **🤌 ジェスチャー操作**: 長押しで画面隅に最小化、タップでリアクション。
- **🤖 オートアイドル**: 放置中もランダムなモーションを再生し、キャラクターの生命感を演出。
- **👁️ 表示・非表示制御**: 文脈に合わせてアシスタントを出したり消したりできる `showAssistant()` / `hideAssistant()` メソッド。
- **🎨 柔軟なカスタマイズ**: 吹き出しの色、表示時間、モデル切り替えをすべてコードから制御。

### 🚀 クイックスタート
```java
Live2DCharacterView live2DView = findViewById(R.id.live2d_view);

// 1. セットアップ
live2DView.setModelPath("Hiyori");
live2DView.setUseTTS(true);
live2DView.setVoiceGender(Live2DCharacterView.VoiceGender.FEMALE); // 性別指定
live2DView.setPitch(1.3f); // 声の高さ調整

// 2. 喋らせる
live2DView.say("こんにちは！私があなたのガイドです。", "TapBody", "f01");
```

### 📦 Installation (JitPack)
Add it to your root `settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency to your app `build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.github.donnadonnalee:Live2D-AssistantView-Android:v1.0.2")
}
```

### 🛠️ Porting Guide (Legacy)
If you prefer manual file copying, please refer to the header comments in [Live2DCharacterView.java](library/src/main/java/jp/ds_soft/live2d/Live2DCharacterView.java).

---

<a name="japanese"></a>
## 日本語

### 📦 導入方法 (JitPack)
ルートの `settings.gradle.kts` に以下を追加します：
```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

アプリの `build.gradle.kts` に依存関係を追加します：
```kotlin
dependencies {
    implementation("com.github.donnadonnalee:Live2D-AssistantView-Android:v1.0.0")
}
```

### 🛠️ 手動での移植ガイド
ファイルを直接コピーして使いたい場合は、[Live2DCharacterView.java](library/src/main/java/jp/ds_soft/live2d/Live2DCharacterView.java) 内のヘッダーコメントを参照してください。

---

## 📜 License & Attributions
### Live2D Cubism SDK
This project utilizes the Live2D Cubism SDK.
The SDK code and binaries are provided under the **Live2D Open Software License**.
Copyright (c) Live2D Inc. See [Live2D Official Site](http://www.live2d.com/) for more details.

### Sample Models
The sample models included in this repository (**Hiyori**, **Mark**) are the property of **Live2D Inc.** and are provided under the **Sample Data License** for demonstration and educational purposes.
- **Hiyori**: Copyright (c) Live2D Inc.
- **Mark**: Copyright (c) Live2D Inc.

---
### ⚠️ Disclaimer
This repository is an unofficial community project and is not affiliated with or endorsed by Live2D Inc.
