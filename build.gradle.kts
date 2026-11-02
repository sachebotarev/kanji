// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    base
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

// Run the stable CLI independently of AGP/Kotlin plugin internals.
val detektCli = configurations.create("detektCli") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    add(detektCli.name, libs.detekt.cli)
}

val detektSourceDirectories = subprojects
    .filter { it.buildFile.isFile }
    .map { it.layout.projectDirectory.dir("src").asFile }
val detektSources = files(detektSourceDirectories).asFileTree.matching {
    include("**/*.kt", "**/*.kts")
}
val detektConfig = layout.projectDirectory.file("config/detekt/detekt.yml")
val detektBaseline = layout.projectDirectory.file("config/detekt/baseline.xml")
val detektReports = layout.buildDirectory.dir("reports/detekt")
val detektArguments = listOf(
    "--input", detektSourceDirectories.joinToString(","),
    "--config", detektConfig.asFile.absolutePath,
    "--base-path", layout.projectDirectory.asFile.absolutePath,
    "--build-upon-default-config",
)

tasks.register<JavaExec>("detekt") {
    group = "verification"
    description = "Analyzes Kotlin sources in all modules; fails on findings outside the baseline."
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    classpath = detektCli
    inputs.files(detektSources).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(detektConfig).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(detektBaseline).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(detektReports)
    args(detektArguments)
    args("--baseline", detektBaseline.asFile.absolutePath)
    args(
        "--report", "html:${detektReports.get().file("detekt.html").asFile}",
        "--report", "sarif:${detektReports.get().file("detekt.sarif").asFile}",
        "--report", "xml:${detektReports.get().file("detekt.xml").asFile}",
    )
}

tasks.register<JavaExec>("detektAll") {
    group = "verification"
    description = "Reports all findings, including existing debt, without applying the baseline."
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    classpath = detektCli
    inputs.files(detektSources).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(detektConfig).withPathSensitivity(PathSensitivity.RELATIVE)
    val reports = layout.buildDirectory.dir("reports/detekt-all")
    outputs.dir(reports)
    args(detektArguments)
    args(
        "--report", "html:${reports.get().file("detekt.html").asFile}",
        "--report", "sarif:${reports.get().file("detekt.sarif").asFile}",
        "--report", "xml:${reports.get().file("detekt.xml").asFile}",
    )
}

tasks.register<JavaExec>("detektBaseline") {
    group = "verification"
    description = "Explicitly regenerates the reviewed baseline of existing detekt findings."
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    classpath = detektCli
    inputs.files(detektSources).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(detektConfig).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(detektBaseline)
    outputs.upToDateWhen { false }
    args(detektArguments)
    args("--baseline", detektBaseline.asFile.absolutePath, "--create-baseline")
}

tasks.named("check") {
    dependsOn("detekt")
}

subprojects {
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(rootProject.tasks.named("detekt"))
    }
}
