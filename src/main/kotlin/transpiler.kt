import java.util.Stack

class transpiler : expr.visitor<String>, stmt.visitor<String> {

    private val autos = Stack<MutableList<Pair<String, type>>>()

    private val types = mutableMapOf<String, type>()

    private fun pushScope() {
        autos.push(mutableListOf())
    }

    private fun popScope() {
        autos.pop()
    }

    private fun addAuto(name: String, type: type) {
        autos.peek().add(Pair(name, type))
    }

    override fun visit(expr: binary): String {
        return "${expr.left.accept(this)} ${expr.operator.text} ${expr.right.accept(this)}"
    }

    override fun visit(expr: intLiteral): String {
        return expr.value.toString()
    }

    override fun visit(expr: floatLiteral): String {
        return expr.value.toString()
    }

    override fun visit(expr: stringLiteral): String {
        return "\"${expr.value}\""
    }

    override fun visit(expr: charLiteral): String {
        return "'${expr.value}'"
    }

    override fun visit(expr: booleanLiteral): String {
        return expr.value.toString()
    }

    override fun visit(expr: variable): String {
        return expr.name.text
    }

    override fun visit(expr: property): String {
        return "${expr.obj.accept(this)}.${expr.name.text}"
    }

    override fun visit(expr: derefProperty): String {
        return "${expr.obj.accept(this)}->${expr.name.text}"
    }

    override fun visit(expr: funcProperty): String {
        return "${expr.obj.accept(this)}->${expr.name.text}(${expr.obj.accept(this)}, ${expr.args.joinToString(", ") { it.accept(this) }})"
    }

    override fun visit(expr: call): String {
        return "${expr.callee.accept(this)}(${expr.args.joinToString(", ") { it.accept(this) }})"
    }

    override fun visit(expr: reference): String {
        return "&${expr.value.accept(this)}"
    }

    override fun visit(expr: dereference): String {
        return "*${expr.value.accept(this)}"
    }

    override fun visit(expr: cast): String {
        return "(${expr.type.cRepresentation()})${expr.value.accept(this)}"
    }

    override fun visit(expr: sizeof): String {
        return "sizeof(${expr.value.cRepresentation()})"
    }

    override fun visit(expr: alloc): String {
        return "malloc(${expr.value.accept(this)})"
    }

    override fun visit(expr: preInc): String {
        return "++${expr.value.accept(this)}"
    }

    override fun visit(expr: preDec): String {
        return "--${expr.value.accept(this)}"
    }

    override fun visit(expr: not): String {
        return "!${expr.value.accept(this)}"
    }

    override fun visit(expr: negate): String {
        return "-${expr.value.accept(this)}"
    }

    override fun visit(expr: bitNot): String {
        return "~${expr.value.accept(this)}"
    }

    override fun visit(expr: index): String {
        return "${expr.value.accept(this)}[${expr.index.accept(this)}]"
    }

    override fun visit(expr: postInc): String {
        return "${expr.value.accept(this)}++"
    }

    override fun visit(expr: postDec): String {
        return "${expr.value.accept(this)}--"
    }

    override fun visit(expr: labelref): String {
        return "&&$expr.name.text"
    }

    override fun visit(expr: cLiteralExpr): String {
        return expr.value.text
    }

    private fun storageToC(storage: storageSpec): String {
        return when (storage) {
            storageSpec.AUTO -> ""
            storageSpec.REGISTER -> "register"
            storageSpec.STATIC -> "static"
            storageSpec.EXTERN -> "extern"
            storageSpec.NONE -> ""
        }
    }

