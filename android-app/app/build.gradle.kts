plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.promptfilm.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.promptfilm.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:router"))
    implementation(project(":core:ui"))
    implementation(project(":feature:login"))
    implementation(project(":feature:payment"))
    implementation(libs.androidx.activity.ktx)
}
