plugins {
    id("com.android.application") version "8.6.1"
    id("org.jetbrains.kotlin.android") version "1.9.24"
}

android {
    namespace = "com.verticaos.games"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.verticaos.games"
        minSdk = 21            // Reduzido para 21 (Mínimo absoluto para o Compose rodar na MXQ)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    // Adicionada a configuração clássica do Compose, compatível com a versão do Kotlin gerada
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-process:2.8.6")
    implementation("io.coil-kt:coil-compose:2.7.0")
}