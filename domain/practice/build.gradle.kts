plugins { alias(libs.plugins.kotlin.jvm) }
kotlin { jvmToolchain(21) }
dependencies {
    api(project(":domain:handwriting"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
