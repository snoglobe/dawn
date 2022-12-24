abstract class expr {
    interface visitor<R> {
        fun visit(expr: binary): R
        fun visit(expr: intLiteral): R
        fun visit(expr: floatLiteral): R
        fun visit(expr: stringLiteral): R
        fun visit(expr: charLiteral): R
        fun visit(expr: booleanLiteral): R
        fun visit(expr: variable): R
        fun visit(expr: property): R
        fun visit(expr: derefProperty): R
        fun visit(expr: funcProperty): R
        fun visit(expr: call): R
        fun visit(expr: reference): R
        fun visit(expr: dereference): R
        fun visit(expr: cast): R
        fun visit(expr: sizeof): R
        fun visit(expr: alloc): R
        fun visit(expr: preInc): R
        fun visit(expr: preDec): R
        fun visit(expr: not): R
        fun visit(expr: negate): R
        fun visit(expr: bitNot): R
        fun visit(expr: index): R
        fun visit(expr: postInc): R
        fun visit(expr: postDec): R
        fun visit(expr: labelref): R
        fun visit(expr: cLiteralExpr): R
    }

    abstract fun <R> accept(visitor: visitor<R>): R
}

abstract class stmt {
    interface visitor<R> {
        fun visit(stmt: decl): R
        fun visit(stmt: typeDecl): R
        fun visit(stmt: ifStmt): R
        fun visit(stmt: whileStmt): R
        fun visit(stmt: doWhileStmt): R
        fun visit(stmt: forStmt): R
        fun visit(stmt: returnStmt): R
        fun visit(stmt: breakStmt): R
        fun visit(stmt: continueStmt): R
        fun visit(stmt: block): R
        fun visit(stmt: switchStmt): R
        fun visit(stmt: gotoStmt): R
        fun visit(stmt: gotoPtrStmt): R
        fun visit(stmt: labelStmt): R
        fun visit(stmt: asmStmt): R
        fun visit(stmt: freeStmt): R
        fun visit(stmt: exprStmt): R
        fun visit(stmt: procStmt): R
        fun visit(stmt: funcStmt): R
        fun visit(stmt: importStmt): R
        fun visit(stmt: cLiteralStmt): R
    }

    abstract fun <R> accept(visitor: visitor<R>): R
}

data class binary(val left: expr, val operator: token, val right: expr): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class intLiteral(val value: Int): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class floatLiteral(val value: Double): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class stringLiteral(val value: String): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class charLiteral(val value: Char): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class booleanLiteral(val value: Boolean): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class variable(val name: token): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class property(val obj: expr, val name: token): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class derefProperty(val obj: expr, val name: token): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class funcProperty(val obj: expr, val name: token, val args: List<expr>): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class call(val callee: expr, val args: List<expr>): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class reference(val value: expr): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class dereference(val value: expr): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class cast(val value: expr, val type: type): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class sizeof(val value: type): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class alloc(val value: expr): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class preInc(val value: expr): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class preDec(val value: expr): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class not(val value: expr): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class negate(val value: expr): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class bitNot(val value: expr): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class index(val value: expr, val index: expr): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class postInc(val value: expr): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class postDec(val value: expr): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class labelref(val value: token): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class cLiteralExpr(val value: token): expr() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

enum class storageSpec {
    AUTO, STATIC, EXTERN, REGISTER, NONE
}

data class decl(val isVolatile: Boolean, val isConst: Boolean, val storage: storageSpec, val autoSize: expr?, val name: token, val type: type, val value: expr?): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class typeDecl(val name: token, val type: type): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class ifStmt(val condition: expr, val thenBranch: stmt, val elseBranch: stmt?): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class whileStmt(val condition: expr, val body: stmt): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class doWhileStmt(val condition: expr, val body: stmt): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class forStmt(val init: stmt?, val condition: expr?, val increment: expr?, val body: stmt): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class case(val value: expr, val body: stmt, val fallthrough: Boolean)
data class switchStmt(val condition: expr, val body: List<case>, val default: stmt?): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class gotoStmt(val label: token): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class gotoPtrStmt(val label: expr): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class returnStmt(val value: expr?): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

class breakStmt: stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

class continueStmt: stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class labelStmt(val label: token): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class asmStmt(val value: token): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class freeStmt(val value: expr): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class block(val value: List<stmt>): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class exprStmt(val value: expr): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class param(val isConst: Boolean, val name: token, val type: type)
data class procStmt(val name: token, val params: List<param>, val body: stmt): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class funcStmt(val name: token, val params: List<param>, val returnType: type, val body: stmt): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class importStmt(val name: token): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}

data class cLiteralStmt(val value: token): stmt() {
    override fun <R> accept(visitor: visitor<R>): R {
        return visitor.visit(this)
    }
}