import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    /* Kotlin JVM version. */
    kotlin("jvm") version "2.4.20"

    /* Kotlinx serialization plugin. */
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20"

    /* OpenAPI Generator for Frontend internal API generation. */
    id ("org.openapi.generator") version "7.25.0"

    /* Download plugin to load OAS. */
    id ("de.undercouch.download") version "5.7.0"

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
    version = "1.6.0"

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
val oasFile = project.file("doc/oas.json").absolutePath.replace('\\', '/')

openApiGenerate {
    generatorName.set("typescript-angular")
    inputSpec.set(oasFile)
    outputDir.set("${project.projectDir}/kiar-ui/openapi")
    configOptions.set(mapOf(
        "npmName" to "@kiar-openapi/api",
        "ngVersion" to "21.2.23",
        "snapshot" to "true",
        "enumPropertyNaming" to "original"
    ))
}

/**
 * Task to generate OAS. Requires running tool
 */
tasks.register<Download>("generateOAS") {
    val f = project.file(oasFile)
    src(fullOAS)
    dest(f)
}
