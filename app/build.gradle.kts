/*
 * Copyright (c) 2026. Aiwazian.
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.serialization)
    alias(libs.plugins.google.services)
    
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("androidx.room3")
}

android {
    namespace = "com.aiwazian.messenger"
    compileSdk = 37
    
    defaultConfig {
        applicationId = "com.aiwazian.messenger"
        minSdk = 30
        targetSdk = 37
        versionCode = 50
        versionName = "1.18.2"
    }
    
    buildTypes {
        debug {
            versionNameSuffix = "-debug"
            
            buildConfigField("String", "API_URL", "\"http://192.168.0.134:4000/api/\"")
            buildConfigField("String", "WS_URL", "\"ws://192.168.0.134:4000\"")
            buildConfigField("String", "AD_BANNER_ID", "\"demo-banner-yandex\"")
        }
        
        release {
            optimization {
                enable = true
            }
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            buildConfigField("String", "API_URL", "\"https://aiwazian.ru/api/\"")
            buildConfigField("String", "WS_URL", "\"wss://ws.aiwazian.ru\"")
            buildConfigField("String", "AD_BANNER_ID", "\"R-M-15520718-1\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        viewBinding = false
        buildConfig = true
    }
    buildToolsVersion = "36.0.0"
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    compileSdkMinor = 0
}

dependencies {
    implementation(libs.mobileads)
    implementation(libs.mobileads.compose)
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended.android)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // Biometric
    implementation(libs.androidx.biometric)
    
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.navigation.material)
    implementation(libs.accompanist.navigation.animation)
    
    implementation(libs.protobuf.javalite)
    
    implementation(libs.retrofit)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    
    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    
    // Lottie animation
    implementation(libs.lottie.compose)
    
    implementation(libs.material.kolor)
    
    implementation(libs.okhttp)
    
    // Dagger Hilt
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.android)
    implementation(libs.androidx.graphics.shapes)
    implementation(libs.androidx.hilt.navigation.compose)
    
    // Room database
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    
    implementation(libs.socketio.client) {
        exclude("org.json", "json")
    }
    
    implementation(libs.androidx.core.splashscreen)
    
    implementation(libs.ketch)
    
    implementation(libs.androidx.browser)
    
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.coil.gif)
    
    // Firebase Cloud Messaging & Analytics
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.androidx.media3.ui.compose.material3)
    implementation(libs.androidx.media3.session)
    
    // Preview Composable
    debugImplementation(libs.androidx.compose.ui.tooling)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}
