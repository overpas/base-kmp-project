import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    id("org.jetbrains.kotlinx.kover")
}

val noTestCoverageModules = setOf("tags", "ui-test")
val noTestCoveragePaths = setOf(":core:ui-test:camera", ":core:ui-test:gallery")

dependencies {
    subprojects
        .filter { it.buildFile.exists() }
        .filterNot { it.name in noTestCoverageModules }
        .filterNot { it.path in noTestCoveragePaths }
        .forEach { kover(it) }
}

kover {
    reports {
        filters {
            excludes {
                androidGeneratedClasses()
                packages("*.di")
                classes(
                    "by.overpas.template.e2e.*",
                    "*.Res",
                    "*.Res\$*",
                    "*.String0*",
                    "*ResourceCollectors*",
                    "by.overpas.template.core.db.AppDatabase",
                    "by.overpas.template.core.db.AppDatabase\$*",
                    "by.overpas.template.core.db.Item",
                    "by.overpas.template.core.db.ItemQueries",
                    "by.overpas.template.core.db.ItemQueries\$*",
                    "by.overpas.template.core.db.db.*",
                    "*.ComposableSingletons\$*",
                    "*.Preview*Component",
                    "*.generated.resources.*",
                    "*\$MetroFactory",
                    "*\$MetroFactory\$*",
                    "*\$MetroContribution*",
                    "*\$BindsMirror",
                    "*\$DefaultImpls",
                )
            }
        }
        total {
            html {
                title = "template coverage"
            }
            verify {
                rule {
                    minBound(80, CoverageUnit.LINE)
                }
            }
        }
    }
}
