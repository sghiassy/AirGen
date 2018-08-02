package io.swagger.codegen.languages;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import java.lang.Double;

import io.swagger.codegen.CliOption;
import io.swagger.codegen.CodegenConfig;
import io.swagger.codegen.CodegenConstants;
import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.DefaultCodegen;
import io.swagger.codegen.SupportingFile;

import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.CookieParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.SerializableParameter;
import io.swagger.models.properties.AbstractNumericProperty;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.BaseIntegerProperty;
import io.swagger.models.properties.BinaryProperty;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.ByteArrayProperty;
import io.swagger.models.properties.DateProperty;
import io.swagger.models.properties.DateTimeProperty;
import io.swagger.models.properties.DecimalProperty;
import io.swagger.models.properties.DoubleProperty;
import io.swagger.models.properties.FileProperty;
import io.swagger.models.properties.FloatProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.LongProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.PropertyBuilder;
import io.swagger.models.properties.PropertyBuilder.PropertyId;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.models.properties.UUIDProperty;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AirGenCodegen extends DefaultCodegen implements CodegenConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AirGenCodegen.class);

    public static final String PROJECT_NAME = "projectName";
    public static final String RESPONSE_AS = "responseAs";
    public static final String UNWRAP_REQUIRED = "unwrapRequired";
    public static final String OBJC_COMPATIBLE = "objcCompatible";
    public static final String POD_SOURCE = "podSource";
    public static final String POD_AUTHORS = "podAuthors";
    public static final String POD_SOCIAL_MEDIA_URL = "podSocialMediaURL";
    public static final String POD_DOCSET_URL = "podDocsetURL";
    public static final String POD_LICENSE = "podLicense";
    public static final String POD_HOMEPAGE = "podHomepage";
    public static final String POD_SUMMARY = "podSummary";
    public static final String POD_DESCRIPTION = "podDescription";
    public static final String POD_SCREENSHOTS = "podScreenshots";
    public static final String POD_DOCUMENTATION_URL = "podDocumentationURL";
    public static final String SWIFT_USE_API_NAMESPACE = "swiftUseApiNamespace";
    public static final String DEFAULT_POD_AUTHORS = "Swagger Codegen";
    public static final String LENIENT_TYPE_CAST = "lenientTypeCast";
    protected static final String LIBRARY_PROMISE_KIT = "PromiseKit";
    protected static final String LIBRARY_RX_SWIFT = "RxSwift";
    protected static final String[] RESPONSE_LIBRARIES = { LIBRARY_PROMISE_KIT, LIBRARY_RX_SWIFT };
    protected String projectName = "AirGen";
    protected boolean unwrapRequired;
    protected boolean objcCompatible = false;
    protected boolean lenientTypeCast = false;
    protected boolean swiftUseApiNamespace;
    protected String[] responseAs = new String[0];
    protected String sourceFolder = "";

    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    @Override
    public String getName() {
        return "airgen";
    }

    @Override
    public String getHelp() {
        return "Generates AirGap interfaces based on Smart Contracts.";
    }

    @Override
    protected void addAdditionPropertiesToCodeGenModel(CodegenModel codegenModel, ModelImpl swaggerModel) {

        final Property additionalProperties = swaggerModel.getAdditionalProperties();

        if (additionalProperties != null) {
            codegenModel.additionalPropertiesType = getSwaggerType(additionalProperties);
        }
    }

    /**
     * Constructor for the airgen language codegen module.
     */
    public AirGenCodegen() {
        super();
        outputFolder = "generated-code" + File.separator + "swift";
        modelTemplateFiles.put("model.mustache", ".swift");
        apiTemplateFiles.put("api.mustache", ".swift");
        embeddedTemplateDir = templateDir = "airgen";
        apiPackage = File.separator + "APIs";
        modelPackage = File.separator + "Models";

        languageSpecificPrimitives = new HashSet<>(Arrays.asList("Int", "Int32", "Int64", "Float", "Double", "Bool",
                "Void", "String", "Character", "AnyObject", "Any"));
        defaultIncludes = new HashSet<>(Arrays.asList("Data", "Date", "URL", // for file
                "UUID", "Array", "Dictionary", "Set", "Any", "Empty", "AnyObject", "Any"));
        reservedWords = new HashSet<>(Arrays.asList(
                // name used by swift client
                "ErrorResponse", "Response",

                // Added for Objective-C compatibility
                "id", "description", "NSArray", "NSURL", "CGFloat", "NSSet", "NSString", "NSInteger", "NSUInteger",
                "NSError", "NSDictionary",

                //
                // Swift keywords. This list is taken from here:
                // https://developer.apple.com/library/content/documentation/Swift/Conceptual/Swift_Programming_Language/LexicalStructure.html#//apple_ref/doc/uid/TP40014097-CH30-ID410
                //
                // Keywords used in declarations
                "associatedtype", "class", "deinit", "enum", "extension", "fileprivate", "func", "import", "init",
                "inout", "internal", "let", "open", "operator", "private", "protocol", "public", "static", "struct",
                "subscript", "typealias", "var",
                // Keywords uses in statements
                "break", "case", "continue", "default", "defer", "do", "else", "fallthrough", "for", "guard", "if",
                "in", "repeat", "return", "switch", "where", "while",
                // Keywords used in expressions and types
                "as", "Any", "catch", "false", "is", "nil", "rethrows", "super", "self", "Self", "throw", "throws",
                "true", "try",
                // Keywords used in patterns
                "_",
                // Keywords that begin with a number sign
                "#available", "#colorLiteral", "#column", "#else", "#elseif", "#endif", "#file", "#fileLiteral",
                "#function", "#if", "#imageLiteral", "#line", "#selector", "#sourceLocation",
                // Keywords reserved in particular contexts
                "associativity", "convenience", "dynamic", "didSet", "final", "get", "infix", "indirect", "lazy",
                "left", "mutating", "none", "nonmutating", "optional", "override", "postfix", "precedence", "prefix",
                "Protocol", "required", "right", "set", "Type", "unowned", "weak", "willSet",

                //
                // Swift Standard Library types
                // https://developer.apple.com/documentation/swift
                //
                // Numbers and Basic Values
                "Bool", "Int", "Double", "Float", "Range", "ClosedRange", "Error", "Optional",
                // Special-Use Numeric Types
                "UInt", "UInt8", "UInt16", "UInt32", "UInt64", "Int8", "Int16", "Int32", "Int64", "Float80", "Float32",
                "Float64",
                // Strings and Text
                "String", "Character", "Unicode", "StaticString",
                // Collections
                "Array", "Dictionary", "Set", "OptionSet", "CountableRange", "CountableClosedRange",

                // The following are commonly-used Foundation types
                "URL", "Data", "Codable", "Encodable", "Decodable",

                // The following are other words we want to reserve
                "Void", "AnyObject", "Class", "dynamicType", "COLUMN", "FILE", "FUNCTION", "LINE"));

        typeMapping = new HashMap<>();
        typeMapping.put("array", "Array");
        typeMapping.put("List", "Array");
        typeMapping.put("map", "Dictionary");
        typeMapping.put("date", "Date");
        typeMapping.put("Date", "Date");
        typeMapping.put("DateTime", "Date");
        typeMapping.put("boolean", "Bool");
        typeMapping.put("string", "String");
        typeMapping.put("char", "Character");
        typeMapping.put("short", "Int");
        typeMapping.put("int", "Int");
        typeMapping.put("long", "Int64");
        typeMapping.put("integer", "Int");
        typeMapping.put("Integer", "Int");
        typeMapping.put("float", "Float");
        typeMapping.put("number", "Double");
        typeMapping.put("double", "Double");
        typeMapping.put("object", "Any");
        typeMapping.put("file", "URL");
        typeMapping.put("binary", "Data");
        typeMapping.put("ByteArray", "Data");
        typeMapping.put("UUID", "UUID");

        importMapping = new HashMap<>();

        cliOptions.add(new CliOption(PROJECT_NAME, "Project name in Xcode"));
        cliOptions.add(new CliOption(RESPONSE_AS, "Optionally use libraries to manage response.  Currently "
                + StringUtils.join(RESPONSE_LIBRARIES, ", ") + " are available."));
        cliOptions.add(new CliOption(UNWRAP_REQUIRED,
                "Treat 'required' properties in response as non-optional "
                        + "(which would crash the app if api returns null as opposed "
                        + "to required option specified in json schema"));
        cliOptions.add(new CliOption(OBJC_COMPATIBLE,
                "Add additional properties and methods for Objective-C " + "compatibility (default: false)"));
        cliOptions.add(new CliOption(POD_SOURCE, "Source information used for Podspec"));
        cliOptions.add(new CliOption(CodegenConstants.POD_VERSION, "Version used for Podspec"));
        cliOptions.add(new CliOption(POD_AUTHORS, "Authors used for Podspec"));
        cliOptions.add(new CliOption(POD_SOCIAL_MEDIA_URL, "Social Media URL used for Podspec"));
        cliOptions.add(new CliOption(POD_DOCSET_URL, "Docset URL used for Podspec"));
        cliOptions.add(new CliOption(POD_LICENSE, "License used for Podspec"));
        cliOptions.add(new CliOption(POD_HOMEPAGE, "Homepage used for Podspec"));
        cliOptions.add(new CliOption(POD_SUMMARY, "Summary used for Podspec"));
        cliOptions.add(new CliOption(POD_DESCRIPTION, "Description used for Podspec"));
        cliOptions.add(new CliOption(POD_SCREENSHOTS, "Screenshots used for Podspec"));
        cliOptions.add(new CliOption(POD_DOCUMENTATION_URL, "Documentation URL used for Podspec"));
        cliOptions.add(new CliOption(SWIFT_USE_API_NAMESPACE,
                "Flag to make all the API classes inner-class " + "of {{projectName}}API"));
        cliOptions.add(new CliOption(CodegenConstants.HIDE_GENERATION_TIMESTAMP,
                CodegenConstants.HIDE_GENERATION_TIMESTAMP_DESC).defaultValue(Boolean.TRUE.toString()));
        cliOptions.add(new CliOption(LENIENT_TYPE_CAST,
                "Accept and cast values for simple types (string->bool, " + "string->int, int->string)")
                        .defaultValue(Boolean.FALSE.toString()));
    }

    @Override
    public void processOpts() {
        super.processOpts();

        // Setup project name
        if (additionalProperties.containsKey(PROJECT_NAME)) {
            setProjectName((String) additionalProperties.get(PROJECT_NAME));
        } else {
            additionalProperties.put(PROJECT_NAME, projectName);
        }
        sourceFolder = projectName + File.separator + sourceFolder;

        // Setup unwrapRequired option, which makes all the
        // properties with "required" non-optional
        if (additionalProperties.containsKey(UNWRAP_REQUIRED)) {
            setUnwrapRequired(convertPropertyToBooleanAndWriteBack(UNWRAP_REQUIRED));
        }
        additionalProperties.put(UNWRAP_REQUIRED, unwrapRequired);

        // Setup objcCompatible option, which adds additional properties
        // and methods for Objective-C compatibility
        if (additionalProperties.containsKey(OBJC_COMPATIBLE)) {
            setObjcCompatible(convertPropertyToBooleanAndWriteBack(OBJC_COMPATIBLE));
        }
        additionalProperties.put(OBJC_COMPATIBLE, objcCompatible);

        // Setup unwrapRequired option, which makes all the properties with "required"
        // non-optional
        if (additionalProperties.containsKey(RESPONSE_AS)) {
            Object responseAsObject = additionalProperties.get(RESPONSE_AS);
            if (responseAsObject instanceof String) {
                setResponseAs(((String) responseAsObject).split(","));
            } else {
                setResponseAs((String[]) responseAsObject);
            }
        }
        additionalProperties.put(RESPONSE_AS, responseAs);
        if (ArrayUtils.contains(responseAs, LIBRARY_PROMISE_KIT)) {
            additionalProperties.put("usePromiseKit", true);
        }
        if (ArrayUtils.contains(responseAs, LIBRARY_RX_SWIFT)) {
            additionalProperties.put("useRxSwift", true);
        }

        // Setup swiftUseApiNamespace option, which makes all the API
        // classes inner-class of {{projectName}}API
        if (additionalProperties.containsKey(SWIFT_USE_API_NAMESPACE)) {
            setSwiftUseApiNamespace(convertPropertyToBooleanAndWriteBack(SWIFT_USE_API_NAMESPACE));
        }

        if (!additionalProperties.containsKey(POD_AUTHORS)) {
            additionalProperties.put(POD_AUTHORS, DEFAULT_POD_AUTHORS);
        }

        setLenientTypeCast(convertPropertyToBooleanAndWriteBack(LENIENT_TYPE_CAST));

        // supportingFiles.add(new SupportingFile("Podspec.mustache", "", projectName +
        // ".podspec"));
        // supportingFiles.add(new SupportingFile("Cartfile.mustache", "", "Cartfile"));
        // supportingFiles.add(new SupportingFile("APIHelper.mustache", sourceFolder,
        // "APIHelper.swift"));
        // supportingFiles.add(new SupportingFile("AlamofireImplementations.mustache",
        // sourceFolder,
        // "AlamofireImplementations.swift"));
        // supportingFiles.add(new SupportingFile("Configuration.mustache",
        // sourceFolder, "Configuration.swift"));
        // supportingFiles.add(new SupportingFile("Extensions.mustache", sourceFolder,
        // "Extensions.swift"));
        // supportingFiles.add(new SupportingFile("Models.mustache", sourceFolder,
        // "Models.swift"));
        // supportingFiles.add(new SupportingFile("APIs.mustache", sourceFolder,
        // "APIs.swift"));
        supportingFiles.add(new SupportingFile("APIHelpers.mustache", sourceFolder, "APIHelpers.swift"));
        supportingFiles.add(new SupportingFile("APIBaseObjects.mustache", sourceFolder, "APIBaseObjects.swift"));
        // supportingFiles.add(new SupportingFile("CodableHelper.mustache",
        // sourceFolder, "CodableHelper.swift"));
        // supportingFiles
        // .add(new SupportingFile("JSONEncodableEncoding.mustache", sourceFolder,
        // "JSONEncodableEncoding.swift"));
        // supportingFiles
        // .add(new SupportingFile("JSONEncodingHelper.mustache", sourceFolder,
        // "JSONEncodingHelper.swift"));
        // supportingFiles.add(new SupportingFile("git_push.sh.mustache", "",
        // "git_push.sh"));
        // supportingFiles.add(new SupportingFile("gitignore.mustache", "",
        // ".gitignore"));

    }

    @Override
    protected boolean isReservedWord(String word) {
        return word != null && reservedWords.contains(word); // don't lowercase as super does
    }

    @Override
    public String escapeReservedWord(String name) {
        if (this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name);
        }
        return "_" + name; // add an underscore to the name
    }

    @Override
    public String modelFileFolder() {
        return outputFolder + File.separator + sourceFolder + modelPackage().replace('.', File.separatorChar);
    }

    @Override
    public String apiFileFolder() {
        return outputFolder + File.separator + sourceFolder + apiPackage().replace('.', File.separatorChar);
    }

    @Override
    public String getTypeDeclaration(Property prop) {
        if (prop instanceof ArrayProperty) {
            ArrayProperty ap = (ArrayProperty) prop;
            Property inner = ap.getItems();
            return "[" + getTypeDeclaration(inner) + "]";
        } else if (prop instanceof MapProperty) {
            MapProperty mp = (MapProperty) prop;
            Property inner = mp.getAdditionalProperties();
            return "[String:" + getTypeDeclaration(inner) + "]";
        }
        return super.getTypeDeclaration(prop);
    }

    @Override
    public String getSwaggerType(Property prop) {
        String swaggerType = super.getSwaggerType(prop);
        String type;
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
            if (languageSpecificPrimitives.contains(type) || defaultIncludes.contains(type)) {
                return type;
            }
        } else {
            type = swaggerType;
        }
        return toModelName(type);
    }

    @Override
    public boolean isDataTypeFile(String dataType) {
        return dataType != null && dataType.equals("URL");
    }

    @Override
    public boolean isDataTypeBinary(final String dataType) {
        return dataType != null && dataType.equals("Data");
    }

    /**
     * Output the proper model name (capitalized).
     *
     * @param name the name of the model
     * @return capitalized model name
     */
    @Override
    public String toModelName(String name) {
        // FIXME parameter should not be assigned. Also declare it as "final"
        name = sanitizeName(name);

        if (!StringUtils.isEmpty(modelNameSuffix)) { // set model suffix
            name = name + "_" + modelNameSuffix;
        }

        if (!StringUtils.isEmpty(modelNamePrefix)) { // set model prefix
            name = modelNamePrefix + "_" + name;
        }

        // camelize the model name
        // phone_number => PhoneNumber
        name = camelize(name);

        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(name)) {
            String modelName = "Model" + name;
            LOGGER.warn(name + " (reserved word) cannot be used as model name. Renamed to " + modelName);
            return modelName;
        }

        // model name starts with number
        if (name.matches("^\\d.*")) {
            // e.g. 200Response => Model200Response (after camelize)
            String modelName = "Model" + name;
            LOGGER.warn(name + " (model name starts with number) cannot be used as model name." + " Renamed to "
                    + modelName);
            return modelName;
        }

        return name;
    }

    /**
     * Return the capitalized file name of the model.
     *
     * @param name the model name
     * @return the file name of the model
     */
    @Override
    public String toModelFilename(String name) {
        // should be the same as the model name
        return toModelName(name);
    }

    @Override
    public String toDefaultValue(Property p) {
        if (p instanceof StringProperty) {
            StringProperty sp = (StringProperty) p;
            if (sp.getDefault() != null) {
                return sp.getDefault();
            }
        } else if (p instanceof BooleanProperty) {
            BooleanProperty bp = (BooleanProperty) p;
            // LOGGER.info("shaheen: starting toDefaultValue.boolean for " + bp.getName());
            try {
                return bp.getDefault() == true ? "true" : "false";
            } catch (Exception e) {
                LOGGER.info("shaheen: could not get default value for boolean");
                return null;
            }
        } else if (p instanceof DateProperty) {
            // LOGGER.info("shaheen: date intance");
            // return "null";
            // no-op
        } else if (p instanceof DateTimeProperty) {
            // LOGGER.info("shaheen: datetime intance");
            // return "null";
            // no-op
        } else if (p instanceof IntegerProperty) {
            IntegerProperty dp = (IntegerProperty) p;
            // LOGGER.info("shaheen: starting toDefaultValue.IntegerProperty for " + dp);
            if (dp.getDefault() != null) {
                return dp.getDefault().toString();
            }
        } else if (p instanceof DoubleProperty) {
            DoubleProperty dp = (DoubleProperty) p;
            // LOGGER.info("shaheen: starting toDefaultValue.DoubleProperty for " + dp);
            if (dp.getDefault() != null) {
                return dp.getDefault().toString();
            }
        } else if (p instanceof FloatProperty) {
            FloatProperty dp = (FloatProperty) p;
            // LOGGER.info("shaheen: starting toDefaultValue.FloatProperty for " + dp);
            if (dp.getDefault() != null) {
                return dp.getDefault().toString();
            }
        } else if (p instanceof LongProperty) {
            LongProperty dp = (LongProperty) p;
            // LOGGER.info("shaheen: starting toDefaultValue.LongProperty for " + dp);
            if (dp.getDefault() != null) {
                return dp.getDefault().toString();
            }
        }

        // LOGGER.info("shaheen: could not find ANY instance type for " + p);

        return null;
    }

    @Override
    public String toInstantiationType(Property prop) {
        if (prop instanceof MapProperty) {
            MapProperty ap = (MapProperty) prop;
            String inner = getSwaggerType(ap.getAdditionalProperties());
            return inner;
        } else if (prop instanceof ArrayProperty) {
            ArrayProperty ap = (ArrayProperty) prop;
            String inner = getSwaggerType(ap.getItems());
            return "[" + inner + "]";
        }
        return null;
    }

    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            return "DefaultAPI";
        }
        return initialCaps(name) + "API";
    }

    @Override
    public String toOperationId(String operationId) {
        operationId = camelize(sanitizeName(operationId), true);

        // Throw exception if method name is empty.
        // This should not happen but keep the check just in case
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method name (operationId) not allowed");
        }

        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(operationId)) {
            String newOperationId = camelize(("call_" + operationId), true);
            LOGGER.warn(
                    operationId + " (reserved word) cannot be used as method name." + " Renamed to " + newOperationId);
            return newOperationId;
        }

        return operationId;
    }

    @Override
    public String toVarName(String name) {
        // sanitize name
        name = sanitizeName(name);

        // if it's all uppper case, do nothing
        if (name.matches("^[A-Z_]*$")) {
            return name;
        }

        // camelize the variable name
        // pet_id => petId
        name = camelize(name, true);

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches("^\\d.*")) {
            name = escapeReservedWord(name);
        }

        return name;
    }

    @Override
    public String toParamName(String name) {
        // sanitize name
        name = sanitizeName(name);

        // replace - with _ e.g. created-at => created_at
        name = name.replaceAll("-", "_");

        // if it's all uppper case, do nothing
        if (name.matches("^[A-Z_]*$")) {
            return name;
        }

        // camelize(lower) the variable name
        // pet_id => petId
        name = camelize(name, true);

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches("^\\d.*")) {
            name = escapeReservedWord(name);
        }

        return name;
    }

    @Override
    public CodegenModel fromModel(String name, Model model, Map<String, Model> allDefinitions) {
        CodegenModel codegenModel = super.fromModel(name, model, allDefinitions);
        if (codegenModel.description != null) {
            codegenModel.imports.add("ApiModel");
        }
        if (allDefinitions != null) {
            String parentSchema = codegenModel.parentSchema;

            // multilevel inheritance: reconcile properties of all the parents
            while (parentSchema != null) {
                final Model parentModel = allDefinitions.get(parentSchema);
                final CodegenModel parentCodegenModel = super.fromModel(codegenModel.parent, parentModel,
                        allDefinitions);
                codegenModel = AirGenCodegen.reconcileProperties(codegenModel, parentCodegenModel);

                // get the next parent
                parentSchema = parentCodegenModel.parentSchema;
            }
        }

        return codegenModel;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setUnwrapRequired(boolean unwrapRequired) {
        this.unwrapRequired = unwrapRequired;
    }

    public void setObjcCompatible(boolean objcCompatible) {
        this.objcCompatible = objcCompatible;
    }

    public void setLenientTypeCast(boolean lenientTypeCast) {
        this.lenientTypeCast = lenientTypeCast;
    }

    public void setResponseAs(String[] responseAs) {
        this.responseAs = responseAs;
    }

    public void setSwiftUseApiNamespace(boolean swiftUseApiNamespace) {
        this.swiftUseApiNamespace = swiftUseApiNamespace;
    }

    @Override
    public String toEnumValue(String value, String datatype) {
        return String.valueOf(value);
    }

    @Override
    public String toEnumDefaultValue(String value, String datatype) {
        return datatype + "_" + value;
    }

    @Override
    public String toEnumVarName(String name, String datatype) {
        if (name.length() == 0) {
            return "empty";
        }

        Pattern startWithNumberPattern = Pattern.compile("^\\d+");
        Matcher startWithNumberMatcher = startWithNumberPattern.matcher(name);
        if (startWithNumberMatcher.find()) {
            String startingNumbers = startWithNumberMatcher.group(0);
            String nameWithoutStartingNumbers = name.substring(startingNumbers.length());

            return "_" + startingNumbers + camelize(nameWithoutStartingNumbers, true);
        }

        // for symbol, e.g. $, #
        if (getSymbolName(name) != null) {
            return camelize(WordUtils.capitalizeFully(getSymbolName(name).toUpperCase()), true);
        }

        // Camelize only when we have a structure defined below
        Boolean camelized = false;
        if (name.matches("[A-Z][a-z0-9]+[a-zA-Z0-9]*")) {
            name = camelize(name, true);
            camelized = true;
        }

        // Reserved Name
        String nameLowercase = StringUtils.lowerCase(name);
        if (isReservedWord(nameLowercase)) {
            return escapeReservedWord(nameLowercase);
        }

        // Check for numerical conversions
        if ("Int".equals(datatype) || "Int32".equals(datatype) || "Int64".equals(datatype) || "Float".equals(datatype)
                || "Double".equals(datatype)) {
            String varName = "number" + camelize(name);
            varName = varName.replaceAll("-", "minus");
            varName = varName.replaceAll("\\+", "plus");
            varName = varName.replaceAll("\\.", "dot");
            return varName;
        }

        // If we have already camelized the word, don't progress
        // any further
        if (camelized) {
            return name;
        }

        char[] separators = { '-', '_', ' ', ':', '(', ')' };
        return camelize(
                WordUtils.capitalizeFully(StringUtils.lowerCase(name), separators).replaceAll("[-_ :\\(\\)]", ""),
                true);
    }

    @Override
    public String toEnumName(CodegenProperty property) {
        String enumName = toModelName(property.name);

        // Ensure that the enum type doesn't match a reserved word or
        // the variable name doesn't match the generated enum type or the
        // Swift compiler will generate an error
        if (isReservedWord(property.datatypeWithEnum) || toVarName(property.name).equals(property.datatypeWithEnum)) {
            enumName = property.datatypeWithEnum + "Enum";
        }

        // TODO: toModelName already does something for names starting with number,
        // so this code is probably never called
        if (enumName.matches("\\d.*")) { // starts with number
            return "_" + enumName;
        } else {
            return enumName;
        }
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        Map<String, Object> postProcessedModelsEnum = postProcessModelsEnum(objs);

        // We iterate through the list of models, and also iterate through each of the
        // properties for each model. For each property, if:
        //
        // CodegenProperty.name != CodegenProperty.baseName
        //
        // then we set
        //
        // CodegenProperty.vendorExtensions["x-codegen-escaped-property-name"] = true
        //
        // Also, if any property in the model has x-codegen-escaped-property-name=true,
        // then we mark:
        //
        // CodegenModel.vendorExtensions["x-codegen-has-escaped-property-names"] = true
        //
        List<Object> models = (List<Object>) postProcessedModelsEnum.get("models");
        for (Object _mo : models) {
            Map<String, Object> mo = (Map<String, Object>) _mo;
            CodegenModel cm = (CodegenModel) mo.get("model");
            boolean modelHasPropertyWithEscapedName = false;
            for (CodegenProperty prop : cm.allVars) {
                if (!prop.name.equals(prop.baseName)) {
                    prop.vendorExtensions.put("x-codegen-escaped-property-name", true);
                    modelHasPropertyWithEscapedName = true;
                }
            }
            if (modelHasPropertyWithEscapedName) {
                cm.vendorExtensions.put("x-codegen-has-escaped-property-names", true);
            }
        }

        return postProcessedModelsEnum;
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        super.postProcessModelProperty(model, property);

        // The default template code has the following logic for
        // assigning a type as Swift Optional:
        //
        // {{^unwrapRequired}}?{{/unwrapRequired}}
        // {{#unwrapRequired}}{{^required}}?{{/required}}{{/unwrapRequired}}
        //
        // which means:
        //
        // boolean isSwiftOptional = !unwrapRequired || (unwrapRequired &&
        // !property.required);
        //
        // We can drop the check for unwrapRequired in (unwrapRequired &&
        // !property.required)
        // due to short-circuit evaluation of the || operator.
        boolean isSwiftOptional = !unwrapRequired || !property.required;
        boolean isSwiftScalarType = property.isInteger || property.isLong || property.isFloat || property.isDouble
                || property.isBoolean;
        if (isSwiftOptional && isSwiftScalarType) {
            // Optional scalar types like Int?, Int64?, Float?, Double?, and Bool?
            // do not translate to Objective-C. So we want to flag those
            // properties in case we want to put special code in the templates
            // which provide Objective-C compatibility.
            property.vendorExtensions.put("x-swift-optional-scalar", true);
        }
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove " to avoid code injection
        return input.replace("\"", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }

    private static CodegenModel reconcileProperties(CodegenModel codegenModel, CodegenModel parentCodegenModel) {
        // To support inheritance in this generator, we will analyze
        // the parent and child models, look for properties that match, and remove
        // them from the child models and leave them in the parent.
        // Because the child models extend the parents, the properties
        // will be available via the parent.

        // Get the properties for the parent and child models
        final List<CodegenProperty> parentModelCodegenProperties = parentCodegenModel.vars;
        List<CodegenProperty> codegenProperties = codegenModel.vars;
        codegenModel.allVars = new ArrayList<CodegenProperty>(codegenProperties);
        codegenModel.parentVars = parentCodegenModel.allVars;

        // Iterate over all of the parent model properties
        boolean removedChildProperty = false;

        for (CodegenProperty parentModelCodegenProperty : parentModelCodegenProperties) {
            // Now that we have found a prop in the parent class,
            // and search the child class for the same prop.
            Iterator<CodegenProperty> iterator = codegenProperties.iterator();
            while (iterator.hasNext()) {
                CodegenProperty codegenProperty = iterator.next();
                if (codegenProperty.baseName == parentModelCodegenProperty.baseName) {
                    // We found a property in the child class that is
                    // a duplicate of the one in the parent, so remove it.
                    iterator.remove();
                    removedChildProperty = true;
                }
            }
        }

        if (removedChildProperty) {
            // If we removed an entry from this model's vars, we need to ensure hasMore is
            // updated
            int count = 0;
            int numVars = codegenProperties.size();
            for (CodegenProperty codegenProperty : codegenProperties) {
                count += 1;
                codegenProperty.hasMore = (count < numVars) ? true : false;
            }
            codegenModel.vars = codegenProperties;
        }

        return codegenModel;
    }
}
