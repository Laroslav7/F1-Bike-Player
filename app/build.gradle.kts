plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.minus81"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.minus81"
        minSdk = 31
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
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Навигация
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Иконки
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // Хранение данных (DataStore для пароля, Room для ссылок)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coil (для загрузки картинок)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Браузер (для открытия ссылок внутри приложения)
    implementation("androidx.browser:browser:1.7.0")
}