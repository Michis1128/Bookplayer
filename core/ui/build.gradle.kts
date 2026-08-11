plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.compose) }
android {
    namespace = "com.michis.player.core.ui"; compileSdk = 36; defaultConfig { minSdk = 26 }; buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
    implementation(project(":domain"))
    implementation(platform(libs.androidx.compose.bom)); implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3); implementation(libs.androidx.compose.ui.tooling.preview)
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
