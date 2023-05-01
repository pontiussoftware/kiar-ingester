import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.*

plugins {
    id("com.github.node-gradle.node") version "4.0.0"
}

configurations {
    val frontendFiles by creating {
        isCanBeConsumed = true
        isCanBeResolved = false
    }
}


val includeConfig: Boolean by lazy { project.hasProperty("includeConfig") }
node {
    this.version.value("18.16.0")
    this.download.value(true)
    this.workDir.dir("${project.projectDir}/.gradle/nodejs")
    this.yarnWorkDir.dir("${project.projectDir}/.gradle/nodejs")
    this.nodeProjectDir.dir("${project.projectDir}")
}

/**
 * New task to build front-end.
 */
val buildFrontend by tasks.registering(com.github.gradle.node.npm.task.NpxTask::class) {
    outputs.upToDateWhen {
        file("$buildDir/dist").isDirectory
    }
    command.value("@angular/cli@latest")
    args.value(listOf("--configuration=production", "--output-path=build/dist"))
    dependsOn(tasks.npmSetup)
    dependsOn(tasks.npmInstall)
}

/**
 * New task to package front-end.
 */
val packageFrontend by tasks.registering(Copy::class) {
    outputs.upToDateWhen {
        file("$buildDir/lib/kiar-ui.jar").exists()
    }
    destinationDir = file("$buildDir/lib")
    from("$buildDir/dist") {
        println("includeConfig: $includeConfig")
        if (!includeConfig) {
            exclude("**/config.json")
        }
        into("html")
    }
    dependsOn(buildFrontend)
}

artifacts {
    add("frontendFiles", packageFrontend) {
        builtBy(packageFrontend)
        type = "jar"
    }
}