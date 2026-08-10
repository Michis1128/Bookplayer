plugins { alias(libs.plugins.android.library); alias(libs.plugins.ksp); alias(libs.plugins.hilt); alias(libs.plugins.room) }
android {
    namespace = "com.michis.player.data"; compileSdk = 36; defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
room { schemaDirectory("$projectDir/schemas") }
dependencies {
    implementation(project(":domain")); implementation(project(":core:common")); implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx); implementation(libs.androidx.datastore.preferences); implementation(libs.hilt.android)
    ksp(libs.androidx.room.compiler); ksp(libs.hilt.compiler); testImplementation(libs.junit)
}
