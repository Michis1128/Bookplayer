plugins { alias(libs.plugins.android.library); alias(libs.plugins.ksp); alias(libs.plugins.hilt) }
android {
    namespace = "com.michis.player.playback"; compileSdk = 36; defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
    implementation(project(":domain")); implementation(libs.androidx.core.ktx); implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.media3.common); implementation(libs.androidx.media3.exoplayer); implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.extractor)
    implementation(libs.hilt.android); ksp(libs.hilt.compiler)
}
