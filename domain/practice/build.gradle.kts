plugins { alias(libs.plugins.kotlin.jvm) }
kotlin { jvmToolchain(21) }
dependencies {
    api(project(":domain:handwriting"))
    testImplementation(libs.junit)
}
