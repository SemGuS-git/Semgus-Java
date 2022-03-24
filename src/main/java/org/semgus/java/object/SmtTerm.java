package org.semgus.java.object;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.semgus.java.util.DeserializationException;
import org.semgus.java.util.JsonUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a term of an SMT formula.
 */
public interface SmtTerm {

    /**
     * Deserializes an SMT formula term in the SemGuS JSON format.
     *
     * @param termDtoRaw The JSON representation of the term.
     * @return The deserialized term.
     * @throws DeserializationException If {@code termDtoRaw} is not a valid representation of an SMT term.
     */
    static SmtTerm deserialize(Object termDtoRaw) throws DeserializationException {
        if (termDtoRaw instanceof Number value) { // it's a numeric constant
            return new CNumber(value.longValue());
        } else if (termDtoRaw instanceof String value) { // it's a string constant
            return new CString(value);
        } else if (termDtoRaw instanceof JSONObject termDto) { // it's something more complex
            String termType = JsonUtils.getString(termDto, "$termType");
            return switch (termType) {
                case "application" -> deserializeApplication(termDto); // a function application
                case "exists" -> deserializeQuantifier(termDto, Quantifier.Type.EXISTS); // an existential quantifier
                case "forall" -> deserializeQuantifier(termDto, Quantifier.Type.FOR_ALL); // a universal quantifier
                case "variable" -> deserializeVariable(termDto); // a variable
                default -> throw new DeserializationException(
                        String.format("Unknown term type \"%s\"", termType), "$termType");
            };
        }
        throw new DeserializationException(String.format("Could not deserialize SMT term \"%s\"", termDtoRaw));
    }

    /**
     * Deserializes a function application.
     *
     * @param termDto The JSON representation of the function application.
     * @return The deserialized function application.
     * @throws DeserializationException If {@code termDto} is not a valid representation of a function application.
     */
    private static SmtTerm deserializeApplication(JSONObject termDto) throws DeserializationException {
        String name = JsonUtils.getString(termDto, "name");
        String returnType = JsonUtils.getString(termDto, "returnSort");

        // zip together argument terms and argument types
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

    /**
     * Deserializes a quantified subterm.
     *
     * @param termDto The JSON representation of the quantified subterm.
     * @param qType   The type of quantifier.
     * @return The deserialized subterm.
     * @throws DeserializationException If {@code termDto} is not a valid representation of a quantifier.
     */
    private static SmtTerm deserializeQuantifier(JSONObject termDto, Quantifier.Type qType)
            throws DeserializationException {
        // collect variables bound by the quantifier
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

        // deserialize child term
        Object childDtoRaw = JsonUtils.get(termDto, "child");
        SmtTerm child;
        try {
            child = deserialize(childDtoRaw);
        } catch (DeserializationException e) {
            throw e.prepend("child");
        }

        return new Quantifier(qType, Arrays.asList(bindings), child);
    }

    /**
     * Deserializes a variable.
     *
     * @param termDto The JSON representation of the variable.
     * @return The deserialized variable.
     * @throws DeserializationException If {@code termDto} is not a valid representation of a variable.
     */
    private static SmtTerm deserializeVariable(JSONObject termDto) throws DeserializationException {
        return new Variable(JsonUtils.getString(termDto, "name"), JsonUtils.getString(termDto, "sort"));
    }

    /**
     * Represents a function application in an SMT formula.
     *
     * @param name       The name of the function.
     * @param returnType The function's return type.
     * @param arguments  The arguments to the function.
     */
    record Application(String name, String returnType, List<TypedTerm> arguments) implements SmtTerm {

        @Override
        public String toString() {
            if (arguments.size() == 0) {
                return "(" + name + ")";
            }
            return String.format("(%s %s)",
                    name, arguments.stream().map(TypedTerm::toString).collect(Collectors.joining(" ")));
        }

        /**
         * An SMT term associated with a type, used for the arguments of a function application.
         *
         * @param type The argument type.
         * @param term The subterm being passed as an argument.
         */
        public static record TypedTerm(String type, SmtTerm term) {

            @Override
            public String toString() {
                return term.toString();
            }

        }

    }

    /**
     * Represents a quantified subterm of an SMT formula.
     *
     * @param type     The quantifier type.
     * @param bindings The variables bound by the quantifier.
     * @param child    The subterm.
     */
    record Quantifier(Type type, List<TypedVar> bindings, SmtTerm child) implements SmtTerm {

        /**
         * Represents a type of quantifier.
         */
        public enum Type {
            /**
             * The existential quantifier.
             */
            EXISTS("∃"),

            /**
             * The universal quantifier.
             */
            FOR_ALL("∀");

            /**
             * The symbol representing the quantifier.
             */
            public final String symbol;

            /**
             * Constructs a quantifier type.
             *
             * @param symbol The symbol representing the quantifier.
             */
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

    /**
     * Represents a variable in an SMT formula.
     *
     * @param name The name of the variable.
     * @param type The name of the type of the variable.
     */
    record Variable(String name, String type) implements SmtTerm {

        @Override
        public String toString() {
            return name;
        }

    }

    /**
     * Represents a string constant in an SMT formula.
     *
     * @param value The value of the string constant.
     */
    record CString(String value) implements SmtTerm {

        @Override
        public String toString() {
            return "\"" + value + "\"";
        }

    }

    /**
     * Represents a numeric constant in an SMT formula.
     *
     * @param value the value of the numeric constant.
     */
    record CNumber(long value) implements SmtTerm { // TODO should this be a decimal type?

        @Override
        public String toString() {
            return Long.toString(value);
        }

    }

}
