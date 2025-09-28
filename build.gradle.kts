// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
//    repositories {
//        google()
////        jcenter()
//        mavenCentral()
//        maven { url = uri("https://jitpack.io") }
//        maven {url = uri("https://artifact.bytedance.com/repository/Volcengine/")}
//
//    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.3.1")
        classpath("com.google.gms:google-services:4.3.14")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.2")
        classpath("com.google.firebase:perf-plugin:1.4.2")
        classpath("org.greenrobot:greendao-gradle-plugin:3.3.1")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
apply(from = "gradle/versions.gradle.kts")