    override fun visit(stmt: decl): String {
        return "${if (stmt.isConst) "const" else ""} ${if (stmt.isVolatile) "volatile" else ""} ${storageToC(stmt.storage)} ${stmt.type.cRepresentation()} ${stmt.name.text} ${if (stmt.value != null) "= ${stmt.value.accept(this)}" 
        else if(stmt.storage == storageSpec.AUTO) {
            if (stmt.type is typeRef)
                addAuto(stmt.name.text, types[stmt.type.name]!!)
            else if(stmt.type is pointer<*> && stmt.type.t is typeRef)
                addAuto(stmt.name.text, pointer(types[stmt.type.t.name]!!))
            if(stmt.autoSize == null) {
                if (stmt.type is pointer<*> && stmt.type.t is struct) {
                    if (stmt.type.t.allocFn != null) {
                        "= ${stmt.type.t.allocFn.name}()"
                    } else {
                        ""
                    }
                } else {
                    if(stmt.type is pointer<*> && stmt.type.t is typeRef && types[stmt.type.t.name] is struct) {
                        "= ${(types[stmt.type.t.name]!! as struct).allocFn!!.name.text}()"
                    } else {
                        ""
                    }
                }
            } else "= malloc(${stmt.autoSize.accept(this)})"
        } else {
            ""
        } };"
    }

    override fun visit(stmt: typeDecl): String {
        var result = "typedef ${stmt.type.cRepresentation()} ${stmt.name.text};"
        if (stmt.type is struct) {
            if (stmt.type.allocFn != null) {
                result += stmt.type.allocFn.accept(this);
            }
            if (stmt.type.freeFn != null) {
                result += stmt.type.freeFn.accept(this);
            }
        }
        types[stmt.name.text] = stmt.type
        return result
    }

    override fun visit(stmt: ifStmt): String {
        return "if (${stmt.condition.accept(this)}) ${stmt.thenBranch.accept(this)} ${if (stmt.elseBranch != null) "else ${stmt.elseBranch.accept(this)}" else ""}"
    }

    override fun visit(stmt: whileStmt): String {
        return "while (${stmt.condition.accept(this)}) ${stmt.body.accept(this)}"
    }

    override fun visit(stmt: doWhileStmt): String {
        return "do ${stmt.body.accept(this)} while (${stmt.condition.accept(this)});"
    }

    override fun visit(stmt: forStmt): String {
        return "for (${stmt.init?.accept(this) ?: ""}; ${stmt.condition?.accept(this) ?: ""}; ${stmt.increment?.accept(this) ?: ""}) ${stmt.body.accept(this)}"
    }

    override fun visit(stmt: returnStmt): String {
        return "return ${stmt.value?.accept(this) ?: ""};"
    }

    override fun visit(stmt: breakStmt): String {
        return "break;"
    }

    override fun visit(stmt: continueStmt): String {
        return "continue;"
    }

    override fun visit(stmt: block): String {
        pushScope()
        val result = "{${stmt.value.joinToString("") { it.accept(this) }} ${
            autos.peek().joinToString("") { 
                if(it.second is pointer<*> && (it.second as pointer<*>).t is struct && ((it.second as pointer<*>).t as struct).freeFn != null) {
                    "${((it.second as pointer<*>).t as struct).freeFn!!.name.text}(${it.first});"
                } else {
                    "free(${it.first});"
                }
            }
        }}"
        popScope()
        return result
    }

    override fun visit(stmt: switchStmt): String {
        return "switch (${stmt.condition.accept(this)}) {${stmt.body.joinToString("") { 
            "case ${it.value.accept(this)}: ${it.body.accept(this)} ${if (it.fallthrough) "break;" else ""}"
        }} ${if (stmt.default != null) "default: ${stmt.default.accept(this)}" else ""}}"
    }

    override fun visit(stmt: gotoStmt): String {
        return "goto ${stmt.label.text};"
    }

    override fun visit(stmt: gotoPtrStmt): String {
        return "goto *${stmt.label.accept(this)};"
    }

    override fun visit(stmt: labelStmt): String {
        return "${stmt.label.text}:"
    }

    override fun visit(stmt: asmStmt): String {
        return "asm(${stmt.value.text});"
    }

    override fun visit(stmt: freeStmt): String {
        return "free(${stmt.value.accept(this)});"
    }

    override fun visit(stmt: exprStmt): String {
        return "${stmt.value.accept(this)};"
    }

    override fun visit(stmt: procStmt): String {
        return "void ${stmt.name.text}(${stmt.params.joinToString(", ") { "${if(it.isConst) "const" else ""} ${it.type.cRepresentation()} ${it.name.text}" }}) { ${stmt.body.accept(this)} }"
    }

    override fun visit(stmt: funcStmt): String {
        return "${stmt.returnType.cRepresentation()} ${stmt.name.text}(${stmt.params.joinToString(", ") { "${if(it.isConst) "const" else ""} ${it.type.cRepresentation()} ${it.name.text}" }}) { ${stmt.body.accept(this)} }"
    }

    override fun visit(stmt: importStmt): String {
        TODO("Not yet implemented")
    }

    override fun visit(stmt: cLiteralStmt): String {
        return stmt.value.text
    }

}