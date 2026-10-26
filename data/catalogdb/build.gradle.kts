plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}
android {
    namespace = "ru.mobui.kanji.data.catalog"
    compileSdk = 37
    defaultConfig { minSdk = 24; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}
dependencies {
    api(project(":domain:catalog"))
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)

}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.androidTest?.sources?.assets?.addStaticSourceDirectory(rootProject.file("data/catalog").path)
        variant.androidTest?.sources?.assets?.addStaticSourceDirectory(project.file("schemas").path)
    }
}
