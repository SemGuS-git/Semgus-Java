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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.semgus.java.event.SemgusSpecEvent.*;

/**
 * Helper class that deserializes SemGuS parser events from their JSON representations.
 */
public class EventParser {

    /**
     * JSON parser instance used by the helper functions in this class.
     */
    private static final JSONParser JSON_PARSER = new JSONParser();

    /**
     * Fully consumes a {@link Reader} as a character stream and parses it into an array of parser events.
     *
     * @param jsonReader The reader to read JSON from.
     * @return The deserialized parser events.
     * @throws IOException              If there is an I/O error while reading from the stream.
     * @throws ParseException           If there is malformed JSON in the stream.
     * @throws DeserializationException If the JSON is not a valid representation of an array of parser events.
     */
    public static List<SpecEvent> parse(Reader jsonReader)
            throws IOException, ParseException, DeserializationException {
        Object eventsDto = JSON_PARSER.parse(jsonReader);
        if (!(eventsDto instanceof JSONArray)) {
            throw new DeserializationException("Event array must be a JSON array!");
        }
        return parse((JSONArray)eventsDto);
    }

    /**
     * Parses a string as a JSON array of parser events.
     *
     * @param json The JSON string.
     * @return The deserialized parser events.
     * @throws ParseException           If the string is not well-formed JSON.
     * @throws DeserializationException If the JSON is not a valid representation of an array of parser events.
     */
    public static List<SpecEvent> parse(String json) throws ParseException, DeserializationException {
        Object eventsDto = JSON_PARSER.parse(json);
        if (!(eventsDto instanceof JSONArray)) {
            throw new DeserializationException("Event array must be a JSON array!");
        }
        return parse((JSONArray)eventsDto);
    }

    /**
     * Deserializes a JSON array as a list of parser events.
     *
     * @param eventsDto The JSON array, where each element should be a JSON object representing a parser event.
     * @return The deserialized parser events.
     * @throws DeserializationException If {@code eventsDto} is not a valid representation of an array of parser events.
     */
    public static List<SpecEvent> parse(JSONArray eventsDto) throws DeserializationException {
        return parseEvents(JsonUtils.ensureObjects(eventsDto));
    }

    /**
     * Deserializes a list of JSON objects as parser events.
     *
     * @param eventsDto The parser event objects.
     * @return The deserialized parser events.
     * @throws DeserializationException If an element of {@code eventsDto} is not a valid parser event.
     */
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

    /**
     * Parses a string as a parser event.
     *
     * @param eventJson The JSON string.
     * @return The deserialized parser event.
     * @throws ParseException           If the string is not well-formed JSON.
     * @throws DeserializationException If the JSON is not a valid representation of a parser event.
     */
    public static SpecEvent parseEvent(String eventJson) throws ParseException, DeserializationException {
        Object eventDto = JSON_PARSER.parse(eventJson);
        if (!(eventDto instanceof JSONObject)) {
            throw new DeserializationException("Event object must be a JSON object!");
        }
        return parseEvent((JSONObject)eventDto);
    }

