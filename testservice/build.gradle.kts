plugins {
    kotlin("jvm")
    java
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.wire.kalium.testservice"
version = "0.0.1-SNAPSHOT"

object Versions {
    const val dropwizard = "2.1.2"
    const val prometheus_simpleclient = "0.1.0"
}

val mainFunctionClassName = "com.wire.kalium.testservice.TestserviceApplication"

application {
    mainClass.set(mainFunctionClassName)
}

tasks.named("run", JavaExec::class) {
    jvmArgs = listOf("-Djava.library.path=/usr/local/lib/:../native/libs")
    args = listOf("server", "config.yml")
    isIgnoreExitValue = true
    standardInput = System.`in`
    standardOutput = System.out
}

tasks.shadowJar {
    archiveBaseName.set("testservice")
    mergeServiceFiles()
    manifest {
        attributes(mapOf("Main-Class" to mainFunctionClassName))
    }
}

repositories {
    // to fetch a version of dropwizard-swagger via git reference
    maven(url = "https://jitpack.io")
}

dependencies {
    add("implementation", "io.dropwizard:dropwizard-core:${Versions.dropwizard}")
    // TODO: The next line was replaced by the following because jitpack.io repo was unreachable
    // add("implementation", "com.github.smoketurner:dropwizard-swagger:72e8441e4a")
    add("implementation", "com.smoketurner:dropwizard-swagger:2.0.12-1")
    add("implementation", "org.slf4j:slf4j-api:1.7.22")

    // prometheus metrics
    add("implementation", "io.prometheus:simpleclient_dropwizard:${Versions.prometheus_simpleclient}")
    add("implementation", "io.prometheus:simpleclient_servlet:${Versions.prometheus_simpleclient}")

    add("implementation", project(":network"))
    add("implementation", project(":cryptography")) {
        exclude("org.slf4j", "slf4j-api")
    }
    add("implementation", project(":logic")) {
        exclude("org.slf4j", "slf4j-api")
    }

    // Okio
    implementation(libs.okio.core)
}