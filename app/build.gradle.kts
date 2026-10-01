plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.playvid"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.playvid"
        minSdk = 26
        targetSdk = 35
        versionCode = 37
        versionName = "3.5.0"
        
        resourceConfigurations += setOf("en")
    }

    splits {
        abi {
            isEnable = true
            reset()
            // Agar testing sirf mobile phone (Redmi 9 Prime wagera) pe karni hai, 
            // toh x86/x86_64 emulator splits CI mein build hone se bacha sakte hain.
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = false
        }
    }

    signingConfigs {
        create("release") {
            val path = System.getenv("KEYSTORE_PATH")
            if (!path.isNullOrBlank()) {
                storeFile = file(path)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (!System.getenv("KEYSTORE_PATH").isNullOrBlank()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-opt-in=androidx.media3.common.util.UnstableApi"
    }

    buildFeatures {
        compose = true
    }

    lint {
        checkReleaseBuilds = false
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/INDEX.LIST",
                "META-INF/*.version",
                "META-INF/NOTICE*",
                "META-INF/LICENSE*",
                "DebugProbesKt.bin"
            )
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    
    // NOTE: Agar 'compose.icons' extended bundle hai, toh sirf zaroori icons use karein 
    // ya core icons use karein kyunki extended icons akele 30MB+ DEX badha deta hai.
    implementation(libs.compose.icons)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)

    // Media3 Core Playback
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.hls)
    implementation(libs.media3.dash)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation(libs.media3.okhttp)
    
    // NOTE: Agar video transcoding / video editing nahi kar rahe, toh 'transformer' 
    // aur 'effect' ko remove/comment kar de, isse 10-15 MB size kam hoga:
    // implementation(libs.media3.transformer)
    // implementation(libs.media3.effect)

    implementation(libs.androidx.media)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore)

    // NewPipeExtractor & Desugaring
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.5")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")
}
