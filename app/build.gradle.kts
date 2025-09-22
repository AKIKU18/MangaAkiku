plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.mangav5"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mangav5"
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // (Optional) Gson for JSON parsing if you want
    implementation ("com.google.code.gson:gson:2.9.0")
    implementation ("androidx.recyclerview:recyclerview:1.3.0")
    implementation ("com.squareup.picasso:picasso:2.8") // For image loading
    implementation("androidx.room:room-runtime:2.5.2")
    annotationProcessor("androidx.room:room-compiler:2.5.2")
// For Kotlin use kapt instead of annotationProcessor
    implementation("androidx.room:room-rxjava2:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation ("androidx.cardview:cardview:1.0.0")

}