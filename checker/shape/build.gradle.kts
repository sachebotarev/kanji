import org.gradle.api.tasks.testing.Test

plugins { alias(libs.plugins.kotlin.jvm) }
kotlin { jvmToolchain(21) }
dependencies {
    api(project(":domain:practice"))
    implementation(project(":checker:strokes"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}

// Corpus probes read environment-selected fixtures and write reports outside build/.
// They must execute for every requested measurement, including with configuration cache enabled.
tasks.withType<Test>().configureEach {
    val probeFlags = listOf("KANJI_CALIBRATION", "KANJI_FRONTIER", "KANJI_HOOK_DIAGNOSTICS")
    val corpusProbe = probeFlags.any { providers.environmentVariable(it).orNull == "1" }
    val environmentInputs = probeFlags + listOf(
        "KANJI_INPUTS", "KANJI_RESULTS", "KANJI_TOLERANCES", "KANJI_MATCH_PERCENT", "KANJI_BUDGET_PERCENT",
    )
    environmentInputs.forEach { name ->
        inputs.property(name, providers.environmentVariable(name).orElse(""))
    }
    outputs.upToDateWhen { !corpusProbe }
    outputs.doNotCacheIf("Corpus probes produce external diagnostic reports") { corpusProbe }
}
