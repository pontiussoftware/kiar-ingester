val appCompatVersion: String by project

val bcryptVersion: String by project
val cliktVersion: String by project
val commonsImagingVersion: String by project
val javalinVersion: String by project
val jacksonVersion: String by project
val jlineVersion: String by project
val jsonPathVersion: String by project
val kotlinVersion: String by project
val kotlinCoroutines: String by project
val kotlinSerialization: String by project
val log4jVersion: String by project
val picnicVersion: String by project
val poiVersion: String by project
val scrimageVersion: String by project
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
    applicationDefaultJvmArgs = listOf("-Xms512M", "-Xmx4G")
}

dependencies {
    /** Frontend. */
    implementation(project(":kiar-ui", "frontendFiles"))

    /** SolrJ. */
    implementation("org.apache.solr:solr-solrj:$solrjVersion")

    /** Bcrypt */
    implementation("org.mindrot:jbcrypt:$bcryptVersion")

    /** Apache POI. */
    implementation("org.apache.poi:poi:$poiVersion")
    implementation("org.apache.poi:poi-ooxml:$poiVersion")

    /** Log4j2 & SLF4j */
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    /** JSON path. */
    implementation("com.jayway.jsonpath:json-path:$jsonPathVersion")

    /** Kotlinx. */
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$kotlinSerialization")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$kotlinCoroutines")

    /** Scrimage for image processing. */
    implementation("com.sksamuel.scrimage:scrimage-core:$scrimageVersion")
    implementation("com.sksamuel.scrimage:scrimage-formats-extra:$scrimageVersion")

    /** Apache Commons imaging for metadata processing. */
    implementation("org.apache.commons:commons-imaging:$commonsImagingVersion")

    /** Javalin + Open API. */
    implementation("io.javalin:javalin:$javalinVersion")
    implementation("io.javalin.community.openapi:javalin-openapi-plugin:$javalinVersion")
    implementation("io.javalin.community.openapi:javalin-swagger-plugin:$javalinVersion")
    implementation("io.javalin.community.ssl:ssl-plugin:$javalinVersion")
    kapt("io.javalin.community.openapi:openapi-annotation-processor:$javalinVersion")

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
    implementation("com.jakewharton.picnic:picnic:$picnicVersion")
}

