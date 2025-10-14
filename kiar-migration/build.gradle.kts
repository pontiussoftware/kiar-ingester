val javalinVersion: String by project
val xodusVersion: String by project
val xodusDnqVersion: String by project

plugins {
    kotlin("jvm")
}

group = "ch.pontius.custodian"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":kiar-ingest"))
    /** Javalin + Open API. */
    implementation("io.javalin:javalin:${javalinVersion}")

    /** SQLite + Kotlin Exposed */
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
    implementation("org.jetbrains.exposed:exposed-core:1.0.0-rc-2")
    implementation("org.jetbrains.exposed:exposed-jdbc:1.0.0-rc-2")
    implementation("org.jetbrains.exposed:exposed-java-time:1.0.0-rc-2")
    implementation("org.jetbrains.exposed:exposed-json:1.0.0-rc-2")

    /** Xodus & Xodus DNQ */
    implementation("org.jetbrains.xodus:xodus-openAPI:${xodusVersion}")
    implementation("org.jetbrains.xodus:xodus-environment:${xodusVersion}")
    implementation("org.jetbrains.xodus:xodus-entity-store:${xodusVersion}")
    implementation("org.jetbrains.xodus:xodus-vfs:${xodusVersion}")
    implementation("org.jetbrains.xodus:dnq:${xodusDnqVersion}")
}

tasks.test {
    useJUnitPlatform()
}