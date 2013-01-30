// lookup maven environment

if (!binding.variables.containsKey("project")) {
    binding.setVariable("project", [basedir: "."])
}
File basedir = project.basedir as File
File sourceDirectory = new File(basedir, "src/meta")
String utilsPackage = 'net.kkolyan.tshooter.protocol'

// parse protocol descriptors

def ns = new groovy.xml.Namespace("http://kkolyan.net/schema/protocol")
List<Package> packages = []

println "[INFO] Searching for protocol descriptors from in $sourceDirectory.absolutePath"

List<File> descriptors = sourceDirectory.listFiles().findAll {it.name.endsWith(".protocol.xml")}

println "[INFO] Loading protocol descriptors from $descriptors"

String typeIdType = null

for (file in descriptors) {
    def protocol = new XmlParser().parse(file)
    typeIdType = protocol.'@typeIdType'
    packages += protocol[ns.namespace].collect {Node namespace ->
        def pack = new Package()
        pack.name = namespace.'@name'
        int typeCounter = 0
        pack.classes = namespace[ns.type].collect {Node type ->
            def t = new Type()
            t.name = type.'@name'
            t.namespace = pack
            t.typeIndex = typeCounter++
            t.properties = type.collect {Node field ->
                def p = new Property()
                p.name = field.'@name'
                p.configuredType = field.'@type'
                p.sequence = {
                    switch (field.name()) {
                        case ns.value: return false
                        case ns.sequence: return true
                        default: throw new IllegalStateException("invalid field tag: $field")
                    }
                }()
                p.sequenceLengthSize = field.'@lengthType'
                return p
            } as List<Property>
            return t
        } as List<Type>
        return pack
    } as List<Package>
}

void generateClass(File sourceDir, Closure generator) {
    ClassModel model = new ClassModel()
    generator(model)
    if (!model.packageName) {
        throw new IllegalStateException(model.properties as String)
    }
    File dir = new File(sourceDir, model.packageName.replace('.',File.separator))
    dir.mkdirs()
    dir.mkdirs()
    File file = new File(dir, "${model.name}.java")
    println "[INFO] writing $file.absolutePath"
    file.text = model.sourceCode
}

// generate messages, encoder and decoder java source files

File sourceDir = new File(basedir, 'target/generated-sources/protocol/')
for (package0 in packages) {
    for (Type type in package0.classes) {
        generateClass(sourceDir, createMessageGenerator(utilsPackage, type))
    }
}
def classes = packages.collectMany {it.classes}
generateClass(sourceDir, createDecoderGenerator(utilsPackage, classes, typeIdType))
generateClass(sourceDir, createEncoderGenerator(utilsPackage, classes, typeIdType))
generateClass(sourceDir, createMessageVisitorGenerator(utilsPackage, classes))
generateClass(sourceDir, createMessageVisitorAdapterGenerator(utilsPackage, classes))
generateClass(sourceDir, createVisitableMessageGenerator(utilsPackage))

println "[INFO] protocol files generated"

//======================================================================================================================

class Package {
    String name;
    List<Type> classes =[]
}

class Type {
    int typeIndex
    String name
    List<Property> properties = []
    Package namespace
}

class Property {
    String name;
    boolean sequence
    String configuredType
    String sequenceLengthSize

    static primitiveWrappers = [
        "boolean":"Boolean",
        "byte":"Byte",
        "char":"Character",
        "short":"Short",
        "int":"Integer",
        "long":"Long",
        "float":"Float",
        "double":"Double"
    ]

    String getValueType() {
        assert !sequence: "can't get value type of sequence"
        return configuredType;
    }

    String getComponentType() {
        assert sequence : "can't get component type of non-sequence"
        return primitiveWrappers[configuredType]?: configuredType
    }

    String getType() {
        return sequence ? "List<$componentType>" : valueType;
    }
}

//======================================================================================================================

class ClassModel {
    String name
    String sourceCode
    String packageName
}

static createMessageVisitorGenerator(String packageName, Collection<Type> classes) {
    {ClassModel model->
        model.name = 'MessageVisitor'
        model.packageName = packageName
        model.sourceCode = """
package $packageName;
${classes.collect {type->"""
import ${type.namespace.name}.${type.name};"""}.join('')}

public interface MessageVisitor {${classes.collect {type->"""
    void visit($type.name o);"""}.join('')}
}
"""
    }
}

static createMessageVisitorAdapterGenerator(String packageName, Collection<Type> classes) {
    {ClassModel model->
        model.name = 'MessageVisitorAdapter'
        model.packageName = packageName
        model.sourceCode = """
package $packageName;
${classes.collect {type->"""
import ${type.namespace.name}.${type.name};"""}.join('')}

public abstract class MessageVisitorAdapter implements MessageVisitor {${classes.collect {type->"""

    @Override
    public void visit($type.name o) {
        throw new UnsupportedOperationException("$type.name is not supported");
    }"""}.join('')}
}
"""
    }
}

static createVisitableMessageGenerator(String packageName) {
    {ClassModel model->
        model.name = 'VisitableMessage'
        model.packageName = packageName
        model.sourceCode = """
package $packageName;

public interface VisitableMessage {
    void acceptVisitor(MessageVisitor visitor);
}"""
    }
}

