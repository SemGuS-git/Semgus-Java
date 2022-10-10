# SemGuS Java Consumer

This library provides a means of marshalling the [SemGuS parser's](https://github.com/SemGuS-git/Semgus-Parser) JSON
output into a usable data structure for JVM-based languages.

## Requirements

The recommended way to use SemGuS-Java is to install it from Maven via [JitPack](https://jitpack.io/#SemGuS-git/Semgus-Java),
which is accomplished in Gradle as follows:
```kotlin
repositories {
    maven("https://jitpack.io") { name = "Jitpack" }
}

dependencies {
    implementation("com.github.SemGuS-git:Semgus-Java:1.0.2")
}
```
SemGuS-Java depends at runtime on JSON-Simple and at compile-time on JSR-305, which is available as part of Google
FindBugs. These dependencies are available at the following Maven coordinates, if you wish to install them manually:
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
SemGuS-Java targets Java 17 and makes extensive use of sealed types and records to represent SemGuS objects.

## Usage

SemGuS-Java provides two different representations for SemGuS problems: a stream of specification events and a
`SemgusProblem` data structure. These are comparable to SAX-style and DOM-style XML parsing, respectively.

To produce a stream of events, you can use the functions defined in `EventParser`, which parse JSON objects into event
objects. For example:

```java
List<SpecEvent> events;
try (Reader reader = new FileReader(new File("max2-exp.sem.json"))) { // open a reader for the JSON file
    events = EventParser.parse(reader); // parse the spec file into a series of events
}
for (SpecEvent event : events) { // dump all the events to console
    System.out.println(event);
}
```

To produce a `SemgusProblem`, you can use the functions defined in `ProblemGenerator`, which consumes a sequence of
events, assembling the synthesis problem data into a `SemgusProblem` data structure. For example:

```java
SemgusProblem problem;
try (Reader reader = new FileReader(new File("max2-exp.sem.json"))) { // open a reader for the JSON file
    problem = ProblemGenerator.parse(reader); // parse the spec file into a SemGuS problem object
}
System.out.println(problem.dump()); // print a human-readable representation of the SemGuS problem
```

Additionally, an entry point is available in `Main`, which simply takes a SemGuS JSON document, parses it into a SemGuS
problem, then dumps it to standard output:

```shell
$ java -jar semgus-java.jar max2-exp.sem.json
```

For more information, check out all the declarations and accompanying JavaDocs in the source code.
