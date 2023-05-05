val appCompatVersion: String by project

val bcryptVersion: String by project
val cliktVersion: String by project
val javalinVersion: String by project
val jacksonVersion: String by project
val jlineVersion: String by project
val kotlinVersion: String by project
val kotlinCoroutines: String by project
val kotlinSerializaion: String by project
val log4jVersion: String by project
val postgresVersion: String by project
val solrjVersion: String by project
val slf4jVersion: String by project
val twelveMonkeysVersion: String by project
val xodusVersion: String by project
val xodusDnqVersion: String by project

plugins {
    id("kotlin-kapt")
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

    /** Bcrypt */
    implementation("org.mindrot:jbcrypt:$bcryptVersion")

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

    /** Javalin + Open API. */
    implementation("io.javalin:javalin:$javalinVersion")
    implementation("io.javalin.community.openapi:javalin-openapi-plugin:$javalinVersion")
    implementation("io.javalin.community.openapi:javalin-swagger-plugin:$javalinVersion")
    implementation("io.javalin.community.ssl:ssl-plugin:$javalinVersion")
    kapt("io.javalin.community.openapi:openapi-annotation-processor:$javalinVersion")

    /* Jackson databind. */
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

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