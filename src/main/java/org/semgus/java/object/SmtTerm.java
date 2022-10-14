package org.semgus.java.object;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.semgus.java.util.DeserializationException;
import org.semgus.java.util.JsonUtils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a term of an SMT formula.
 */
public sealed interface SmtTerm {

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
                case "lambda" -> deserializeLambda(termDto); // a lambda abstraction
                case "match" -> deserializeMatch(termDto); // a pattern matching expression
                case "variable" -> deserializeVariable(termDto); // a variable
                case "bitvector" -> deserializeBitVector(termDto); // a bit vector
                default -> throw new DeserializationException(
                        String.format("Unknown term type \"%s\"", termType), "$termType");
            };
        }
        throw new DeserializationException(String.format("Could not deserialize SMT term \"%s\"", termDtoRaw));
    }

    /**
     * Deserializes an SMT term from the SemGuS JSON format at a given key in a parent JSON object.
     *
     * @param parentDto The parent JSON object.
     * @param key       The key whose value should be deserialized.
     * @return The deserialized SMT term.
     * @throws DeserializationException If the value at {@code key} is not a valid representation of an SMT term.
     */
    static SmtTerm deserializeAt(JSONObject parentDto, String key) throws DeserializationException {
        Object termDto = JsonUtils.get(parentDto, key);
        try {
            return deserialize(termDto);
        } catch (DeserializationException e) {
            throw e.prepend(key);
        }
    }

    /**
     * Deserializes a function application.
     *
     * @param termDto The JSON representation of the function application.
     * @return The deserialized function application.
     * @throws DeserializationException If {@code termDto} is not a valid representation of a function application.
     */
    private static SmtTerm deserializeApplication(JSONObject termDto) throws DeserializationException {
        // deserialize function and return type identifiers
        Identifier id = Identifier.deserializeAt(termDto, "name");
        Identifier returnType = Identifier.deserializeAt(termDto, "returnSort");

        // zip together argument terms and argument types
        JSONArray argTypes = JsonUtils.getArray(termDto, "argumentSorts");
        JSONArray args = JsonUtils.getArray(termDto, "arguments");
        if (argTypes.size() != args.size()) {
            throw new DeserializationException(String.format(
                    "Argument sorts and arguments of SMT function application have different lengths %d != %d",
                    argTypes.size(), args.size()));
        }
        Application.TypedTerm[] argTerms = new Application.TypedTerm[argTypes.size()];
        for (int i = 0; i < argTerms.length; i++) {
            // deserialize type
            Identifier type;
            try {
                type = Identifier.deserialize(argTypes.get(i));
            } catch (DeserializationException e) {
                throw e.prepend("argumentSorts." + i);
            }

            // deserialize term
            SmtTerm term;
            try {
                term = deserialize(args.get(i));
            } catch (DeserializationException e) {
                throw e.prepend("arguments." + i);
            }

            argTerms[i] = new Application.TypedTerm(type, term);
        }

        return new Application(id, returnType, Arrays.asList(argTerms));
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
                bindings[i] = new TypedVar(JsonUtils.getString(bindingDto, "name"),
                        Identifier.deserializeAt(bindingDto, "sort"));
            } catch (DeserializationException e) {
                throw e.prepend("bindings." + i);
            }
        }

        // deserialize child term
        SmtTerm child = deserializeAt(termDto, "child");

        return new Quantifier(qType, Arrays.asList(bindings), child);
    }

    /**
     * Deserializes a lambda abstraction.
     *
     * @param termDto The JSON representation of the lambda abstraction.
     * @return The deserialized lambda abstraction.
     * @throws DeserializationException If {@code termDto} is not a valid representation of lambda abstraction.
     */
    private static SmtTerm deserializeLambda(JSONObject termDto) throws DeserializationException {
        return new Lambda(JsonUtils.getStrings(termDto, "arguments"), deserializeAt(termDto, "body"));
    }

    /**
     * Deserializes a pattern-matching expression.
     *
     * @param termDto The JSON representation of the pattern-matching expression.
     * @return The deserialized pattern-matching expression.
     * @throws DeserializationException If {@code termDto} is not a valid representation of a pattern match.
     */
    private static SmtTerm deserializeMatch(JSONObject termDto) throws DeserializationException {
        SmtTerm matchTerm = deserializeAt(termDto, "term");
        List<JSONObject> casesDto = JsonUtils.getObjects(termDto, "binders");

        Match.Case[] cases = new Match.Case[casesDto.size()];
        for (int i = 0; i < cases.length; i++) {
            JSONObject caseDto = casesDto.get(i);
            try {
                cases[i] = new Match.Case(
                        JsonUtils.getString(caseDto, "operator"),
                        JsonUtils.getStrings(caseDto, "arguments"),
                        deserializeAt(caseDto, "child"));
            } catch (DeserializationException e) {
                throw e.prepend("binders." + i);
            }
        }

        return new Match(matchTerm, Arrays.asList(cases));
    }

    /**
     * Deserializes a variable.
     *
     * @param termDto The JSON representation of the variable.
     * @return The deserialized variable.
     * @throws DeserializationException If {@code termDto} is not a valid representation of a variable.
     */
    private static SmtTerm deserializeVariable(JSONObject termDto) throws DeserializationException {
        return new Variable(JsonUtils.getString(termDto, "name"), Identifier.deserializeAt(termDto, "sort"));
    }

    /**
     * Deserializes a bit vector constant.
     *
     * @param termDto The JSON representation of the bit vector constant.
     * @return The deserialized bit vector constant.
     * @throws DeserializationException If {@code termDto} is not a valid representation of a bit vector constant.
     */
    private static SmtTerm deserializeBitVector(JSONObject termDto) throws DeserializationException {
        // deserialize size
        int size = JsonUtils.getInt(termDto, "size");
        if (size < 0) {
            throw new DeserializationException("Bit vector size must be non-negative!", "size");
        }

        // deserialize the bit field string
        String bitVecStr = JsonUtils.getString(termDto, "value");
        if (!bitVecStr.startsWith("0x")) {
            throw new DeserializationException("Bit vector value must start with \"0x\"!", "value");
        }
        int bitVecStrLen = bitVecStr.length();
        int unpaddedByteCount = bitVecStrLen / 2 - 1; // minus one to account for the "0x"

        // parse out the bit field
        byte[] bitField;
        try {
            if ((bitVecStrLen % 2) == 1) { // hex string is of odd length; need to pad the most significant byte
                bitField = new byte[unpaddedByteCount + 1];
                bitField[unpaddedByteCount] = (byte)readHexChar(bitVecStr.charAt(2));
            } else { // hex string is of even length; no bytes need to be padded
                bitField = new byte[unpaddedByteCount];
            }
            for (int i = 0; i < unpaddedByteCount; i++) {
                bitField[i] = (byte)(readHexChar(bitVecStr.charAt(bitVecStrLen - i * 2 - 1))
                        | (readHexChar(bitVecStr.charAt(bitVecStrLen - i * 2 - 2)) << 4));
            }
        } catch (DeserializationException e) {
            throw e.prepend("value");
        }
        BitSet bitVecValue = BitSet.valueOf(bitField);
        if (bitVecValue.nextSetBit(size) != -1) { // ensure there are no set bits beyond the vector size
            throw new DeserializationException("Bit vector value is wider than bit vector size!");
        }

        return new CBitVector(size, bitVecValue);
    }

    /**
     * Parses a hexadecimal character into its corresponding numeric value.
     *
     * @param hexChar The hexadecimal character.
     * @return The numeric value of {@code hexChar},
     * @throws DeserializationException If {@code hexChar} is not a valid hexadecimal character.
     */
    private static int readHexChar(char hexChar) throws DeserializationException {
        return switch (hexChar) {
            case '0' -> 0x0;
            case '1' -> 0x1;
            case '2' -> 0x2;
            case '3' -> 0x3;
            case '4' -> 0x4;
            case '5' -> 0x5;
            case '6' -> 0x6;
            case '7' -> 0x7;
            case '8' -> 0x8;
            case '9' -> 0x9;
            case 'a', 'A' -> 0xA;
            case 'b', 'B' -> 0xB;
            case 'c', 'C' -> 0xC;
            case 'd', 'D' -> 0xD;
            case 'e', 'E' -> 0xE;
            case 'f', 'F' -> 0xF;
            default -> throw new DeserializationException("Not a valid hexadecimal character: " + hexChar);
        };
    }

    /**
     * Represents a function application in an SMT formula.
     *
     * @param name       The identifier for the function.
     * @param returnType The function's return type.
     * @param arguments  The arguments to the function.
     */
    record Application(Identifier name, Identifier returnType, List<TypedTerm> arguments) implements SmtTerm {

        @Override
        public String toString() {
            return arguments.isEmpty() ? name.toString() : String.format("(%s %s)",
                    name, arguments.stream().map(TypedTerm::toString).collect(Collectors.joining(" ")));
        }

        /**
         * An SMT term associated with a type, used for the arguments of a function application.
         *
         * @param type The argument type.
         * @param term The subterm being passed as an argument.
         */
        public record TypedTerm(Identifier type, SmtTerm term) {

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
            EXISTS("∃", "exists"),

            /**
             * The universal quantifier.
             */
            FOR_ALL("∀", "forall");

            /**
             * The symbol representing the quantifier.
             */
            public final String symbol, name;

            /**
             * Constructs a quantifier type.
             *
             * @param symbol The symbol representing the quantifier.
             */
            Type(String symbol, String name) {
                this.symbol = symbol;
                this.name = name;
            }

            @Override
            public String toString() {
                return name;
            }
        }

        @Override
        public String toString() {
            return String.format("(%s (%s) %s)",
                    type,
                    bindings.stream().map(TypedVar::toString).collect(Collectors.joining(" ")),
                    child);
        }

    }

    /**
     * Represents a lambda abstraction in an SMT formula.
     *
     * @param arguments The names of the lambda arguments. Beware of conflicts with variables in the outer context!
     * @param body      The body of the lambda term, which may contain the arguments as bound variables.
     */
    record Lambda(List<String> arguments, SmtTerm body) implements SmtTerm {

        @Override
        public String toString() {
            return String.format("(lambda (%s) %s)", String.join(" ", arguments), body);
        }

    }

    /**
     * Represents a pattern-matching expression in an SMT formula. Used to match against constructors for inductive
     * types as defined by {@link org.semgus.java.event.SmtSpecEvent.DefineDatatypeEvent}.
     *
     * @param matchTerm The term being matched on.
     * @param cases     The match cases.
     */
    record Match(SmtTerm matchTerm, List<Case> cases) implements SmtTerm {

        @Override
        public String toString() {
            return String.format("(match %s (%s))",
                    matchTerm,
                    cases.stream().map(Case::toString).collect(Collectors.joining(" ")));
        }

        /**
         * A match case in a {@link Match} pattern-matching expression.
         *
         * @param opName    The name of the operator to match against.
         * @param arguments The names to which the operator's arguments should be bound in the result term.
         * @param result    The match result.
         */
        public record Case(String opName, List<String> arguments, SmtTerm result) {

            @Override
            public String toString() {
                return arguments.isEmpty() ? String.format("(%s %s)", opName, result)
                        : String.format("((%s %s) %s)", opName, String.join(" ", arguments), result);
            }

        }

    }

    /**
     * Represents a variable in an SMT formula.
     *
     * @param name The name of the variable.
     * @param type The identifier for the type of the variable.
     */
    record Variable(String name, Identifier type) implements SmtTerm {

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
     * @param value The value of the numeric constant.
     */
    record CNumber(long value) implements SmtTerm { // TODO should this be a decimal type?

        @Override
        public String toString() {
            return Long.toString(value);
        }

    }

    /**
     * Represents a bit vector constant in an SMT formula.
     *
     * @param size  The width of the bit vector.
     * @param value The value of the bit vector constant.
     */
    record CBitVector(int size, BitSet value) implements SmtTerm {

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("#b");
            for (int i = size - 1; i >= 0; i--) {
                sb.append(value.get(i) ? '1' : '0');
            }
            return sb.toString();
        }

    }

}
