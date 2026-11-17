plugins {
    id("kmp-compose-lib")
}

val thePackage = "by.overpas.template.core.design"

kotlin {

    android {
        namespace = thePackage
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.components.resources)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
        }

        commonTest.dependencies {
            implementation(projects.core.uiTest)

            implementation(libs.ultron.compose)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = thePackage
}
