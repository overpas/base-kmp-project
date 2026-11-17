plugins {
    id("kmp-compose-lib")
}

val thePackage = "by.overpas.template.core.strings"

kotlin {

    android {
        namespace = thePackage
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.components.resources)
            implementation(libs.compose.runtime)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = thePackage
}
