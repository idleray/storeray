plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "com.rayject"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // CLI
    implementation(libs.clikt)

    // HTTP & Serialization
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.rayject.storeray.MainKt")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

// 制作 Fat JAR
tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes("Main-Class" to application.mainClass)
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}
