package co.arago.hiro.client.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class JsonTools {
    /**
     * Does not serialize values that are set to 'null' in an json-object to the output.
     */
    public static final Integer SKIP_NULL_MAP_VALUES = 0x01;
    /**
     * Ignore properties on deserialization that are not used to construct an object.
     */
    public static final Integer FAIL_ON_UNKNOWN_PROPERTIES = 0x02;
    /**
     * Allow created beans to be empty
     */
    public static final Integer FAIL_ON_EMPTY_BEANS = 0x04;
    /**
     * Try your best to parse even illegal JSON.
     */
    public static final Integer LENIENT_PARSING = 0x08;
    /**
     * Escape all non-ascii characters, even UTF8
     */
    public static final Integer ESCAPE_NON_ASCII = 0x10;

    /**
     * The default instance of the JsonTools
     */
    public static final JsonTools DEFAULT = new JsonTools();

    /**
     * An instance of the JsonTools that fails on unknown properties.
     * Better suited to deserialize objects from Json, i.e. a configuration file.
     */
    public static final JsonTools STRICT = new JsonTools(FAIL_ON_UNKNOWN_PROPERTIES);

    /**
     * An instance of the JsonTools that skips serializing null values.
     */
    public static final JsonTools SKIP_NULL = new JsonTools(SKIP_NULL_MAP_VALUES);

    /**
     * Json mapper who does the mapping
     */
    private final JsonMapper mapper;

    /**
     * Constructor<br/>
     * FAIL_ON_UNKNOWN_PROPERTIES: off<br/>
     * SKIP_NULL_MAP_VALUES: off<br/>
     * ALLOW_EMPTY_BEANS: on<br/>
     * ESCAPE_NON_ASCII: off<br/>
     * LENIENT_PARSING: off<br/>
     * SKIP_JDK8_TYPES: off<br/>
     * WARN_ON_UNKNOWN_PROPERTIES: off<br/>
     */
    private JsonTools() {
        JsonMapper.Builder builder = JsonMapper.builder();

        builder.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        builder.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        builder.defaultPrettyPrinter(PrettyPrinterFactory.createDefault());

        mapper = builder.build();
    }

    /**
     * Constructor.
     *
     * @param options The options to set. <br/>
     *                FAIL_ON_UNKNOWN_PROPERTIES<br/>
     *                SKIP_NULL_MAP_VALUES<br/>
     *                FAIL_ON_EMPTY_BEANS<br/>
     *                LENIENT_PARSING<br/>
     *                ESCAPE_NON_ASCII<br/>
     *                SKIP_JDK8_TYPES<br/>
     *                WARN_ON_UNKNOWN_PROPERTIES<br/>
     */
    private JsonTools(Integer options) {
        JsonMapper.Builder builder = JsonMapper.builder();

        boolean failUnknown = (options & FAIL_ON_UNKNOWN_PROPERTIES) == FAIL_ON_UNKNOWN_PROPERTIES;
        boolean skip = (options & SKIP_NULL_MAP_VALUES) == SKIP_NULL_MAP_VALUES;
        boolean failEmptyBeans = (options & FAIL_ON_EMPTY_BEANS) == FAIL_ON_EMPTY_BEANS;
        boolean lenientParsing = (options & LENIENT_PARSING) == LENIENT_PARSING;
        boolean escapeNonAscii = (options & ESCAPE_NON_ASCII) == ESCAPE_NON_ASCII;

        if (lenientParsing) {
            builder.configure(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            builder.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, true);
            builder.configure(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
            builder.configure(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS, true);
            builder.configure(JsonReadFeature.ALLOW_SINGLE_QUOTES, true);
        }

        builder.configure(JsonWriteFeature.ESCAPE_NON_ASCII, escapeNonAscii);
        builder.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failUnknown);
        builder.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, failEmptyBeans);

        builder.defaultPrettyPrinter(PrettyPrinterFactory.createDefault());

        if (skip)
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        mapper = builder.build();
    }

    /**
     * Parse string into JsonNode tree
     *
     * @param data String data
     * @return The JsonNode containing the data
     * @throws JsonProcessingException On error
     */
    public JsonNode readTree(String data) throws JsonProcessingException {
        return mapper.readTree(data);
    }

    /**
     * Parse stream into JsonNode tree
     *
     * @param stream A stream to read from
     * @return The JsonNode containing the data
     * @throws IOException On IO error
     */
    public JsonNode readTree(InputStream stream) throws IOException {
        return mapper.readTree(stream);
    }

    /**
     * Parse File into JsonNode tree
     *
     * @param file File to read
     * @return The JsonNode containing the data
     * @throws IOException On IO error
     */
    public JsonNode readTree(File file) throws IOException {
        return mapper.readTree(file);
    }

    /**
     * Transforms a JSON structure read from an inputStream into an Object using injectMap
     * as additional parameters.
     *
     * @param inputStream Input stream with data
     * @param targetClass The target type of object to create
     * @param injectMap   Map with additional inject values
     * @return The created Object
     * @throws IOException If something else goes wrong
     */
    public <T> T toObject(InputStream inputStream, Class<T> targetClass, Map<String, Object> injectMap) throws IOException {
        return mapper.reader(new InjectableValues.Std(injectMap)).forType(targetClass).readValue(inputStream);
    }

    /**
     * Transforms a JSON structure read from an inputStream into an Object.
     *
     * @param inputStream Input stream with data
     * @param targetClass The target type of object to create
     * @return The created Object
     * @throws IOException If something else goes wrong
     */
    public <T> T toObject(InputStream inputStream, Class<T> targetClass) throws IOException {
        return mapper.readValue(inputStream, targetClass);
    }

    /**
     * Transforms a JSON structure read from a file into an Object using injectMap
     * as additional parameters.
     *
     * @param file        The file with the JSON structure to convert
     * @param targetClass The target type of object to create
     * @param injectMap   Map with additional inject values
     * @return The created Object
     * @throws IOException If something else goes wrong
     */
    public <T> T toObject(File file, Class<T> targetClass, Map<String, Object> injectMap) throws IOException {
        return mapper.reader(new InjectableValues.Std(injectMap)).forType(targetClass).readValue(file);
    }

    /**
     * Transforms a JSON structure read from a file into an Object.
     *
     * @param file        The file with the JSON structure to convert
     * @param targetClass The target type of object to create
     * @return The created Object
     * @throws IOException If something else goes wrong
     */
    public <T> T toObject(File file, Class<T> targetClass) throws IOException {
        return mapper.readValue(file, targetClass);
    }

    /**
     * Transforms a JSON structure given as String into an Object.
     *
     * @param json        The JSON structure to convert (String)
     * @param targetClass The target type of object to create
     * @return The created Object
     * @throws JsonProcessingException On Json Error
     */
    public <T> T toObject(String json, Class<T> targetClass) throws JsonProcessingException {
        return mapper.readValue(json, targetClass);
    }

    /**
     * Transforms a JSON structure given as String into an Object.
     *
     * @param json      The JSON structure to convert (String)
     * @param classname The target type of object to create given as string
     * @return The created Object
     * @throws ClassNotFoundException When the classname is no name of a valid class
     */
    public Object toObject(String json, String classname) throws ClassNotFoundException, JsonProcessingException {
        return toObject(json, (Class<?>) Class.forName(classname));
    }

    /**
     * Transforms a JSON structure given as Object into an Object.
     *
     * @param json        The JSON structure to convert (String)
     * @param targetClass The target type of object to create
     * @param injectMap   Map with additional inject values
     * @return The created Object
     */
    public <T> T toObject(String json, Class<T> targetClass, Map<String, Object> injectMap) throws JsonProcessingException {
        return mapper.reader(new InjectableValues.Std(injectMap)).forType(targetClass).readValue(json);
    }

    /**
     * Transforms a JSON structure given as Object into an Object.
     *
     * @param json        The JSON structure to convert (Object)
     * @param targetClass The target type of object to create
     * @param injectMap   Map with additional inject values
     * @return The created Object
     */
    public <T> T toObject(Object json, Class<T> targetClass, Map<String, Object> injectMap) {
        JsonMapper copy = mapper.copy();
        copy.setInjectableValues(new InjectableValues.Std(injectMap));
        return copy.convertValue(json, targetClass);
    }

    /**
     * Transforms a JSON structure given as Object into an Object.
     *
     * @param json      The JSON structure to convert (Object)
     * @param className The target type of object to create given as string
     * @return The created Object
     * @throws ClassNotFoundException When the className is no name of a valid class
     */
    public Object toObject(Object json, String className) throws ClassNotFoundException {
        return toObject(json, (Class<?>) Class.forName(className));
    }

    /**
     * Transforms a JSON structure given as Object into an Object.
     *
     * @param json        The JSON structure to convert (Object)
     * @param targetClass The target type of object to create
     * @return The created Object
     */
    public <T> T toObject(Object json, Class<T> targetClass) {
        return mapper.convertValue(json, targetClass);
    }

    /**
     * Transforms a JSON structure given as Object into an Object cast by
     * TypeReference.
     *
     * @param json          The JSON structure to convert (Object)
     * @param typeReference The target type of object to create
     * @return The created Object
     */
    public <T> T toObject(Object json, TypeReference<T> typeReference) {
        return mapper.convertValue(json, typeReference);
    }

    /**
     * Transforms a JSON structure given as Object into an Object cast by
     * TypeReference.
     *
     * @param inputStream   Input stream with data
     * @param typeReference The target type of object to create
     * @return The created Object
     */
    public <T> T toObject(InputStream inputStream, TypeReference<T> typeReference) {
        return mapper.convertValue(inputStream, typeReference);
    }

    /**
     * Transforms a JSON structure given as String into an Object cast by
     * TypeReference.
     *
     * @param json          The JSON structure to convert (String)
     * @param typeReference The target type of object to create
     * @return The created Object
     * @throws JsonProcessingException On Json Error
     */
    public <T> T toObject(String json, TypeReference<T> typeReference) throws JsonProcessingException {
        return mapper.readValue(json, typeReference);
    }

    /**
     * Write an object as json string
     *
     * @param json The object
     * @return Object converted to a json string
     * @throws JsonProcessingException If the object cannot be converted to a
     *                                 string.
     */
    public String toString(Object json) throws JsonProcessingException {
        return mapper.writeValueAsString(json);
    }

    /**
     * Write an object as pretty json string
     *
     * @param json The object
     * @return Object converted to a json string
     * @throws JsonProcessingException If the object cannot be converted to a
     *                                 string.
     */
    public String toPrettyString(Object json) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    }

    /**
     * Prettify a json string
     *
     * @param json The json string
     * @return The prettified json string
     * @throws IOException If the string is no valid JSON.
     */
    public String toPrettyString(String json) throws IOException {
        return toPrettyString(toPOJO(json));
    }

    /**
     * Read an InputStream with json data and return a prettified string.
     *
     * @param stream The stream with json data
     * @return The prettified json string
     * @throws IOException If the inputStream contains no valid JSON.
     */
    public String toPrettyString(InputStream stream) throws IOException {
        return toPrettyString(toPOJO(stream));
    }

    /**
     * Read a File with json data and return a prettified string.
     *
     * @param file The file with json data
     * @return The prettified json string
     * @throws IOException If the file contains no valid JSON.
     */
    public String toPrettyString(File file) throws IOException {
        return toPrettyString(toPOJO(file));
    }

    /**
     * Transforms a JSON structure given as JsonNode into an Object.
     *
     * @param json        The JSON structure to convert (JsonNode)
     * @param targetClass The target type of object to create
     * @return The created Object
     * @throws JsonProcessingException On processing error
     */
    public <T> T toObject(JsonNode json, Class<T> targetClass) throws JsonProcessingException {
        return mapper.treeToValue(json, targetClass);
    }

    /**
     * Transforms a JSON structure given as JsonNode into an Object.
     *
     * @param json      The JSON structure to convert (JsonNode)
     * @param className The target type of object to create
     * @return The created Object
     * @throws ClassNotFoundException  When the className is not the name of a valid class.
     * @throws JsonProcessingException On processing error
     */
    public Object toObject(JsonNode json, String className) throws ClassNotFoundException, JsonProcessingException {
        return mapper.treeToValue(json, (Class<?>) Class.forName(className));
    }

    /**
     * This function tries to transform either a String or any Object into another object via Jackson.
     *
     * @param json      The json data. String or any Object. String uses {@link JsonTools#toObject(String, String)},
     *                  everything else uses {@link JsonTools#toObject(Object, String)}. If the string is blank, null is returned.
     * @param className Name of the resulting class that shall be created. The default is {@link Map}.
     * @return The generated object or null if no object can be created (String is blank for instance).
     * @throws ClassNotFoundException  When the className is not the name of a valid class.
     * @throws JsonProcessingException On processing error
     */
    public Object toObjectEx(Object json, String className) throws ClassNotFoundException, JsonProcessingException {
        if (json instanceof String) {
            String str = (String) json;
            return str.isBlank() ? null : toObject(str, (Class<?>) Class.forName(className));
        } else {
            return toObject(json, (Class<?>) Class.forName(className));
        }

    }

    /**
     * This function tries to transform either a String or any Object into another object via Jackson.
     *
     * @param json The json data. String or any Object. String uses {@link JsonTools#toObject(String, String)},
     *             everything else uses {@link JsonTools#toObject(Object, String)}. If the string is blank, null is returned.
     * @return The generated object or null if no object can be created (String is blank for instance).
     */
    public Object toObjectEx(Object json) throws IOException, ClassNotFoundException {
        return toObjectEx(json, "java.util.Map");
    }

    /**
     * Create a POJO structure from a string
     *
     * @param string String with JSON
     * @return The POJO Object
     * @throws JsonProcessingException On errors with JSON conversion
     */
    public Object toPOJO(String string) throws JsonProcessingException {
        return toObject(string, Object.class);
    }

    /**
     * Create a POJO structure from a file containing JSON
     *
     * @param file File with JSON data
     * @return The POJO Object
     * @throws IOException On errors with JSON conversion
     */
    public Object toPOJO(File file) throws IOException {
        return toObject(file, Object.class);
    }

    /**
     * Create a POJO structure from an InputStream containing JSON
     *
     * @param inputStream InputStream with JSON data
     * @return The POJO Object
     * @throws IOException On errors with JSON conversion
     */
    public Object toPOJO(InputStream inputStream) throws IOException {
        return toObject(inputStream, Object.class);
    }
}

