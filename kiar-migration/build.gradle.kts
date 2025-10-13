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