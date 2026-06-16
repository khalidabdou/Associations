import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.File

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // alias(libs.plugins.composeHotReload)
    alias(libs.plugins.sqldelight)
    kotlin("plugin.serialization") version "2.0.20"
}

kotlin {
    jvmToolchain(17)

    androidTarget()

    jvm("desktop")

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
            implementation("com.itextpdf:itext7-core:7.2.5")
            implementation("com.itextpdf:html2pdf:4.0.5")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.navigation.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Licensing Dependencies
            implementation(libs.supabase.postgrest)
            implementation(libs.multiplatform.settings)
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.ktor.client.cio)
            implementation("com.russhwolf:multiplatform-settings-no-arg:1.2.0")
            implementation("com.itextpdf:itext7-core:7.2.5")
            implementation("com.itextpdf:html2pdf:4.0.5")
        }
    }
}

android {
    namespace = "org.associations.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.associations.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging { resources { excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/versions/**") } }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies { debugImplementation(compose.uiTooling) }

val appVersion: String = providers.environmentVariable("APP_VERSION")
    .orElse("1.0.11")
    .get()
    .ifEmpty { "1.0.11" }

// Generate a compile-time version constant from the environment variable (set by CI)
// so the runtime APP_VERSION stays in sync with the release tag.
// The directory and task are configured lazily to be configuration-cache compatible.
val generatedVersionDir = project.layout.buildDirectory.dir("generated/version")
val generateVersionConfig = tasks.register("generateVersionConfig") {
    val version = appVersion // capture the string, not a script object
    // This task references project.layout which Gradle cannot serialize for the
    // configuration cache. It runs in under a millisecond, so skipping caching is fine.
    notCompatibleWithConfigurationCache("references project.layout at configuration time")
    outputs.dir(generatedVersionDir)
    doLast {
        val outDir = generatedVersionDir.get().asFile
        outDir.mkdirs()
        outDir.resolve("BuildConfig.kt").writeText("""
package org.associations.project.utils

/**
 * Auto-generated at build time. See build.gradle.kts generateVersionConfig task.
 * Override via APP_VERSION environment variable (set by CI release workflow).
 */
object BuildConfig {
    const val APP_VERSION: String = "$version"
}
""".trimIndent() + "\n")
    }
}

// Wire the generated source directory into the commonMain source sets so it is
// available to every target.
kotlin.sourceSets.named("commonMain") {
    kotlin.srcDir(generatedVersionDir)
}

// Ensure BuildConfig.kt is generated before any Kotlin compilation task runs.
tasks.matching { it.name.startsWith("compileKotlin") }.configureEach {
    dependsOn(generateVersionConfig)
}

compose.desktop {
    application {
        mainClass = "org.associations.project.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Associations"
            packageVersion = appVersion
            description = "Water Association Management System"
            vendor = "Associations Project"
            
            // Include required JVM modules for SQLite JDBC driver
            modules("java.sql", "java.naming", "jdk.unsupported")
            
            // Include all JAR files in the distribution
            includeAllModules = true
            
            windows {
                menuGroup = "Associations"
                shortcut = true
                dirChooser = true
                perUserInstall = true
            }
        }
    }
}

sqldelight {
    databases { create("AppDatabase") { packageName.set("org.associations.project.database") } }
}

val windowsSignPfx = providers.environmentVariable("WINDOWS_SIGN_PFX")
val windowsSignPfxPassword = providers.environmentVariable("WINDOWS_SIGN_PFX_PASSWORD")
val windowsSignTimestampUrl = providers.environmentVariable("WINDOWS_SIGN_TIMESTAMP_URL")
    .orElse("http://timestamp.digicert.com")
val signToolPath = providers.environmentVariable("SIGNTOOL_PATH").orElse("signtool")

tasks.register("signWindowsDist") {
    dependsOn("packageMsi")

    doLast {
        if (!windowsSignPfx.isPresent) {
            logger.lifecycle("WINDOWS_SIGN_PFX is not set; skipping Windows code signing.")
            return@doLast
        }
        if (!windowsSignPfxPassword.isPresent) {
            throw GradleException("WINDOWS_SIGN_PFX_PASSWORD is not set; cannot sign Windows artifacts.")
        }

        val pfxFile = File(windowsSignPfx.get())
        if (!pfxFile.exists()) {
            throw GradleException("Signing certificate not found at: ${pfxFile.absolutePath}")
        }

        val artifacts = fileTree(buildDir).matching {
            include("**/*.msi")
            include("**/*.exe")
        }.files

        if (artifacts.isEmpty()) {
            throw GradleException("No .msi/.exe artifacts found under: ${buildDir.absolutePath}")
        }

        artifacts.forEach { artifact ->
            exec {
                commandLine(
                    signToolPath.get(),
                    "sign",
                    "/f",
                    pfxFile.absolutePath,
                    "/p",
                    windowsSignPfxPassword.get(),
                    "/fd",
                    "sha256",
                    "/tr",
                    windowsSignTimestampUrl.get(),
                    "/td",
                    "sha256",
                    artifact.absolutePath
                )
            }
        }
    }
}
