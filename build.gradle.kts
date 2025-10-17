import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    /* Kotlin JVM version. */
    kotlin("jvm") version "2.2.20"

    /* Kotlinx serialization plugin. */
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20"

    /* OpenAPI Generator for Frontend internal API generation. */
    id ("org.openapi.generator") version "7.16.0"

    /* Download plugin to load OAS. */
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
    version = "1.2.0"

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

/**
 * Task to generate OAS. Requires running tool
 */
tasks.register<Download>("generateOAS") {
    val f = project.file(oasFile)
    src(fullOAS)
    dest(f)
}

/**
 * Task to run database migration. Requires running tool
 */
tasks.register<JavaExec>("runMigration") {
    group = "application"
    description = "Run the database migration logic (Xodus > SQLite) with two custom arguments."

    // use the same classpath that the standard `run` task uses
    val migration = project(":kiar-migration")
    classpath = migration.sourceSets["main"].runtimeClasspath
    mainClass = "ch.pontius.kiar.migration.MainKt"

    // read arguments from project properties (e.g. -PfirstArg=foo -PsecondArg=bar)
    val source: String by project
    val destination: String by project
    args(source, destination)

    // optional: fail fast if the user forgets to provide the arguments
    doFirst {
        if (!project.hasProperty("source") || !project.hasProperty("destination")) {
            throw GradleException("Both `source` and `destination` must be supplied, e.g. -source=foo -destination=bar")
        }
    }
}
