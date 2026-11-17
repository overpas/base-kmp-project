import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.dsl.Lint
import dev.detekt.gradle.Detekt
import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("dev.detekt")
}

val libs = the<LibrariesForLibs>()

detekt {
    parallel = true
    buildUponDefaultConfig = true
    basePath = rootProject.layout.projectDirectory
    config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
    source.setFrom(layout.projectDirectory.dir("src"), layout.projectDirectory.file("build.gradle.kts"))
}

dependencies {
    detektPlugins(fileTree(rootProject.layout.projectDirectory.dir("config/detekt/plugins")) { include("*.jar") })

    detektPlugins(libs.detekt.compose.rules)
    detektPlugins(libs.detekt.intellij.inspections.android.all)
    detektPlugins(libs.detekt.intellij.inspections.compose.multiplatform.preview)
    detektPlugins(libs.detekt.intellij.inspections.gradle.best.practices)
    detektPlugins(libs.detekt.intellij.inspections.jvm.all)
    detektPlugins(libs.detekt.intellij.inspections.kotlin.all)
    detektPlugins(libs.detekt.ktlint.wrapper)
}

tasks.withType<Detekt>().configureEach {
    autoCorrect = true
    reports {
        html.required = true
        markdown.required = true
        sarif.required = false
        checkstyle.required = false
    }
}

val typeResolutionTasks = tasks.withType<Detekt>().matching {
    it.name != "detekt" && !it.name.endsWith("SourceSet")
}

typeResolutionTasks.configureEach {
    val buildDir = layout.buildDirectory.get().asFile
    autoCorrect = false
    buildUponDefaultConfig = false
    config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt-type-resolution.yml"))
    exclude { it.file.startsWith(buildDir) }
}

tasks.named("detekt") {
    dependsOn(typeResolutionTasks)
}

val lintConfigFile = rootProject.layout.projectDirectory.file("config/lint/lint.xml").asFile

val allLintChecks: Lint.() -> Unit = {
    checkAllWarnings = true
    warningsAsErrors = true
    abortOnError = true
    checkTestSources = true
    explainIssues = true
    lintConfig = lintConfigFile
}

plugins.withId("com.android.application") {
    extensions.configure<ApplicationExtension> {
        lint(allLintChecks)
    }
}

plugins.withId("com.android.kotlin.multiplatform.library") {
    apply(plugin = "com.android.lint")
    extensions.configure<Lint>(allLintChecks)
    extensions.configure<KotlinMultiplatformExtension> {
        extensions.configure<KotlinMultiplatformAndroidLibraryExtension> {
            lint(allLintChecks)
        }
    }
}
