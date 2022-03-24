package org.semgus.java;

import org.json.simple.parser.ParseException;
import org.semgus.java.problem.ProblemGenerator;
import org.semgus.java.problem.SemgusProblem;
import org.semgus.java.util.DeserializationException;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Entry point class that simply reads a SemGuS JSON document, parses it, and dumps the problem to standard output.
 */
public class Main {

    /**
     * Entry point:
     *
     * @param args CLI args
     * @throws IOException              If I/O fails.
     * @throws ParseException           if the JSON document is malformed.
     * @throws DeserializationException If the JSON document is not a valid SemGuS JSON specification.
     */
    public static void main(String[] args) throws IOException, ParseException, DeserializationException {
        if (args.length != 1) {
            System.err.println("Usage: semgus-java <spec-json>");
            System.exit(1);
        }

        SemgusProblem problem;
        try (Reader specJsonReader = Files.newBufferedReader(Paths.get(args[0]))) {
            problem = ProblemGenerator.parse(specJsonReader);
        }

        System.out.println(problem.dump());
    }

}
