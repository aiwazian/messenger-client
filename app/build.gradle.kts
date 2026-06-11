/*
 * Copyright (c) 2026. Aiwazian.
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.gms)
    alias(libs.plugins.serialization)
    
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.aiwazian.messenger"
    compileSdk = 37
    
    defaultConfig {
        applicationId = "com.aiwazian.messenger"
        minSdk = 30
        targetSdk = 37
        versionCode = 26
        versionName = "1.10.0"
    }
    
    buildTypes {
        debug {
            buildConfigField("String", "API_URL", "\"http://10.170.67.101:4000/api/\"")
            buildConfigField("String", "WS_URL", "\"ws://10.170.67.101:4000\"")
        }
        
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            
            buildConfigField("String", "API_URL", "\"https://aiwazian.ru/api/\"")
            buildConfigField("String", "WS_URL", "\"wss://ws.aiwazian.ru\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        viewBinding = true
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
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.navigation.compose)
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
    
    implementation(libs.okhttp)
    
    // Dagger Hilt
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.android)
    implementation(libs.androidx.graphics.shapes)
    implementation(libs.androidx.hilt.navigation.compose)
    
    // Room database
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    
    implementation(libs.material.icons.extended)
    
    implementation(libs.socketio.client) {
        exclude("org.json", "json")
    }
    
    implementation(libs.androidx.core.splashscreen)
    
    implementation(libs.ketch)
    
    implementation(libs.androidx.browser)
    
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.coil.gif)
    
    implementation(libs.pushclient)
    
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.androidx.media3.ui.compose.material3)
    implementation(libs.androidx.media3.session)
    
    // Preview Composable
    debugImplementation(libs.androidx.compose.ui.tooling)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
