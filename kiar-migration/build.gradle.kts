val exposedVersion: String by project
val javalinVersion: String by project
val sqliteVersion: String by project
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
    implementation("org.xerial:sqlite-jdbc:${sqliteVersion}")
    implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-java-time:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-json:${exposedVersion}")

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