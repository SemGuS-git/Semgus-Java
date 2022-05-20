plugins {
    java
    `maven-publish`
}

group = "org.semgus"
version = "1.0.2-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io") {
        name = "Jitpack"
    }
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "semgus-java"
            from(components["java"])
        }
    }
}
