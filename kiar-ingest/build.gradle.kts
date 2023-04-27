val appCompatVersion: String by project

val cliktVersion: String by project
val jlineVersion: String by project
val ktorVersion: String by project
val kotlinVersion: String by project
val kotlinCoroutines: String by project
val kotlinSerializaion: String by project
val log4jVersion: String by project
val solrjVersion: String by project
val slf4jVersion: String by project
val twelveMonkeysVersion: String by project
val xodusVersion: String by project
val xodusDnqVersion: String by project

plugins {
    id("io.ktor.plugin") version "2.3.0"
}

configurations {
    val frontendClasspath by creating {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
}

application {
    mainClass.set("ch.pontius.kiar.ApplicationKt")
}

dependencies {
    /** Frontend. */
    implementation(project(":kiar-ui", "frontendFiles"))

    /** SolrJ. */
    implementation("org.apache.solr:solr-solrj:$solrjVersion")

    /** Log4j2 & SLF4j */
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    /** Kotlinx. */
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$kotlinSerializaion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$kotlinCoroutines")

    /** Twelve Monkey (image processing). */
    implementation("com.twelvemonkeys.imageio:imageio-core:$twelveMonkeysVersion")
    implementation("com.twelvemonkeys.imageio:imageio-jpeg:$twelveMonkeysVersion")

    /** Ktor */
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")

    /** Xodus & Xodus DNQ */
    implementation("org.jetbrains.xodus:xodus-openAPI:$xodusVersion")
    implementation("org.jetbrains.xodus:xodus-environment:$xodusVersion")
    implementation("org.jetbrains.xodus:xodus-entity-store:$xodusVersion")
    implementation("org.jetbrains.xodus:xodus-vfs:$xodusVersion")
    implementation("org.jetbrains.xodus:dnq:$xodusDnqVersion")

    /** CLI. */
    implementation("com.github.ajalt.clikt:clikt:$cliktVersion")
    implementation("org.jline:jline:$jlineVersion")
    implementation("org.jline:jline-terminal-jna:$jlineVersion")
}