package org.semgus.java.event;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.semgus.java.object.*;
import org.semgus.java.util.DeserializationException;
import org.semgus.java.util.JsonUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

import static org.semgus.java.event.SemgusSpecEvent.*;

public class EventParser {

    private static final JSONParser JSON_PARSER = new JSONParser();

    public static List<SpecEvent> parse(Reader jsonReader) throws IOException, ParseException, DeserializationException {
        Object eventsDto = JSON_PARSER.parse(jsonReader);
        if (!(eventsDto instanceof JSONArray)) {
            throw new DeserializationException("Event array must be a JSON array!", "");
        }
        return parse((JSONArray)eventsDto);
    }

    public static List<SpecEvent> parse(String json) throws ParseException, DeserializationException {
        Object eventsDto = JSON_PARSER.parse(json);
        if (!(eventsDto instanceof JSONArray)) {
            throw new DeserializationException("Event array must be a JSON array!", "");
        }
        return parse((JSONArray)eventsDto);
    }

    public static List<SpecEvent> parse(JSONArray eventsDto) throws DeserializationException {
        return parseEvents(JsonUtils.ensureObjects(eventsDto));
    }

    public static List<SpecEvent> parseEvents(List<JSONObject> eventsDto) throws DeserializationException {
        SpecEvent[] events = new SpecEvent[eventsDto.size()];
        for (int i = 0; i < events.length; i++) {
            try {
                events[i] = parseEvent(eventsDto.get(i));
            } catch (DeserializationException e) {
                throw e.prepend(i);
            }
        }
        return Arrays.asList(events);
    }

    public static SpecEvent parseEvent(JSONObject eventDto) throws DeserializationException {
        String eventType = JsonUtils.getString(eventDto, "$event");
        return switch (eventType) {
            case "set-info" -> parseSetInfo(eventDto);
            case "end-of-stream" -> new MetaSpecEvent.StreamEndEvent();
            case "check-synth" -> new CheckSynthEvent();
            case "declare-term-type" -> parseDeclareTermType(eventDto);
            case "define-term-type" -> parseDefineTermType(eventDto);
            case "chc" -> parseHornClause(eventDto);
            case "constraint" -> parseConstraint(eventDto);
            case "synth-fun" -> parseSynthFun(eventDto);
            default -> throw new DeserializationException(
                    String.format("Unknown specification event \"%s\"", eventType), "type");
        };
    }

    private static SpecEvent parseSetInfo(JSONObject eventDto) throws DeserializationException {
        String keyword = JsonUtils.getString(eventDto, "keyword");
        Object valueDtoRaw = JsonUtils.get(eventDto, "value");
        AttributeValue value;
        try {
            return new MetaSpecEvent.SetInfoEvent(keyword, AttributeValue.deserialize(valueDtoRaw));
        } catch (DeserializationException e) {
            throw e.prepend("value");
        }
    }

    private static SpecEvent parseDeclareTermType(JSONObject eventDto) throws DeserializationException {
        return new DeclareTermTypeEvent(JsonUtils.getString(eventDto, "name"));
    }

    private static SpecEvent parseDefineTermType(JSONObject eventDto) throws DeserializationException {
        String name = JsonUtils.getString(eventDto, "name");
        List<JSONObject> constructorsDto = JsonUtils.getObjects(eventDto, "constructors");
        DefineTermTypeEvent.Constructor[] constructors = new DefineTermTypeEvent.Constructor[constructorsDto.size()];
        for (int i = 0; i < constructors.length; i++) {
            JSONObject constructorDto = constructorsDto.get(i);
            try {
                constructors[i] = new DefineTermTypeEvent.Constructor(
                        JsonUtils.getString(constructorDto, "name"),
                        JsonUtils.getStrings(constructorDto, "children"));
            } catch (DeserializationException e) {
                throw e.prepend("constructors." + i);
            }
        }
        return new DefineTermTypeEvent(name, Arrays.asList(constructors));
    }

