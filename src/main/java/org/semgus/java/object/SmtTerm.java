package org.semgus.java.object;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.semgus.java.util.DeserializationException;
import org.semgus.java.util.JsonUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface SmtTerm {

    static SmtTerm deserialize(Object termDtoRaw) throws DeserializationException {
        if (termDtoRaw instanceof Number value) {
            return new CNumber(value.longValue());
        } else if (termDtoRaw instanceof String value) {
            return new CString(value);
        } else if (termDtoRaw instanceof JSONObject termDto) {
            String termType = JsonUtils.getString(termDto, "$termType");
            return switch (termType) {
                case "application" -> deserializeApplication(termDto);
                case "exists" -> deserializeQuantifier(termDto, Quantifier.Type.EXISTS);
                case "forall" -> deserializeQuantifier(termDto, Quantifier.Type.FOR_ALL);
                case "variable" -> deserializeVariable(termDto);
                default -> throw new DeserializationException(
                        String.format("Unknown term type \"%s\"", termType), "$termType");
            };
        }
        throw new DeserializationException(String.format("Could not deserialize SMT term \"%s\"", termDtoRaw));
    }

    private static SmtTerm deserializeApplication(JSONObject termDto) throws DeserializationException {
        String name = JsonUtils.getString(termDto, "name");
        String returnType = JsonUtils.getString(termDto, "returnSort");

        List<String> argTypes = JsonUtils.getStrings(termDto, "argumentSorts");
        JSONArray args = JsonUtils.getArray(termDto, "arguments");
        if (argTypes.size() != args.size()) {
            throw new DeserializationException(String.format(
                    "Argument sorts and arguments of SMT function application have different lengths %d != %d",
                    argTypes.size(), args.size()));
        }
        Application.TypedTerm[] argTerms = new Application.TypedTerm[argTypes.size()];
        for (int i = 0; i < argTerms.length; i++) {
            try {
                argTerms[i] = new Application.TypedTerm(argTypes.get(i), deserialize(args.get(i)));
            } catch (DeserializationException e) {
                throw e.prepend("arguments." + i);
            }
        }

        return new Application(name, returnType, Arrays.asList(argTerms));
    }

    private static SmtTerm deserializeQuantifier(JSONObject termDto, Quantifier.Type qType)
            throws DeserializationException {
        List<JSONObject> bindingsDto = JsonUtils.getObjects(termDto, "bindings");
        TypedVar[] bindings = new TypedVar[bindingsDto.size()];
        for (int i = 0; i < bindings.length; i++) {
            JSONObject bindingDto = bindingsDto.get(i);
            try {
                bindings[i] = new TypedVar(
                        JsonUtils.getString(bindingDto, "name"), JsonUtils.getString(bindingDto, "sort"));
            } catch (DeserializationException e) {
                throw e.prepend("bindings." + i);
            }
        }

        Object childDtoRaw = JsonUtils.get(termDto, "child");
        SmtTerm child;
        try {
            child = deserialize(childDtoRaw);
        } catch (DeserializationException e) {
            throw e.prepend("child");
        }

        return new Quantifier(qType, Arrays.asList(bindings), child);
    }

    private static SmtTerm deserializeVariable(JSONObject termDto) throws DeserializationException {
        return new Variable(JsonUtils.getString(termDto, "name"), JsonUtils.getString(termDto, "sort"));
    }

    record Application(String name, String returnType, List<TypedTerm> arguments) implements SmtTerm {

        @Override
        public String toString() {
            if (arguments.size() == 0) {
                return "(" + name + ")";
            }
            return String.format("(%s %s)",
                    name, arguments.stream().map(TypedTerm::toString).collect(Collectors.joining(" ")));
        }

        public static record TypedTerm(String type, SmtTerm term) {

            @Override
            public String toString() {
                return term.toString();
            }

        }

    }

    record Quantifier(Type type, List<TypedVar> bindings, SmtTerm child) implements SmtTerm {

        public enum Type {
            EXISTS("∃"), FOR_ALL("∀");

            public final String symbol;

            Type(String symbol) {
                this.symbol = symbol;
            }

            @Override
            public String toString() {
                return symbol;
            }
        }

        @Override
        public String toString() {
            return String.format("(%s (%s) %s)",
                    type,
                    bindings.stream().map(TypedVar::toStringSExpr).collect(Collectors.joining(" ")),
                    child);
        }

    }

    record Variable(String name, String type) implements SmtTerm {

        @Override
        public String toString() {
            return name;
        }

    }

    record CString(String value) implements SmtTerm {

        @Override
        public String toString() {
            return "\"" + value + "\"";
        }

    }

    record CNumber(long value) implements SmtTerm {

        @Override
        public String toString() {
            return Long.toString(value);
        }

    }

}