    /**
     * Deserializes a JSON object as a parser event.
     *
     * @param eventDto The JSON object.
     * @return The deserialized parser event.
     * @throws DeserializationException If {@code eventDto} is not a valid representation of a parser event.
     */
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
                    String.format("Unknown specification event \"%s\"", eventType), "$event");
        };
    }

    /**
     * Deserializes a "set-info" event.
     *
     * @param eventDto The JSON representation of the event.
     * @return The deserialized event.
     * @throws DeserializationException If {@code eventDto} is not a valid representation of a "set-info" event.
     */
    private static SpecEvent parseSetInfo(JSONObject eventDto) throws DeserializationException {
        String keyword = JsonUtils.getString(eventDto, "keyword");
        Object valueDtoRaw = JsonUtils.get(eventDto, "value");
        try {
            return new MetaSpecEvent.SetInfoEvent(keyword, AttributeValue.deserialize(valueDtoRaw));
        } catch (DeserializationException e) {
            throw e.prepend("value");
        }
    }

    /**
     * Deserializes a "declare-term-type" event.
     *
     * @param eventDto The JSON representation of the event.
     * @return The deserialized event.
     * @throws DeserializationException If {@code eventDto} is not a valid representation of a "declare-term-type"
     *                                  event.
     */
    private static SpecEvent parseDeclareTermType(JSONObject eventDto) throws DeserializationException {
        return new DeclareTermTypeEvent(JsonUtils.getString(eventDto, "name"));
    }

    /**
     * Deserializes a "define-term-type" event.
     *
     * @param eventDto The JSON representation of the event.
     * @return The deserialized event.
     * @throws DeserializationException If {@code eventDto} is not a valid representation of a "define-term-type" event.
     */
    private static SpecEvent parseDefineTermType(JSONObject eventDto) throws DeserializationException {
        String name = JsonUtils.getString(eventDto, "name");
        List<JSONObject> constructorsDto = JsonUtils.getObjects(eventDto, "constructors");

        // parse constructors definitions
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

    /**
     * Deserializes a "chc" event.
     *
     * @param eventDto The JSON representation of the event.
     * @return The deserialized event.
     * @throws DeserializationException If {@code eventDto} is not a valid representation of a "chc" event.
     */
    private static SpecEvent parseHornClause(JSONObject eventDto) throws DeserializationException {
        JSONObject constructorDto = JsonUtils.getObject(eventDto, "constructor");

        // parse constructor specification
        String constructorName, returnType;
        List<String> constructorArgs;
        JSONArray constructorArgTypesDto;
        try {
            constructorName = JsonUtils.getString(constructorDto, "name");
            returnType = JsonUtils.getString(constructorDto, "returnSort");
            constructorArgs = JsonUtils.getStrings(constructorDto, "arguments");
            constructorArgTypesDto = JsonUtils.getArray(constructorDto, "argumentSorts");
        } catch (DeserializationException e) {
            throw e.prepend("constructor");
        }
        if (constructorArgs.size() != constructorArgTypesDto.size()) { // ensure args and arg types coincide
            throw new DeserializationException(
                    String.format(
                            "Argument sorts and arguments of CHC constructor have different lengths %d != %d",
                            constructorArgTypesDto.size(), constructorArgs.size()),
                    "constructor");
        }

        // parse constructor arg type identifiers
        Identifier[] constructorArgTypes = new Identifier[constructorArgTypesDto.size()];
        for (int i = 0; i < constructorArgTypes.length; i++) {
            try {
                constructorArgTypes[i] = Identifier.deserialize(constructorArgTypesDto.get(i));
            } catch (DeserializationException e) {
                throw e.prepend("constructor.argumentSorts." + i);
            }
        }
        HornClauseEvent.Constructor constructor = new HornClauseEvent.Constructor(
                constructorName,
                TypedVar.fromNamesAndTypes(constructorArgs, Arrays.asList(constructorArgTypes)),
                returnType);

        // parse head relation
        JSONObject headDto = JsonUtils.getObject(eventDto, "head");
        RelationApp head;
        try {
            head = RelationApp.deserialize(headDto);
        } catch (DeserializationException e) {
            throw e.prepend("head");
        }

        // parse body relations
        List<JSONObject> bodyRelationsDto = JsonUtils.getObjects(eventDto, "bodyRelations");
        RelationApp[] bodyRelations = new RelationApp[bodyRelationsDto.size()];
        for (int i = 0; i < bodyRelations.length; i++) {
            try {
                bodyRelations[i] = RelationApp.deserialize(bodyRelationsDto.get(i));
            } catch (DeserializationException e) {
                throw e.prepend("bodyRelations." + i);
            }
        }

        // parse semantic constraint
        Object constraintDtoRaw = JsonUtils.get(eventDto, "constraint");
        SmtTerm constraint;
        try {
            constraint = SmtTerm.deserialize(constraintDtoRaw);
        } catch (DeserializationException e) {
            throw e.prepend("constraint");
        }

        // parse variable list
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

        // parse input variable annotations
        Object inputVariablesRaw = eventDto.get("inputVariables");
        if (inputVariablesRaw != null) {
            // if present, ensure it's an array of strings
            if (!(inputVariablesRaw instanceof JSONArray)) {
                throw new DeserializationException("Input variable list must be a JSON array!", "inputVariables");
            }
            List<String> inputVariables = JsonUtils.ensureStrings((JSONArray)inputVariablesRaw);

            // annotate the listed variables
            for (int i = 0; i < inputVariables.size(); i++) {
                AnnotatedVar variable = variables.get(inputVariables.get(i));
                if (variable == null) {
                    throw new DeserializationException(
                            String.format("Unknown variable \"%s\" declared as input", inputVariables.get(i)),
                            "inputVariables." + i);
                }
                variable.attributes().put("input", new AttributeValue.Unit());
            }
        }

        // ditto for output variable annotations
        Object outputVariablesRaw = eventDto.get("outputVariables");
        if (outputVariablesRaw != null) {
            // if present, ensure it's an array of strings
            if (!(outputVariablesRaw instanceof JSONArray)) {
                throw new DeserializationException("Output variable list must be a JSON array!", "outputVariables");
            }
            List<String> outputVariables = JsonUtils.ensureStrings((JSONArray)outputVariablesRaw);

            // annotate the listed variables
            for (int i = 0; i < outputVariables.size(); i++) {
                AnnotatedVar variable = variables.get(outputVariables.get(i));
                if (variable == null) {
                    throw new DeserializationException(
                            String.format("Unknown variable \"%s\" declared as output", outputVariables.get(i)),
                            "outputVariables." + i);
                }
                variable.attributes().put("output", new AttributeValue.Unit());
            }
        }

        return new HornClauseEvent(
                constructor, head, Arrays.asList(bodyRelations), constraint, variables);
    }

    /**
     * Deserializes a "constraint" event.
     *
     * @param eventDto The JSON representation of the event.
     * @return The deserialized event.
     * @throws DeserializationException If {@code eventDto} is not a valid representation of a "constraint" event.
     */
    private static SpecEvent parseConstraint(JSONObject eventDto) throws DeserializationException {
        Object constraintDtoRaw = JsonUtils.get(eventDto, "constraint");
        try {
            return new ConstraintEvent(SmtTerm.deserialize(constraintDtoRaw));
        } catch (DeserializationException e) {
            throw e.prepend("constraint");
        }
    }

    /**
     * Deserializes a "synth-fun" event.
     *
     * @param eventDto The JSON representation of the event.
     * @return The deserialized event.
     * @throws DeserializationException If {@code eventDto} is not a valid representation of a "synth-fun" event.
     */
    private static SpecEvent parseSynthFun(JSONObject eventDto) throws DeserializationException {
        String name = JsonUtils.getString(eventDto, "name");
        String termType = JsonUtils.getString(eventDto, "termType");

        // parse target grammar specification
        JSONObject grammarDto = JsonUtils.getObject(eventDto, "grammar");
        Map<String, SynthFunEvent.NonTerminal> grammar = new HashMap<>();
        try {
            List<JSONObject> nonTerminalsDto = JsonUtils.getObjects(grammarDto, "nonTerminals");
            List<JSONObject> productionsDto = JsonUtils.getObjects(grammarDto, "productions");

            // construct non-terminals
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

            // construct productions and attach them to their associated non-terminals
            for (int i = 0; i < productionsDto.size(); i++) {
                JSONObject prodDto = productionsDto.get(i);
                try {
                    String ntName = JsonUtils.getString(prodDto, "instance");
                    String operator = JsonUtils.getString(prodDto, "operator");
                    List<String> occurrences = JsonUtils.getStrings(prodDto, "occurrences");

                    SynthFunEvent.NonTerminal nonTerminal = grammar.get(ntName);
                    if (nonTerminal == null) { // ensure non-terminal exists
                        throw new DeserializationException(
                                String.format("Unknown nonterminal \"%s\" referenced in production", ntName),
                                "instance");
                    }
                    if (nonTerminal.productions().containsKey(operator)) { // ensure this production is distinct
                        throw new DeserializationException(
                                String.format("Duplicate production \"%s\" for nonterminal \"%s\"", operator, ntName),
                                "operator");
                    }

                    for (int j = 0; j < occurrences.size(); j++) { // ensure child term non-terminals exist
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
