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
    compileSdk = 36
    
    defaultConfig {
        applicationId = "com.aiwazian.messenger"
        minSdk = 30
        targetSdk = 36
        versionCode = 13
        versionName = "1.6.1"
    }
    
    buildTypes {
        debug {
            buildConfigField(
                "String",
                "API_URL",
                "\"http://10.155.204.101:3000/api/\""
            )
            
            buildConfigField(
                "String",
                "WS_URL",
                "\"ws://10.155.204.101:3000\""
            )
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
            
            buildConfigField(
                "String",
                "API_URL",
                "\"https://aiwazian.ru/api/\""
            )
            
            buildConfigField(
                "String",
                "WS_URL",
                "\"wss://aiwazian.ru\""
            )
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
}

dependencies {
    implementation(libs.mobileads)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
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
    
    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    
    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    
    implementation(libs.coil.compose)
    
    // Lottie animation
    implementation(libs.lottie.compose)
    
    implementation(libs.okhttp)
    
    // Dagger Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.graphics.shapes)
    debugImplementation(libs.androidx.compose.ui.tooling)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    
    // Room database
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    
    implementation(libs.material.icons.extended)
    
    implementation(libs.socketio.client) {
        exclude(
            "org.json",
            "json"
        )
    }
    
    implementation(libs.androidx.core.splashscreen)
    
    implementation(libs.ketch)
}