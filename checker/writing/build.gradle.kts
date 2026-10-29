plugins { alias(libs.plugins.kotlin.jvm) }
kotlin { jvmToolchain(21) }
dependencies {
    api(project(":domain:practice"))
    implementation(project(":checker:strokes"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
