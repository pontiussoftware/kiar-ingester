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
    dependsOn(tasks.npmInstall)
    command.value("@angular/cli@latest")
    args.value(listOf("build", "--configuration=production", "--output-path=build/dist"))
}

/**
 * New task to package front-end.
 */
val packageFrontend by tasks.registering(Zip::class) {
    dependsOn(buildFrontend)
    outputs.upToDateWhen {
        file("$buildDir/libs/kiar-ui.jar").exists()
    }
    archiveFileName.set("kiar-ui.jar")
    destinationDirectory.set(file("$buildDir/libs"))
    from("$buildDir/dist") {
        into("html")
    }
}

artifacts {
    add("frontendFiles", packageFrontend) {
        builtBy(packageFrontend)
        type = "jar"
    }
}