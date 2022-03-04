package org.semgus.java.problem;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.semgus.java.event.EventParser;
import org.semgus.java.event.MetaSpecEvent;
import org.semgus.java.event.SemgusSpecEvent;
import org.semgus.java.event.SpecEvent;
import org.semgus.java.object.AttributeValue;
import org.semgus.java.object.SmtTerm;
import org.semgus.java.util.DeserializationException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.semgus.java.event.SemgusSpecEvent.*;

public class ProblemGenerator {

    public static SemgusProblem parse(Reader jsonReader) throws IOException, ParseException, DeserializationException {
        return fromEvents(EventParser.parse(jsonReader));
    }

    public static SemgusProblem parse(String json) throws ParseException, DeserializationException {
        return fromEvents(EventParser.parse(json));
    }

    public static SemgusProblem parse(JSONArray eventsDto) throws DeserializationException {
        return fromEvents(EventParser.parse(eventsDto));
    }

    public static SemgusProblem parseEvents(List<JSONObject> eventsDto) throws DeserializationException {
        return fromEvents(EventParser.parseEvents(eventsDto));
    }

    public static SemgusProblem fromEvents(Iterable<SpecEvent> events) {
        ProblemGenerator problemGen = new ProblemGenerator();
        for (SpecEvent event : events) {
            problemGen.consume(event);
        }
        return problemGen.end();
    }

    private final Map<String, AttributeValue> metadata = new HashMap<>();
    private final Map<String, TermType> termTypes = new HashMap<>();
    private final List<SmtTerm> constraints = new ArrayList<>();
    @Nullable
    private SynthFunEvent synthFun;

    public void consume(SpecEvent eventRaw) {
        if (eventRaw instanceof MetaSpecEvent.SetInfoEvent event) {
            consumeSetInfo(event);
        } else if (eventRaw instanceof DeclareTermTypeEvent event) {
            consumeDeclareTermType(event);
        } else if (eventRaw instanceof DefineTermTypeEvent event) {
            consumeDefineTermType(event);
        } else if (eventRaw instanceof SemgusSpecEvent.HornClauseEvent event) {
            consumeHornClause(event);
        } else if (eventRaw instanceof SemgusSpecEvent.ConstraintEvent event) {
            consumeConstraint(event);
        } else if (eventRaw instanceof SemgusSpecEvent.SynthFunEvent event) {
            consumeSynthFun(event);
        }
    }

    private void consumeSetInfo(MetaSpecEvent.SetInfoEvent event) {
        metadata.put(event.keyword(), event.value());
    }

    private void consumeDeclareTermType(DeclareTermTypeEvent event) {
        if (termTypes.containsKey(event.name())) {
            throw new IllegalStateException("Duplicate term type declaration: " + event.name());
        }
        termTypes.put(event.name(), new TermType());
    }

    private void consumeDefineTermType(DefineTermTypeEvent event) {
        TermType termType = termTypes.get(event.name());
        if (termType == null) {
            throw new IllegalStateException("Undeclared term type for definition: " + event.name());
        }
        for (DefineTermTypeEvent.Constructor constructor : event.constructors()) {
            if (termType.constructors.containsKey(constructor.name())) {
                throw new IllegalStateException("Duplicate term constructor: " + constructor.name());
            }
            for (String child : constructor.children()) {
                if (!termTypes.containsKey(child)) {
                    throw new IllegalStateException("Undeclared term type for constructor child: " + child);
                }
            }
            termType.constructors.put(constructor.name(), new TermConstructor(constructor.children()));
        }
    }

    private void consumeHornClause(HornClauseEvent event) {
        TermType termType = termTypes.get(event.constructor().returnType());
        if (termType == null) {
            throw new IllegalStateException("Unknown term type: " + event.constructor().returnType());
        }
        TermConstructor termCtor = termType.constructors.get(event.constructor().name());
        if (termCtor == null) {
            throw new IllegalStateException("Unknown term constructor: " + event.constructor().name());
        }
        // TODO do some checks to ensure all this CHC stuff is well-formed
        termCtor.semanticRules.add(new SemanticRule(
                event.constructor().arguments(),
                event.head(),
                event.bodyRelations(),
                event.constraint()));
    }

    private void consumeConstraint(ConstraintEvent event) {
        constraints.add(event.constraint()); // TODO check that the constraint is well-formed
    }

    private void consumeSynthFun(SynthFunEvent event) {
        if (synthFun != null) {
            throw new IllegalStateException("Synthesis function already set!");
        }
        synthFun = event; // TODO check that the synth-fun is well-formed and consistent
    }

    public SemgusProblem end() {
        if (synthFun == null) {
            throw new IllegalStateException("No synthesis function has been set!");
        }
        Map<String, SemgusNonTerminal> nonTerminals = new HashMap<>();
        for (SynthFunEvent.NonTerminal nonTerminal : synthFun.grammar().values()) {
            nonTerminals.put(nonTerminal.termType(), new SemgusNonTerminal(nonTerminal.termType(), new HashMap<>()));
        }
        for (SynthFunEvent.NonTerminal nonTerminal : synthFun.grammar().values()) {
            TermType termType = termTypes.get(nonTerminal.termType());
            Map<String, SemgusProduction> productions = nonTerminals.get(nonTerminal.termType()).productions();
            for (SynthFunEvent.Production production : nonTerminal.productions().values()) {
                TermConstructor termCtor = termType.constructors.get(production.operator());
                productions.put(production.operator(), new SemgusProduction(
                        production.operator(),
                        termCtor.childTermTypes.stream().map(nonTerminals::get).collect(Collectors.toList()),
                        termCtor.semanticRules.stream()
                                .map(r -> new SemanticRule(r.childTermVars(), r.head(), r.bodyRelations(), r.constraint()))
                                .collect(Collectors.toList())));
            }
        }
        return new SemgusProblem(nonTerminals, constraints);
    }

    private static class TermType {

        private final Map<String, TermConstructor> constructors = new HashMap<>();

    }

    private static class TermConstructor {

        private final List<String> childTermTypes;
        private final List<SemanticRule> semanticRules = new ArrayList<>();

        private TermConstructor(List<String> childTermTypes) {
            this.childTermTypes = childTermTypes;
        }

    }

}
