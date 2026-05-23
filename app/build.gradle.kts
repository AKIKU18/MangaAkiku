plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.mangav5"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.mangav5"
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "1.0.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }

    signingConfigs {
        create("release") {
            // Use your old Windows keystore
            storeFile = file("/home/akiku/Desktop/DebugKey/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("release") // pentru update direct de pe telefon
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // OkHttp for network requests
    implementation("com.squareup.okhttp3:okhttp:5.3.2")

    // (Optional) Gson for JSON parsing if you want
    implementation ("com.google.code.gson:gson:2.14.0")
    implementation ("androidx.recyclerview:recyclerview:1.3.0")
    implementation ("com.squareup.picasso:picasso:2.71828") // For image loading
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.5.2")
    annotationProcessor("androidx.room:room-compiler:2.8.4")
// For Kotlin use kapt instead of annotationProcessor
    implementation("androidx.room:room-rxjava2:2.8.4")
    implementation ("com.github.bumptech.glide:glide:5.0.7")
    annotationProcessor("com.github.bumptech.glide:compiler:5.0.7")
    implementation("com.github.chrisbanes:PhotoView:2.3.0") {
        exclude(group = "com.android.support")
    }
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("com.github.Dimezis:BlurView:version-3.1.0")
    implementation ("org.jsoup:jsoup:1.22.2")
    implementation ("com.github.javiersantos:AppUpdater:2.7") {
        exclude(group = "com.android.support")
    }
    implementation("com.github.bumptech.glide:okhttp3-integration:5.0.7")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:5.3.2")
}