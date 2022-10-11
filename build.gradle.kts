import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val appCompatVersion: String by project

plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
}

group = "ch.pontius"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    /** SolrJ. */
    implementation("org.apache.solr:solr-solrj:9.0.0")

    /** Log4j2 & SLF4j */
    implementation("org.apache.logging.log4j:log4j-api:2.19.0")
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.19.0")

    /** Kotlinx Serialization. */
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")

    /** CLI. */
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("org.jline:jline:3.21.0")
    implementation("org.jline:jline-terminal-jna:3.21.0")

    /* UnitTest dependencies. */
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}