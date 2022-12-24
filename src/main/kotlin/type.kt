interface type {
    fun cRepresentation(): String
}

object u8: type {
    override fun cRepresentation(): String {
        return "unsigned char"
    }
}
object u16: type {
    override fun cRepresentation(): String {
        return "unsigned short"
    }
}
object u32: type {
    override fun cRepresentation(): String {
        return "unsigned int"
    }
}
object u64: type {
    override fun cRepresentation(): String {
        return "unsigned long"
    }
}
object s8: type {
    override fun cRepresentation(): String {
        return "signed char"
    }
}
object s16: type {
    override fun cRepresentation(): String {
        return "signed short"
    }
}
object s32: type {
    override fun cRepresentation(): String {
        return "signed int"
    }
}
object s64: type {
    override fun cRepresentation(): String {
        return "signed long"
    }
}
object f32: type {
    override fun cRepresentation(): String {
        return "float"
    }
}
object f64: type {
    override fun cRepresentation(): String {
        return "double"
    }
}
object bool: type {
    override fun cRepresentation(): String {
        return "unsigned char"
    }
}
object sBool: type {
    override fun cRepresentation(): String {
        return "signed char"
    }
}
object char: type {
    override fun cRepresentation(): String {
        return "char"
    }
}
object int: type {
    override fun cRepresentation(): String {
        return "int"
    }
}
object double: type {
    override fun cRepresentation(): String {
        return "double"
    }
}
object float: type {
    override fun cRepresentation(): String {
        return "float"
    }
}
object long: type {
    override fun cRepresentation(): String {
        return "long"
    }
}
object longlong: type {
    override fun cRepresentation(): String {
        return "long long"
    }
}
object longdouble: type {
    override fun cRepresentation(): String {
        return "long double"
    }
}
object short: type {
    override fun cRepresentation(): String {
        return "short"
    }
}
object void: type {
    override fun cRepresentation(): String {
        return "void"
    }
}
data class proc(val args: List<type>, val name: String): type {
    override fun cRepresentation(): String {
        return "void (*$name)(${args.joinToString(", ") { it.cRepresentation() }})"
    }
}
data class func(val args: List<type>, val ret: type, val name: String): type {
    override fun cRepresentation(): String {
        return "${ret.cRepresentation()} (*$name)(${args.joinToString(", ") { it.cRepresentation() }})"
    }
}
data class pointer<T: type>(val t: T): type {
    override fun cRepresentation(): String {
        return "${t.cRepresentation()}*"
    }
}
data class struct(val name: String, val fields: Map<String, type>, val functions: List<stmt>, val allocFn: funcStmt?, val freeFn: procStmt?): type {
    override fun cRepresentation(): String {
        return "struct { ${fields.map { "${it.value.cRepresentation()} ${it.key};" }.joinToString(" ")} }"
    }
}
data class union(val fields: Map<String, type>): type {
    override fun cRepresentation(): String {
        return "union { ${fields.map { "${it.value.cRepresentation()} ${it.key};" }.joinToString(" ")} }"
    }
}
data class enum(val fields: List<token>): type {
    override fun cRepresentation(): String {
        return "enum { ${fields.joinToString(", ") { it.text }} }"
    }
}
data class typeRef(val name: String): type {
    override fun cRepresentation(): String {
        return name
    }
}