rootProject.name = "template"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":androidApp")
include(":desktopApp")
include(":shared")
include(":core:arch")
include(":core:db")
include(":core:design")
include(":core:platform")
include(":core:strings")
include(":core:system")
include(":core:test")
include(":core:ui-test")
include(":core:ui-test:camera")
include(":core:ui-test:gallery")
include(":feature:greeting")
include(":feature:greeting:tags")
include(":feature:greeting:ui-test")
include(":feature:hello")
include(":feature:hello:tags")
include(":feature:hello:ui-test")
