import com.thoughtworks.qdox.model.JavaMethod
import com.thoughtworks.qdox.JavaDocBuilder
import com.thoughtworks.qdox.model.JavaClass

// lookup maven environment

if (!binding.variables.containsKey("project")) {
    binding.setVariable("project", [basedir: "."])
}
File basedir = project.basedir as File

String listenerAnnotation = "net.kkolyan.tshooter.server.HandleMessage";

JavaDocBuilder builder = new JavaDocBuilder();
builder.addSourceTree(new File(basedir, "src/main/java"))

def methods = builder.classes.collectMany {JavaClass it ->it.methods.findAll {
    it.annotations.find {it.type.fullyQualifiedName == listenerAnnotation}
}}

String handlerPackage = "net.kkolyan.tshooter.protocol"
def messageTypes = new HashSet()
for (JavaMethod method: methods) {
    if (!messageTypes.add(method.parameters[0].type.fullyQualifiedName)) {
        throw new IllegalStateException("each message type should have only one handler")
    }
}

File dir = new File(basedir, "target/generated-sources/protocol/${handlerPackage.replace('.', File.separator)}")
dir.mkdirs()
new File(dir, "DispatchingMessageVisitor.java").text = generateDispatcher(methods, handlerPackage)


static String generateDispatcher(Collection<JavaMethod> methods, String handlerPackage) {
    Map<String,JavaClass> classes = [:]
    methods.each {classes[it.parentClass.fullyQualifiedName] = it.parentClass}
    """
package ${handlerPackage};

import javax.annotation.Resource;
${classes.values().collect {"""
import ${it.fullyQualifiedName};"""}.join('')}

public class DispatchingMessageVisitor extends MessageVisitorAdapter {
    ${classes.values().collect {"""
    private ${it.name} _${it.name};"""}.join('')
    }${methods.collect {"""

    @Override
    public void visit(${it.parameters[0].type.javaClass.name} o) {
        _${it.parentClass.name}.${it.name}(o);
    }"""}.join('')
    }${classes.values().collect {"""

    @Resource
    public void set${it.name}($it.name bean) {
        _$it.name = bean;
    }"""}.join('')}
}
"""
}