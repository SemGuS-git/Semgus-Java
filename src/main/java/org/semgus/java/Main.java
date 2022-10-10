package org.semgus.java;

import org.json.simple.parser.ParseException;
import org.semgus.java.event.EventParser;
import org.semgus.java.event.SpecEvent;
import org.semgus.java.problem.ProblemGenerator;
import org.semgus.java.problem.SemgusProblem;
import org.semgus.java.util.DeserializationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point class that simply reads a SemGuS JSON document, parses it, and dumps the problem to standard output.
 */
public final class Main {

    /**
     * Empty helper class constructor.
     */
    private Main() {
        // NO-OP
    }

    /**
     * Entry point: reads a JSON document, parses it as a SemGuS JSON specification, and dumps it to standard output.
     *
     * @param args CLI args
     * @throws IOException              If I/O fails.
     * @throws ParseException           if the JSON document is malformed.
     * @throws DeserializationException If the JSON document is not a valid SemGuS JSON specification.
     */
    public static void main(String[] args) throws IOException, ParseException, DeserializationException {
        // check CLI args
        if (args.length != 1) {
            System.err.println("Usage: semgus-java <spec-json>");
            System.exit(1);
        }
        Path specFile = Paths.get(args[0]);

        // check if it's stream mode or batch mode JSON by checking the first non-space character
        boolean stream = true;
        try (BufferedReader specJsonReader = Files.newBufferedReader(specFile)) {
            int chr;
            while ((chr = specJsonReader.read()) != 0) {
                if (!Character.isSpaceChar(chr)) {
                    if (chr == '[') { // if it's a [, it must be a JSON array, and so it's in batch mode
                        stream = false;
                    }
                    break;
                }
            }
        }

        // parse the problem
        SemgusProblem problem;
        try (BufferedReader specJsonReader = Files.newBufferedReader(Paths.get(args[0]))) {
            if (stream) { // stream mode: each line is an event
                List<SpecEvent> events = new ArrayList<>();
                String eventJson;
                while ((eventJson = specJsonReader.readLine()) != null) {
                    eventJson = eventJson.trim();
                    if (eventJson.isEmpty()) {
                        continue;
                    }
                    events.add(EventParser.parseEvent(eventJson));
                }
                problem = ProblemGenerator.fromEvents(events);
            } else { // batch mode: it's a big JSON array
                problem = ProblemGenerator.parse(specJsonReader);
            }
        }

        // dump the problem to stdout
        System.out.println(problem.dump());
    }

}
