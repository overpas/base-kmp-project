import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("code-coverage")
    id("java-test-fixtures")
    id("static-analysis")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.metro)
}

dependencies {
    implementation(projects.core.platform)
    implementation(projects.core.strings)
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.compose.components.resources)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.decompose)
    implementation(libs.decompose.extensions.compose)
    implementation(libs.essenty.back.handler)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)

    testFixturesImplementation(libs.essenty.back.handler)
    testFixturesImplementation(project.dependencies.compose.desktop.currentOs)

    testFixturesImplementation(libs.compose.ui.test)
    testFixturesImplementation(libs.ultron.compose)
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.junit.jupiter)

            dependencies {
                implementation(projects.core.uiTest)

                implementation(libs.essenty.back.handler)
                implementation(project.dependencies.compose.desktop.currentOs)

                implementation(libs.compose.ui.test)
                implementation(libs.kotlin.test)
                implementation(libs.ultron.compose)
            }
        }

        register<JvmTestSuite>("e2eTest") {
            useJUnitJupiter(libs.versions.junit.jupiter)

            dependencies {
                implementation(project())
                implementation(projects.core.platform)
                implementation(projects.core.uiTest)
                implementation(projects.feature.greeting.uiTest)
                implementation(projects.feature.hello.uiTest)
                implementation(projects.shared)

                implementation(libs.allure.junit5)
                implementation(libs.essenty.back.handler)
                implementation(libs.essenty.lifecycle)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(project.dependencies.compose.desktop.currentOs)
                implementation(testFixtures(project()))

                implementation(libs.compose.ui.test)
                implementation(libs.ultron.compose)
            }

            targets.configureEach {
                testTask.configure {
                    systemProperty(
                        "allure.results.directory",
                        rootProject.layout.projectDirectory
                            .dir(".allure/desktop/results")
                            .asFile
                            .absolutePath,
                    )
                }
            }
        }
    }
}

kover {
    currentProject {
        instrumentation {
            disabledForTestTasks.add("e2eTest")
        }
    }
}

compose.desktop {
    application {
        mainClass = "by.overpas.template.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "by.overpas.template"
            packageVersion = properties["app.version.name"].toString()
        }
    }
}
