plugins {
    id("com.github.node-gradle.node") version "7.1.0"
}

configurations {
    create("frontendFiles") {
        isCanBeConsumed = true
        isCanBeResolved = false
    }
}


val includeConfig: Boolean by lazy { project.hasProperty("includeConfig") }

node {
    this.version.value("26.9.0")
    this.download.value(true)
    this.workDir.dir("${project.projectDir}/.gradle/nodejs")
    this.yarnWorkDir.dir("${project.projectDir}/.gradle/nodejs")
    this.nodeProjectDir.dir("${project.projectDir}")
}

/**
 * New task to build front-end.
 */
val buildFrontend = tasks.register<com.github.gradle.node.npm.task.NpxTask>("buildFrontend") {
    dependsOn(tasks.npmInstall)
    command.value("@angular/cli@20")
    args.value(listOf("build", "--configuration=production", "--output-path=build/dist"))
}

/**
 * New task to package front-end.
 */
val packageFrontend = tasks.register<Zip>("packageFrontend") {
    dependsOn(buildFrontend)
    archiveFileName.set("kiar-ui.jar")
    destinationDirectory.set(project.layout.buildDirectory.dir("libs").get())
    from(project.layout.buildDirectory.dir("dist").get()) {
        into("html")
    }
}

artifacts {
    add("frontendFiles", packageFrontend) {
        builtBy(packageFrontend)
        type = "jar"
    }
}