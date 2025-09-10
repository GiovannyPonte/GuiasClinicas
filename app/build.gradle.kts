plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)   // módulo: sin "version" y sin "apply false"
}


android {
    namespace = "com.gio.guiasclinicas"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gio.guiasclinicas"
        minSdk = 24
        targetSdk = 36
        versionCode = 110000
        versionName = "11.0.0"

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
        // Mantengo Java 11 como en tu local
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
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.google.material)

    // Compose BOM

    // Compose core
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // NECESARIO para LazyColumn y stickyHeader
    implementation(libs.androidx.foundation)
    // (opcional) si necesitas más layouts:
    // implementation(libs.androidx.foundation.layout)

    // Activity / Navigation
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Lifecycle (unificados, usando catálogo)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // Room (añadido desde Codex, con KSP)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)




    // BOM de Compose: usa el alias de librería "compose-bom"

        implementation(platform(libs.compose.bom))   // ← este alias de librería NO cambia
        // ...


    // Material3 (Compose) si lo usas por alias del catálogo
    implementation(libs.androidx.material3)

    // Activity Compose, Navigation, etc. (según tus alias)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Íconos extendidos (usa el alias que dejaste vivo en el TOML)
    implementation(libs.material.icons.extended)
    // o, si elegiste el alias largo:
    // implementation(libs.androidx.material.icons.extended)

    // (Opcional) Material Components (Views) para Theme.Material3.* en XML
    implementation(libs.google.material)

    // Compose BoM (controla versiones de todo Compose)
    implementation(platform(libs.compose.bom))

    // UI base
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.material3)
    implementation(libs.material.icons.extended)

    // Activity + Lifecycle para integrar Compose de forma estándar
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Kotlin Serialization (para parsear el JSON del algoritmo)
    implementation(libs.kotlinx.serialization.json)

    // Opcional pero útil en desarrollo / previews
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Test básico
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

