import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    /* Kotlin JVM version. */
    kotlin("jvm") version "2.2.20"

    /* Kotlinx serialization plugin. */
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20"

    /* OpenAPI Generator for Frontend internal API generation */
    id ("org.openapi.generator") version "7.16.0"

    id ("de.undercouch.download") version "5.6.0"

    idea
}

allprojects {
    /* Repositories for build script. */
    buildscript {
        repositories {
            mavenCentral()
        }
    }

    /* Project repositories for build script. */
    repositories {
        mavenCentral()
    }

}

subprojects {
    /* All subprojects are Kotlin projects. */
    apply {
        plugin("kotlin")
        plugin("application")
        plugin("idea")
        plugin("org.jetbrains.kotlin.plugin.serialization")
    }

    /* Group name of our artifacts */
    group = "ch.pontius.kiar"

    /* Our current version, on dev branch this should always be release+1-SNAPSHOT */
    version = "1.1.0"

    tasks {
        compileKotlin {
            compilerOptions {
                compilerOptions.jvmTarget = JvmTarget.JVM_21
            }
        }

        compileTestKotlin {
            compilerOptions {
                compilerOptions.jvmTarget = JvmTarget.JVM_21
            }
        }

        test {
            useJUnitPlatform()
        }
    }
}


val fullOAS = "http://localhost:7070/swagger-docs"
val oasFile = "${project.projectDir}/doc/oas.json"

openApiGenerate {
    generatorName.set("typescript-angular")
    inputSpec.set(oasFile)
    outputDir.set("${project.projectDir}/kiar-ui/openapi")
    configOptions.set(mapOf(
        "npmName" to "@kiar-openapi/api",
        "ngVersion" to "20.3.0",
        "snapshot" to "true",
        "enumPropertyNaming" to "original"
    ))
}

val generateOAS by tasks.registering(Download::class) {
    /* Requires KIAR running on default port */
    val f = project.file(oasFile)
    src(fullOAS)
    dest(f)
}