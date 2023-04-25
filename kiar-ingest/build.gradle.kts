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
    mainClass.set("ch.pontius.kiar.ApplicationKt")
}

dependencies {
    /** SolrJ. */
    implementation("org.apache.solr:solr-solrj:9.1.0")

    /** Log4j2 & SLF4j */
    implementation("org.apache.logging.log4j:log4j-api:2.19.0")
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.19.0")

    /** Kotlinx Serialization. */
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")

    /** Kotlinx Serialization. */
    implementation("com.twelvemonkeys.imageio:imageio-core:3.9.4")
    implementation("com.twelvemonkeys.imageio:imageio-jpeg:3.9.4")

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
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("org.jline:jline:3.21.0")
    implementation("org.jline:jline-terminal-jna:3.21.0")
}