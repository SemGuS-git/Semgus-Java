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
                case "bitvector" -> deserializeBitVector(termDto); // a bit vector
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
        // deserialize function identifier
        Object idDto = JsonUtils.get(termDto, "name");
        Identifier id;
        try {
            id = Identifier.deserialize(idDto);
        } catch (DeserializationException e) {
            throw e.prepend("name");
        }

        // deserialize return type identifier
        Object returnTypeDto = JsonUtils.get(termDto, "returnSort");
        Identifier returnType;
        try {
            returnType = Identifier.deserialize(returnTypeDto);
        } catch (DeserializationException e) {
            throw e.prepend("returnSort");
        }

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
                String name = JsonUtils.getString(bindingDto, "name");
                Object typeDto = JsonUtils.get(bindingDto, "sort");
                Identifier type;
                try {
                    type = Identifier.deserialize(typeDto);
                } catch (DeserializationException e) {
                    throw e.prepend("sort");
                }
                bindings[i] = new TypedVar(name, type);
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
        String name = JsonUtils.getString(termDto, "name");
        Object typeDto = JsonUtils.get(termDto, "sort");
        Identifier type;
        try {
            type = Identifier.deserialize(typeDto);
        } catch (DeserializationException e) {
            throw e.prepend("sort");
        }
        return new Variable(name, type);
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
        public static record TypedTerm(Identifier type, SmtTerm term) {

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
            StringBuilder sb = new StringBuilder("<");
            for (int i = size - 1; i >= 0; i--) {
                sb.append(value.get(i) ? '1' : '0');
            }
            return sb.append(">").toString();
        }

    }

}
