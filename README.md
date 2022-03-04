# SemGuS Java Consumer

This library provides a means of marshalling the [SemGuS parser's](https://github.com/SemGuS-git/Semgus-Parser) JSON
output into a usable data structure for JVM-based languages.

## Requirements

SemGuS-Java depends at runtime on JSON-Simple and at compile-time on JSR-305, which is available as part of Google
FindBugs. These dependencies are available at the following Maven coordinates:
```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io") { name = "Jitpack" }
}

dependencies {
    implementation("com.github.phantamanta44:jsr305:1.0.1")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
}
```
SemGuS-Java targets Java 16 and makes extensive use of records to represent SemGuS objects.

## Usage

SemGuS-Java provides two different representations for SemGuS problems: a stream of specification events and a
`SemgusProblem` data structure. These are comparable to SAX-style and DOM-style XML parsing, respectively. To produce
these representations, use the methods available in the `EventParser` and `ProblemGenerator` classes, which can pull
JSON data from JSON-Simple objects, strings, or `Reader`s.

Additionally, an entry point is available in `Main`, which simply takes a SemGuS JSON document, parses it into a SemGuS
problem, then dumps it to standard output.
