plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)   // <- Asegúrate de tener ESTA línea
    alias(libs.plugins.kotlin.compose)
    // 👇 KAPT sin versión ni alias del catálogo
    id("org.jetbrains.kotlin.kapt")

}

android {
    namespace = "com.gio.guiasclinicas"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gio.guiasclinicas"
        minSdk = 24
        targetSdk = 36
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))

    // Compose core
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // ⬅️ NECESARIO para LazyColumn y stickyHeader
    implementation(libs.androidx.foundation)
    // (opcional) si necesitas más layouts
    // implementation(libs.androidx.foundation.layout)

    // Activity/Navigation
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Lifecycle (unificados)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

}
