val appCompatVersion: String by project
val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val xodusVersion: String by project
val xodusDnqVersion: String by project

plugins {
    id("io.ktor.plugin") version "2.2.2"
}

application {
    mainClass.set("ch.pontius.kiar.uploader.ApplicationKt")
}

dependencies {

}