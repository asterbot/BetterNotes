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
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)

    val voyagerVersion = "1.1.0-beta02"
    implementation("cafe.adriel.voyager:voyager-navigator:$voyagerVersion")

    implementation("org.jetbrains:markdown-jvm:0.7.3")
    implementation(compose.material3)

    // FDG layout
    implementation(project(":fdg_layout"))

    // Kotlin coroutine dependency
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.4")

    // MongoDB Kotlin driver dependency
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:4.11.1")

    implementation("org.mongodb:bson-kotlinx:4.10.1")
    // For logging from Mongo's side
//    implementation("org.slf4j:slf4j-simple:2.0.7")

    // DotEnv file reader
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")

    // JSON serialization library
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.0")

    // KMath libraries
    implementation("space.kscience:kmath-ast:0.4.2")

    // Scilab
    implementation("org.scilab.forge:jlatexmath:1.0.7")
    implementation("org.jetbrains.compose.ui:ui-graphics-desktop:1.5.0")

    implementation("org.scilab.forge:jlatexmath:1.0.7")

    // File Picker
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("io.github.vinceglb:filekit-core:0.10.0-beta01")
    implementation("io.github.vinceglb:filekit-coil:0.10.0-beta01")
    implementation("io.github.vinceglb:filekit-dialogs:0.10.0-beta01")
    implementation("io.github.vinceglb:filekit-dialogs-compose:0.10.0-beta01")
    implementation("org.mindrot:jbcrypt:0.4")

    // Additional icons
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")

    // color picker
    implementation("com.github.skydoves:colorpicker-compose:1.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

configurations.all {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            includeAllModules = true
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "cs-346-project"
            packageVersion = "1.0.0"
            // If using JVM target and Linux distribution
            linux {
                modules("jdk.security.auth")
                iconFile.set(project.file("src/main/resources/betternotes_logo.png"))

            }
            macOS {
                iconFile.set(project.file("src/main/resources/betternotes_logo.icns"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/betternotes_logo.png"))
            }
        }
    }
}