    private static SpecEvent parseHornClause(JSONObject eventDto) throws DeserializationException {
        JSONObject constructorDto = JsonUtils.getObject(eventDto, "constructor");

        String constructorName, returnType;
        List<String> constructorArgs, constructorArgTypes;
        try {
            constructorName = JsonUtils.getString(constructorDto, "name");
            returnType = JsonUtils.getString(constructorDto, "returnSort");
            constructorArgs = JsonUtils.getStrings(constructorDto, "arguments");
            constructorArgTypes = JsonUtils.getStrings(constructorDto, "argumentSorts");
        } catch (DeserializationException e) {
            throw e.prepend("constructor");
        }
        if (constructorArgs.size() != constructorArgTypes.size()) {
            throw new DeserializationException(
                    String.format(
                            "Argument sorts and arguments of CHC constructor have different lengths %d != %d",
                            constructorArgTypes.size(), constructorArgs.size()),
                    "constructor");
        }
        HornClauseEvent.Constructor constructor = new HornClauseEvent.Constructor(
                constructorName, TypedVar.fromNamesAndTypes(constructorArgs, constructorArgTypes), returnType);

        JSONObject headDto = JsonUtils.getObject(eventDto, "head");
        RelationApp head;
        try {
            head = RelationApp.deserialize(headDto);
        } catch (DeserializationException e) {
            throw e.prepend("head");
        }

        List<JSONObject> bodyRelationsDto = JsonUtils.getObjects(eventDto, "bodyRelations");
        RelationApp[] bodyRelations = new RelationApp[bodyRelationsDto.size()];
        for (int i = 0; i < bodyRelations.length; i++) {
            try {
                bodyRelations[i] = RelationApp.deserialize(bodyRelationsDto.get(i));
            } catch (DeserializationException e) {
                throw e.prepend("bodyRelations." + i);
            }
        }

        Object constraintDtoRaw = JsonUtils.get(eventDto, "constraint");
        SmtTerm constraint;
        try {
            constraint = SmtTerm.deserialize(constraintDtoRaw);
        } catch (DeserializationException e) {
            throw e.prepend("constraint");
        }

        List<String> variablesDto = JsonUtils.getStrings(eventDto, "variables");
        Map<String, AnnotatedVar> variables = new HashMap<>();
        for (int i = 0; i < variablesDto.size(); i++) {
            String variable = variablesDto.get(i);
            if (variables.containsKey(variable)) {
                throw new DeserializationException(
                        String.format("Duplicate variable \"%s\"", variable), "variables." + i);
            }
            variables.put(variable, new AnnotatedVar(variable, new HashMap<>()));
        }

        List<String> inputVariables = JsonUtils.getStrings(eventDto, "inputVariables");
        List<String> outputVariables = JsonUtils.getStrings(eventDto, "outputVariables");
        for (int i = 0; i < inputVariables.size(); i++) {
            AnnotatedVar variable = variables.get(inputVariables.get(i));
            if (variable == null) {
                throw new DeserializationException(
                        String.format("Unknown variable \"%s\" declared as input", inputVariables.get(i)),
                        "inputVariables." + i);
            }
            variable.attributes().put("input", new AttributeValue.Unit());
        }
        for (int i = 0; i < outputVariables.size(); i++) {
            AnnotatedVar variable = variables.get(outputVariables.get(i));
            if (variable == null) {
                throw new DeserializationException(
                        String.format("Unknown variable \"%s\" declared as output", outputVariables.get(i)),
                        "outputVariables." + i);
            }
            variable.attributes().put("output", new AttributeValue.Unit());
        }

        return new HornClauseEvent(
                constructor, head, Arrays.asList(bodyRelations), constraint, new HashSet<>(variables.values()));
    }

    private static SpecEvent parseConstraint(JSONObject eventDto) throws DeserializationException {
        Object constraintDtoRaw = JsonUtils.get(eventDto, "constraint");
        try {
            return new ConstraintEvent(SmtTerm.deserialize(constraintDtoRaw));
        } catch (DeserializationException e) {
            throw e.prepend("constraint");
        }
    }

    private static SpecEvent parseSynthFun(JSONObject eventDto) throws DeserializationException {
        String name = JsonUtils.getString(eventDto, "name");
        String termType = JsonUtils.getString(eventDto, "termType");

        JSONObject grammarDto = JsonUtils.getObject(eventDto, "grammar");
        Map<String, SynthFunEvent.NonTerminal> grammar = new HashMap<>();
        try {
            List<JSONObject> nonTerminalsDto = JsonUtils.getObjects(grammarDto, "nonTerminals");
            List<JSONObject> productionsDto = JsonUtils.getObjects(grammarDto, "productions");

            for (int i = 0; i < nonTerminalsDto.size(); i++) {
                JSONObject ntDto = nonTerminalsDto.get(i);
                try {
                    String ntName = JsonUtils.getString(ntDto, "name");
                    if (grammar.containsKey(ntName)) {
                        throw new DeserializationException(
                                String.format("Duplicate nonterminal declaration \"%s\"", ntName), "name");
                    }
                    grammar.put(ntName,
                            new SynthFunEvent.NonTerminal(JsonUtils.getString(ntDto, "termType"), new HashMap<>()));
                } catch (DeserializationException e) {
                    throw e.prepend("nonTerminals." + i);
                }
            }

            for (int i = 0; i < productionsDto.size(); i++) {
                JSONObject prodDto = productionsDto.get(i);
                try {
                    String ntName = JsonUtils.getString(prodDto, "instance");
                    String operator = JsonUtils.getString(prodDto, "operator");
                    List<String> occurrences = JsonUtils.getStrings(prodDto, "occurrences");

                    SynthFunEvent.NonTerminal nonTerminal = grammar.get(ntName);
                    if (nonTerminal == null) {
                        throw new DeserializationException(
                                String.format("Unknown nonterminal \"%s\" referenced in production", ntName),
                                "instance");
                    }
                    if (nonTerminal.productions().containsKey(operator)) {
                        throw new DeserializationException(
                                String.format("Duplicate production \"%s\" for nonterminal \"%s\"", operator, ntName),
                                "operator");
                    }

                    for (int j = 0; j < occurrences.size(); j++) {
                        if (!grammar.containsKey(occurrences.get(j))) {
                            throw new DeserializationException(
                                    String.format("Unknown nonterminal \"%s\" referenced in production child", ntName),
                                    "occurrences." + j);
                        }
                    }

                    nonTerminal.productions().put(operator, new SynthFunEvent.Production(operator, occurrences));
                } catch (DeserializationException e) {
                    throw e.prepend("productions." + i);
                }
            }
        } catch (DeserializationException e) {
            throw e.prepend("grammar");
        }

        return new SynthFunEvent(name, grammar, termType);
    }

}
