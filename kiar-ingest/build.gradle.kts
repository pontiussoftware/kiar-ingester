val bcryptVersion = project.property("bcryptVersion") as String
val caffeineVersion = project.property("caffeineVersion") as String
val commonsImagingVersion = project.property("commonsImagingVersion") as String
val exposedVersion = project.property("exposedVersion") as String
val gsonVersion = project.property("gsonVersion") as String
val ktorVersion = project.property("ktorVersion") as String
val jsonPathVersion = project.property("jsonPathVersion") as String
val kotlinCoroutines = project.property("kotlinCoroutines") as String
val kotlinLoggingVersion = project.property("kotlinLoggingVersion") as String
val kotlinSerialization = project.property("kotlinSerialization") as String
val log4jVersion = project.property("log4jVersion") as String
val poiVersion = project.property("poiVersion") as String
val scrimageVersion = project.property("scrimageVersion") as String
val slf4jVersion = project.property("slf4jVersion") as String
val solrjVersion = project.property("solrjVersion") as String
val sqliteVersion = project.property("sqliteVersion") as String


configurations {
    create("frontendClasspath") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
}

application {
    applicationName = "kiar-ingest"
    mainClass.set("ch.pontius.kiar.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Xms512M", "-Xmx4G")
}

/* Adjust names for archives. */
tasks.distZip {
    archiveFileName = "kiar-ingest-bin.zip"
}
tasks.distTar {
    archiveFileName = "kiar-ingest-bin.tar"
}

dependencies {
    /** Frontend. */
    implementation(project(":kiar-ui", "frontendFiles"))

    /** Caffeine cache.*/
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")

    /** SolrJ. */
    implementation("org.apache.solr:solr-solrj:$solrjVersion")
    implementation("org.apache.solr:solr-solrj-jetty:${solrjVersion}")
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
    implementation("io.github.oshai:kotlin-logging-jvm:${kotlinLoggingVersion}")

    /** JSON path. */
    implementation("com.jayway.jsonpath:json-path:$jsonPathVersion")

    /** Gson (used by the JSON ingest parsers). */
    implementation("com.google.code.gson:gson:$gsonVersion")

    /** Kotlinx. */
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$kotlinSerialization")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$kotlinCoroutines")

    /** Scrimage for image processing. */
    implementation("com.sksamuel.scrimage:scrimage-core:$scrimageVersion")
    implementation("com.sksamuel.scrimage:scrimage-formats-extra:$scrimageVersion")

    /** Apache Commons imaging for metadata processing. */
    implementation("org.apache.commons:commons-imaging:$commonsImagingVersion")

    /** Ktor server + OpenAPI. */
    implementation(platform("io.ktor:ktor-bom:$ktorVersion"))
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-sessions")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-routing-openapi")
    implementation("io.ktor:ktor-server-swagger")

    /** SQLite + Kotlin Exposed */
    implementation("org.xerial:sqlite-jdbc:${sqliteVersion}")
    implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-java-time:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-json:${exposedVersion}")
}