static createMessageGenerator(String utilsPackage, Type type) {
    {ClassModel model->
        model.name = type.name
        model.packageName = type.namespace.name
        model.sourceCode = """
package $type.namespace.name;

import java.util.*;
import ${utilsPackage}.VisitableMessage;
import ${utilsPackage}.MessageVisitor;

public final class $type.name implements VisitableMessage {
    ${type.properties.collect {p->"""
    public $p.type $p.name;"""
    }.join('')}
    ${type.properties.collect {p->"""
    public $p.type get${p.name.capitalize()}() {
        return $p.name;
    }

    public void set${p.name.capitalize()}($p.type $p.name) {
        this.$p.name = $p.name;
    }"""
    }.join('')}

    @Override
    public void acceptVisitor(MessageVisitor visitor) {
        visitor.visit(this);
    }
}"""
    }
}

static createDecoderGenerator(String packageName, Collection<Type> classes, String typeIdType) {
    def decodeValue = {switch (it) {
        case 'boolean': case 'Boolean': return 'stream.get() == 1'
        case 'byte': case 'Byte': return 'stream.get()'
        case 'char': case 'Character': return 'stream.getChar()'
        case 'short': case 'Short': return 'stream.getShort()'
        case 'int': case 'Integer': return 'stream.getInt()'
        case 'long': case 'Long': return 'stream.getLong()'
        case 'float': case 'Float': return 'stream.getFloat()'
        case 'double': case 'Double': return 'stream.getDouble()'
        case 'String':  return 'stream.getString(decoder)'
        default: return "decode${it}(stream)"
    }}
    return {ClassModel model->
        model.name = 'DecoderImpl'
        model.packageName = packageName
        model.sourceCode = """
package $packageName;
${classes.collect {type->"""
import ${type.namespace.name}.${type.name};"""}.join('')}

import java.util.*;
import java.io.*;
import org.apache.mina.core.buffer.*;
import java.nio.charset.*;

public class DecoderImpl implements Decoder {
    private static final CharsetDecoder decoder = Charset.forName("utf8").newDecoder();
    ${classes.collect {type-> """
    private ${type.name} decode${type.name}(IoBuffer stream) throws IOException {
        ${type.name} o = new ${type.name}();
        ${type.properties.collect {p->
        p.sequence ? """

        int ${p.name}Length = ${decodeValue(p.sequenceLengthSize)};
        o.${p.name} = new ArrayList<${p.componentType}>();
        for (int i= 0; i < ${p.name}Length; i ++) {
            o.${p.name}.add(${decodeValue(p.componentType)});
        }""" : """
        o.${p.name} = ${decodeValue(p.valueType)};"""
        }.join('')}

        return o;
    }
"""}.join('')}
    @Override
    public Object decode(IoBuffer stream) throws IOException {
        int code = ${decodeValue(typeIdType)};
        ${classes.collect {type->"""
        if (code == $type.typeIndex) {
            return decode${type.name}(stream);
        }"""}.join('')}
        throw new IllegalStateException();
    }
}
    """
    }
}

static createEncoderGenerator(String packageName, Collection<Type> classes, String typeIdType) {
    def encodeValue = {type,value->switch (type) {
        case 'boolean': case 'Boolean': return "stream.put((byte) $value ? 1 : 0)"
        case 'byte': case 'Byte': return "stream.put($value)"
        case 'char': case 'Character': return "stream.putChar($value)"
        case 'short': case 'Short': return "stream.putShort($value)"
        case 'int': case 'Integer': return "stream.putInt($value)"
        case 'long': case 'Long': return "stream.putLong($value)"
        case 'float': case 'Float': return "stream.putFloat($value)"
        case 'double': case 'Double': return "stream.putDouble($value)"
        case 'String':  return "stream.putString($value, encoder).put((byte)0x00)"
        default: return "encode${type}($value, stream)"
    }}
    return {ClassModel model->
        model.name = 'EncoderImpl'
        model.packageName = packageName
        model.sourceCode = """
package $packageName;
${classes.collect {type->"""
import ${type.namespace.name}.${type.name};"""}.join('')}

import java.util.*;
import java.io.*;
import net.kkolyan.tshooter.protocol.*;
import org.apache.mina.core.buffer.*;
import java.nio.charset.*;

public class EncoderImpl implements Encoder {
    private static final CharsetEncoder encoder = Charset.forName("utf8").newEncoder();
    ${classes.collect {type->
    """
    private void encode${type.name}($type.name o, IoBuffer stream) throws IOException {${
        type.properties.collect {p->
        p.sequence ? """
        ${encodeValue(p.sequenceLengthSize, "(${p.sequenceLengthSize}) o.${p.name}.size()")};
        for ($p.componentType value : o.$p.name) {
            ${encodeValue(p.componentType, "value")};
        }""" : """
        ${encodeValue(p.valueType, "o.${p.name}")};"""}.join('')}
    }
"""}.join('')}
    @Override
    public void encode(Object o, IoBuffer stream) throws IOException {
        ${classes.collect {type-> """
        if (o.getClass() == ${type.name}.class) {
            ${encodeValue(typeIdType, "(${typeIdType}) ${type.typeIndex}")};
            encode${type.name}(($type.name) o, stream);
            return;
        }"""}.join('')}
        throw new IllegalStateException("Unsupported type:" + o.getClass());
    }
}
    """
    }
}