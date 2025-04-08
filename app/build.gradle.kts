plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.gichehafarm.registry"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gichehafarm.registry"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        viewBinding = true
        compose =  true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.01")) // Replace with the latest compatible BOM version

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.room:room-runtime:2.5.0")
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation ("androidx.viewpager2:viewpager2:1.0.0")
    implementation ("com.google.android.material:material:1.9.0")
        implementation("androidx.compose.compiler:compiler:1.5.10")
    implementation ("androidx.recyclerview:recyclerview:1.3.1")
    implementation ("androidx.fragment:fragment-ktx:1.8.0")
    implementation ("androidx.core:core-splashscreen:1.0.1")
    implementation ("androidx.work:work-runtime-ktx:2.8.1")
    implementation ("androidx.compose.material:material-icons-extended:1.5.0")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("androidx.compose.ui:ui:1.7.8")
    implementation ("androidx.compose.foundation:foundation:1.7.8")
    implementation ("androidx.work:work-runtime-ktx:2.8.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
      implementation ("com.jakewharton.timber:timber:5.0.1")

    //hashing dependency
    implementation("org.mindrot:jbcrypt:0.4")
    implementation(libs.firebase.crashlytics)
    kapt("androidx.room:room-compiler:2.5.0")
}
