plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.michis.player"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.michis.player"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

dependencies {
    implementation(project(":domain")); implementation(project(":data")); implementation(project(":playback"))
    implementation(project(":core:ui")); implementation(project(":feature:library")); implementation(project(":feature:player"))
    implementation(project(":feature:bookdetails")); implementation(project(":feature:bookmarks")); implementation(project(":feature:settings"))
    implementation(libs.androidx.core.ktx); implementation(libs.androidx.activity.compose); implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(platform(libs.androidx.compose.bom)); implementation(libs.androidx.compose.ui); implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.hilt.android); ksp(libs.hilt.compiler); debugImplementation(libs.androidx.compose.ui.tooling)
}
