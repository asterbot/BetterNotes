import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization") version "1.9.0"
}

group = "ca.uwaterloo"
version = "0.10"

repositories {
    maven("https://repo.kotlin.link")
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.common)
    testImplementation(kotlin("test"))
    implementation(compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "FdgLayoutModuleKt"
        nativeDistributions {
            includeAllModules = true
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "cs-346-project"
            packageVersion = "1.0.0"
            // If using JVM target and Linux distribution
            linux {
                modules("jdk.security.auth")
            }
        }
    }
}
