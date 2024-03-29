plugins {
    java
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "org.semgus"
version = "1.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io") {
        name = "Jitpack"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("com.github.phantamanta44:jsr305:1.0.1")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.getByName<Jar>("jar") {
    manifest {
        attributes(
            "Main-Class" to "org.semgus.java.Main"
        )
    }
}

tasks.shadowJar {
    configurations = listOf(project.configurations.getByName("runtimeClasspath")) // Include runtime dependencies in shadow JAR
    manifest {
        attributes(
            "Main-Class" to "org.semgus.java.Main"
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "semgus-java"
            from(components["java"])
        }
    }
}
