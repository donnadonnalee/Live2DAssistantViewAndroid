plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "jp.ds_soft.live2d.library"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        singleVariant("release")
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    
    // Live2D Core (AAR wrapper module)
    implementation(project(":cubism-sdk"))

    // Markdown
    implementation(libs.markwon.core)
    implementation(libs.markwon.html)
}

// JitPack用の公開設定
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.donnadonnalee"
                artifactId = "Live2D-AssistantView-Android"
                version = "1.0.0"
            }
        }
    }
}
