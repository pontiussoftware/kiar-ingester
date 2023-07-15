
plugins {
    /* Kotlin JVM version. */
    kotlin("jvm") version "1.8.21"

    /* Kotlinx serialization plugin. */
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.21"

    /* OpenAPI Generator for Frontend internal API generation */
    id ("org.openapi.generator") version "5.2.0"

    id ("de.undercouch.download") version "5.4.0"

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
    version = "1.0.0-SNAPSHOT"

    dependencies {
        /** UnitTest dependencies. */
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:1.9.2")
    }

    tasks {
        compileKotlin {
            kotlinOptions {
                jvmTarget = "11"
            }
        }

        compileTestKotlin {
            kotlinOptions {
                jvmTarget = "11"
            }
        }

        test {
            useJUnitPlatform()
        }
    }
}


val fullOAS = "http://localhost:7070/swagger-docs"
val oasFile = "${project.projectDir}/doc/oas.json"
val outputDir = "${project.projectDir}/kiar-ui/openapi"

tasks.register("generateOpenApi") {
    doLast {
        val configOptions = mapOf(
            "npmName" to "@kiar-openapi/api",
            "ngVersion" to "16.0.2",
            "snapshot" to "true", /// I suggest to remove this, as soon as we automate this,
            "enumPropertyNaming" to "original"
        )

        exec {
            commandLine(
                "openapi-generator-cli",
                "generate",
                "-g",
                "typescript-angular",
                "-i",
                oasFile,
                "-o",
                outputDir,
                "--skip-validate-spec",
                "--additional-properties",
                configOptions.entries.joinToString(",") { "${it.key}=${it.value}" }
            )
            standardOutput = System.out
            errorOutput = System.err
        }
    }
}

val generateOAS by tasks.registering(org.jetbrains.kotlin.de.undercouch.gradle.tasks.download.Download::class) {
    /* Requires DRES running on default port */
    val f = project.file(oasFile)
    src(fullOAS)
    dest(f)
}