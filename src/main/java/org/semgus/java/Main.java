package org.semgus.java;

import org.semgus.java.problem.ProblemGenerator;
import org.semgus.java.problem.SemgusProblem;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) throws Exception {
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
