plugins { alias(libs.plugins.android.library) }
android {
    namespace = "ru.mobui.kanji.checker.readability"
    compileSdk = 37
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
dependencies {
    api(project(":domain:practice"))
    implementation(libs.mlkit.ink)
    implementation(libs.kotlinx.coroutines.core)
}
