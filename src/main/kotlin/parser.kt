import kotlin.system.exitProcess

class parser (val tokens: List<token>) {
    var current = 0

    fun parse(): List<stmt> {
        val statements = mutableListOf<stmt>()
        while (!isAtEnd()) {
            statements.add(topLevel())
        }
        return statements
    }

    fun error(message: String) {
        println(message)
        exitProcess(1)
    }

    fun isAtEnd(): Boolean {
        return peek() == tokenType.eof
    }

    fun peek(): tokenType {
        return tokens[current].type
    }

    fun peek(tokenType: tokenType): Boolean {
        return peek() == tokenType
    }

    fun eat(tokenType: tokenType): token {
        if (peek(tokenType)) {
            advance()
            return previous()
        }
        error("expected ${tokenType.name} but got ${peek().name} at line ${tokens[current].line}")
        throw Exception()
    }

    fun advance(): token {
        if (!isAtEnd()) current++
        return previous()
    }

    fun previous(): token {
        return tokens[current - 1]
    }

    fun seeDecl(): Boolean {
        return peek(tokenType.var_) || peek(tokenType.const) || peek(tokenType.volatile) || peek(tokenType.extern) || peek(tokenType.static) || peek(tokenType.auto) || peek(tokenType.register)
    }

    fun topLevel(): stmt {
        if (seeDecl()) {
            val result = declaration()
            eat(tokenType.semicolon)
            return result
        }
        when (peek()) {
            tokenType.type -> {
                val result = typeStmt()
                return result
            }
            tokenType.import -> {
                val result = import()
                return result
            }
            tokenType.proc -> {
                return procDef()
            }
            tokenType.func -> {
                return funcDef()
            }
            tokenType.cInsert -> {
                return cLiteralStmt(eat(tokenType.cInsert))
            }
            else -> {
                error("only decl, type, proc, func, or import allowed at top level, got ${peek().name}")
                throw Exception()
            }
        }
    }

    fun declaration(): stmt {
        val isVolatile = peek(tokenType.volatile)
        if (isVolatile) eat(tokenType.volatile)
        var autoSize: expr? = null
        val storageType = when(peek()) {
            tokenType.extern -> {
                eat(tokenType.extern)
                storageSpec.EXTERN
            }
            tokenType.static -> {
                eat(tokenType.static)
                storageSpec.STATIC
            }
            tokenType.auto -> {
                eat(tokenType.auto)
                if (peek(tokenType.leftParen)) {
                    eat(tokenType.leftParen)
                    autoSize = expr()
                    eat(tokenType.rightParen)
                }
                storageSpec.AUTO
            }
            tokenType.register -> {
                eat(tokenType.register)
                storageSpec.REGISTER
            }
            else -> {
                storageSpec.NONE
            }
        }
        val isConst = peek(tokenType.const)
        if (isConst) eat(tokenType.const)
        eat(tokenType.var_)
        val name = eat(tokenType.identifier)
        eat(tokenType.is_)
        val type = type()
        var value: expr? = null
        if (!peek(tokenType.semicolon)) {
            value = expr()
        }
        return decl(isVolatile, isConst, storageType, autoSize, name, type, value)
    }

    fun typeStmt(): stmt {
        eat(tokenType.type)
        val name = eat(tokenType.identifier)
        eat(tokenType.is_)
        val type: type
        when (peek()) {
            tokenType.struct -> {
                eat(tokenType.struct)
                val members = mutableMapOf<String, type>()
                val functions = mutableListOf<stmt>()
                var allocFunc: funcStmt? = null
                var freeFunc: procStmt? = null
                while(!peek(tokenType.end)) {
                    when (peek()) {
                        tokenType.identifier -> {
                            val memberName = eat(tokenType.identifier)
                            eat(tokenType.is_)
                            val memberType = type()
                            members[memberName.text] = memberType
                            eat(tokenType.semicolon)
                        }
                        tokenType.proc -> {
                            val proc = procDef()
                            functions.add(proc)
                        }
                        tokenType.func -> {
                            val func = funcDef()
                            functions.add(func)
                        }
                        tokenType.alloc -> {
                            if (allocFunc != null) error("alloc function already defined")
                            eat(tokenType.alloc)
                            eat(tokenType.is_)
                            val body = stmt()
                            allocFunc = funcStmt(name.copy(text = name.text + "_alloc"), listOf(), pointer(typeRef(name.text)), body)
                        }
                        tokenType.free -> {
                            if (freeFunc != null) error("free function already defined")
                            eat(tokenType.free)
                            eat(tokenType.leftParen)
                            val thisName = eat(tokenType.identifier)
                            eat(tokenType.rightParen)
                            eat(tokenType.is_)
                            val body = stmt()
                            freeFunc = procStmt(name.copy(text = name.text + "_free"),
                                listOf(param(false, thisName, pointer(typeRef(name.text)))), body)
                        }
                        else -> {
                            error("expected member or function")
                        }
                    }
                }
                eat(tokenType.end)
                type = struct(name.text, members, functions, allocFunc, freeFunc)
            }
            tokenType.union -> {
                eat(tokenType.union)
                val members = mutableMapOf<String, type>()
                while (!peek(tokenType.end)) {
                    val memberName = eat(tokenType.identifier)
                    eat(tokenType.is_)
                    val memberType = type()
                    members[memberName.text] = memberType
                    eat(tokenType.semicolon)
                }
                eat(tokenType.end)
                type = union(members)
            }
            tokenType.enum -> {
                eat(tokenType.enum)
                val values = mutableListOf<token>()
                while (!peek(tokenType.end)) {
                    values.add(eat(tokenType.identifier))
                    eat(tokenType.semicolon)
                }
                eat(tokenType.end)
                type = enum(values)
            }
            tokenType.alias -> {
                eat(tokenType.alias)
                type = type()
            }
            else -> {
                error("expected struct, union, enum, or alias")
                throw Exception()
            }
        }
        return typeDecl(name, type)
    }

    fun procDef(): stmt {
        eat(tokenType.proc)
        val name = eat(tokenType.identifier)
        eat(tokenType.leftParen)
        val params = mutableListOf<param>()
        do {
            if (peek(tokenType.rightParen)) break
            if (peek(tokenType.comma)) eat(tokenType.comma)
            val isConst = peek(tokenType.const)
            if (isConst) eat(tokenType.const)
            val name = eat(tokenType.identifier)
            eat(tokenType.is_)
            val type = type()
            params.add(param(isConst, name, type))
        } while (peek(tokenType.comma))
        eat(tokenType.rightParen)
        eat(tokenType.is_)
        val body = stmt()
        return procStmt(name, params, body)
    }

    fun funcDef(): stmt {
        eat(tokenType.func)
        val name = eat(tokenType.identifier)
        eat(tokenType.leftParen)
        val params = mutableListOf<param>()
        do {
            if (peek(tokenType.rightParen)) break
            if (peek(tokenType.comma)) eat(tokenType.comma)
            val isConst = peek(tokenType.const)
            if (isConst) eat(tokenType.const)
            val name = eat(tokenType.identifier)
            eat(tokenType.is_)
            val type = type()
            params.add(param(isConst, name, type))
        } while (peek(tokenType.comma))
        eat(tokenType.rightParen)
        val returnType = type()
        eat(tokenType.is_)
        val body = stmt()
        return funcStmt(name, params, returnType, body)
    }

    fun import(): stmt {
        eat(tokenType.import)
        val name = eat(tokenType.string)
        return importStmt(name)
    }

    fun type(): type {
        var ptrCount = 0
        while(peek(tokenType.star)) {
            eat(tokenType.star)
            ptrCount++
        }
        var type: type
        when (peek()) {
            tokenType.u8 -> {
                eat(tokenType.u8)
                type = u8
            }
            tokenType.u16 -> {
                eat(tokenType.u16)
                type = u16
            }
            tokenType.u32 -> {
                eat(tokenType.u32)
                type = u32
            }
            tokenType.u64 -> {
                eat(tokenType.u64)
                type = u64
            }
            tokenType.s8 -> {
                eat(tokenType.s8)
                type = s8
            }
            tokenType.s16 -> {
                eat(tokenType.s16)
                type = s16
            }
            tokenType.s32 -> {
                eat(tokenType.s32)
                type = s32
            }
            tokenType.s64 -> {
                eat(tokenType.s64)
                type = s64
            }
            tokenType.f32 -> {
                eat(tokenType.f32)
                type = f32
            }
            tokenType.f64 -> {
                eat(tokenType.f64)
                type = f64
            }
            tokenType.bool -> {
                eat(tokenType.bool)
                type = bool
            }
            tokenType.sBool -> {
                eat(tokenType.sBool)
                type = sBool
            }
            tokenType.proc -> {
                eat(tokenType.proc)
                eat(tokenType.leftParen)
                val params = mutableListOf<type>()
                do {
                    if (peek(tokenType.rightParen)) break
                    if (peek(tokenType.comma)) eat(tokenType.comma)
                    params.add(type())
                } while (peek(tokenType.comma))
                eat(tokenType.rightParen)
                type = proc(params, "")
            }
            tokenType.func -> {
                eat(tokenType.func)
                eat(tokenType.leftParen)
                val params = mutableListOf<type>()
                do {
                    if (peek(tokenType.rightParen)) break
                    if (peek(tokenType.comma)) eat(tokenType.comma)
                    params.add(type())
                } while (peek(tokenType.comma))
                eat(tokenType.rightParen)
                val returnType = type()
                type = func(params, returnType, "")
            }
            tokenType.identifier -> {
                val name = eat(tokenType.identifier)
                type = typeRef(name.text)
            }
            tokenType.char -> {
                eat(tokenType.char)
                type = char
            }
            tokenType.int -> {
                eat(tokenType.int)
                type = int
            }
            tokenType.float -> {
                eat(tokenType.float)
                type = float
            }
            tokenType.double -> {
                eat(tokenType.double)
                type = double
            }
            tokenType.short -> {
                eat(tokenType.short)
                type = short
            }
            tokenType.long -> {
                eat(tokenType.long)
                type = long
            }
            tokenType.longdouble -> {
                eat(tokenType.longdouble)
                type = longdouble
            }
            tokenType.longlong -> {
                eat(tokenType.longlong)
                type = longlong
            }
            tokenType.void -> {
                eat(tokenType.void)
                type = void
            }
            else -> {
                error("expected type")
                throw Exception()
            }
        }
        for (i in 0 until ptrCount) {
            type = pointer(type)
        }
        return type
    }

    fun stmt(): stmt {
        if(seeDecl()) {
            val result = declaration()
            eat(tokenType.semicolon)
            return result
        }
        when(peek()) {
            tokenType.if_ -> {
                eat(tokenType.if_)
                val condition = expr()
                val thenBranch = stmt()
                val elseBranch = if (peek(tokenType.else_)) {
                    eat(tokenType.else_)
                    stmt()
                } else {
                    null
                }
                return ifStmt(condition, thenBranch, elseBranch)
            }
            tokenType.while_ -> {
                eat(tokenType.while_)
                val condition = expr()
                val body = stmt()
                return whileStmt(condition, body)
            }
            tokenType.do_ -> {
                eat(tokenType.do_)
                val body = stmt()
                eat(tokenType.while_)
                val condition = expr()
                return doWhileStmt(condition, body)
            }
            tokenType.for_ -> {
                eat(tokenType.for_)
                val init = if (peek(tokenType.semicolon)) {
                    null
                } else {
                    stmt()
                }
                eat(tokenType.semicolon)
                val condition = if (peek(tokenType.semicolon)) {
                    null
                } else {
                    expr()
                }
                eat(tokenType.semicolon)
                val increment = if (peek(tokenType.begin)) {
                    null
                } else {
                    expr()
                }
                val body = stmt()
                return forStmt(init, condition, increment, body)
            }
            tokenType.switch -> {
                eat(tokenType.switch)
                val condition = expr()
                eat(tokenType.begin)
                val cases = mutableListOf<case>()
                while (!peek(tokenType.end) && !peek(tokenType.default)) {
                    eat(tokenType.case)
                    val caseCondition = expr()
                    eat(tokenType.colon)
                    val caseBody = stmt()
                    val fallthrough = peek(tokenType.next)
                    if (fallthrough) eat(tokenType.next)
                    cases.add(case(caseCondition, caseBody, fallthrough))
                }
                val default = if (peek(tokenType.default)) {
                    eat(tokenType.default)
                    eat(tokenType.colon)
                    stmt()
                } else {
                    null
                }
                eat(tokenType.end)
                return switchStmt(condition, cases, default)
            }
            tokenType.goto -> {
                eat(tokenType.goto)
                val label = eat(tokenType.identifier)
                eat(tokenType.semicolon)
                return gotoStmt(label)
            }
            tokenType.gotoptr -> {
                eat(tokenType.gotoptr)
                val label = expr()
                eat(tokenType.semicolon)
                return gotoPtrStmt(label)
            }
            tokenType.return_ -> {
                eat(tokenType.return_)
                val value = if (peek(tokenType.semicolon)) {
                    null
                } else {
                    expr()
                }
                eat(tokenType.semicolon)
                return returnStmt(value)
            }
            tokenType.break_ -> {
                eat(tokenType.break_)
                eat(tokenType.semicolon)
                return breakStmt()
            }
            tokenType.continue_ -> {
                eat(tokenType.continue_)
                eat(tokenType.semicolon)
                return continueStmt()
            }
            tokenType.label -> {
                val label = eat(tokenType.label)
                eat(tokenType.colon)
                return labelStmt(label)
            }
            tokenType.asm -> {
                eat(tokenType.asm)
                val asm = eat(tokenType.string)
                eat(tokenType.semicolon)
                return asmStmt(asm)
            }
            tokenType.free -> {
                eat(tokenType.free)
                val value = expr()
                eat(tokenType.semicolon)
                return freeStmt(value)
            }
            tokenType.begin -> {
                eat(tokenType.begin)
                val statements = mutableListOf<stmt>()
                while (!peek(tokenType.end)) {
                    statements.add(stmt())
                }
                eat(tokenType.end)
                return block(statements)
            }
            else -> {
                val result = expr()
                eat(tokenType.semicolon)
                return exprStmt(result)
            }
        }
    }

    fun atom(): expr {
        var result = when(peek()) {
            tokenType.intLiteral -> {
                val token = eat(tokenType.intLiteral)
                intLiteral(token.text.toInt())
            }
            tokenType.floatLiteral -> {
                val token = eat(tokenType.floatLiteral)
                floatLiteral(token.text.toDouble())
            }
            tokenType.string -> {
                val token = eat(tokenType.string)
                stringLiteral(token.text)
            }
            tokenType.charLiteral -> {
                val token = eat(tokenType.charLiteral)
                charLiteral(token.text[0])
            }
            tokenType.true_ -> {
                eat(tokenType.true_)
                booleanLiteral(true)
            }
            tokenType.false_ -> {
                eat(tokenType.false_)
                booleanLiteral(false)
            }
            tokenType.identifier -> {
                val name = eat(tokenType.identifier)
                variable(name)
            }
            tokenType.labelref -> {
                eat(tokenType.labelref)
                val name = eat(tokenType.identifier)
                labelref(name)
            }
            tokenType.leftParen -> {
                eat(tokenType.leftParen)
                val result = expr()
                eat(tokenType.rightParen)
                result
            }
            tokenType.alloc -> {
                eat(tokenType.alloc)
                val expr = expr()
                alloc(expr)
            }
            tokenType.sizeof -> {
                eat(tokenType.sizeof)
                val expr = type()
                sizeof(expr)
            }
            tokenType.cInsert -> {
                return(cLiteralExpr(eat(tokenType.cInsert)))
            }
            else -> {
                error("unexpected token ${peek()}")
                throw Exception()
            }
        }
        while (seeAtomSuffix()) {
            result = atomSuffix(result)
        }
        return result
    }

    fun seeAtomSuffix(): Boolean {
        return when(peek()) {
            tokenType.leftParen -> true
            tokenType.leftBracket -> true
            tokenType.dot -> true
            tokenType.dotStar -> true
            tokenType.colon -> true
            tokenType.as_ -> true
            tokenType.inc -> true
            tokenType.dec -> true
            else -> false
        }
    }

    fun atomSuffix(intermediate: expr): expr {
        return when(peek()) {
            tokenType.leftParen -> {
                eat(tokenType.leftParen)
                val args = mutableListOf<expr>()
                do {
                    if (peek(tokenType.rightParen)) break
                    if (peek(tokenType.comma)) eat(tokenType.comma)
                    args.add(expr())
                } while (peek(tokenType.comma))
                eat(tokenType.rightParen)
                call(intermediate, args)
            }
            tokenType.leftBracket -> {
                eat(tokenType.leftBracket)
                val index = expr()
                eat(tokenType.rightBracket)
                index(intermediate, index)
            }
            tokenType.dot -> {
                eat(tokenType.dot)
                val name = eat(tokenType.identifier)
                property(intermediate, name)
            }
            tokenType.dotStar -> {
                eat(tokenType.dotStar)
                val name = eat(tokenType.identifier)
                derefProperty(intermediate, name)
            }
            tokenType.colon -> {
                eat(tokenType.colon)
                val name = eat(tokenType.identifier)
                eat(tokenType.leftParen)
                val args = mutableListOf<expr>()
                do {
                    if (peek(tokenType.rightParen)) break
                    if (peek(tokenType.comma)) eat(tokenType.comma)
                    args.add(expr())
                } while (peek(tokenType.comma))
                eat(tokenType.rightParen)
                funcProperty(intermediate, name, args)
            }
            tokenType.as_ -> {
                eat(tokenType.as_)
                val type = type()
                cast(intermediate, type)
            }
            tokenType.inc -> {
                eat(tokenType.inc)
                postInc(intermediate)
            }
            tokenType.dec -> {
                eat(tokenType.dec)
                postDec(intermediate)
            }
            else -> {
                error("unexpected token ${peek()}")
                throw Exception()
            }
        }
    }

    fun atomPrefix(): expr {
        when(peek()) {
            tokenType.ref -> {
                eat(tokenType.ref)
                val expr = atomPrefix()
                return reference(expr)
            }
            tokenType.deref -> {
                eat(tokenType.deref)
                val expr = atomPrefix()
                return dereference(expr)
            }
            tokenType.inc -> {
                eat(tokenType.inc)
                val expr = atomPrefix()
                return preInc(expr)
            }
            tokenType.dec -> {
                eat(tokenType.dec)
                val expr = atomPrefix()
                return preDec(expr)
            }
            tokenType.bang -> {
                eat(tokenType.bang)
                val expr = atomPrefix()
                return not(expr)
            }
            tokenType.minus -> {
                eat(tokenType.minus)
                val expr = atomPrefix()
                return negate(expr)
            }
            tokenType.tilde -> {
                eat(tokenType.tilde)
                val expr = atomPrefix()
                return bitNot(expr)
            }
            else -> {
                return atom()
            }
        }
    }

    fun term(): expr {
        var result = atomPrefix()
        while (peek(tokenType.star) || peek(tokenType.slash) || peek(tokenType.percent)) {
            result = binary(result, eat(peek()), term())
        }
        return result
    }

    fun expr1(): expr {
        var result = term()
        while (peek(tokenType.plus) || peek(tokenType.minus)) {
            result = binary(result, eat(peek()), expr1())
        }
        return result
    }

    fun expr2(): expr {
        var result = expr1()
        while (peek(tokenType.leftShift) || peek(tokenType.rightShift)) {
            result = binary(result, eat(peek()), expr2())
        }
        return result
    }

    fun comparison(): expr {
        var result = expr2()
        while (peek(tokenType.less) || peek(tokenType.greater) || peek(tokenType.lessEqual) || peek(tokenType.greaterEqual)) {
            result = binary(result, eat(peek()), comparison())
        }
        return result
    }

    fun equality(): expr {
        var result = comparison()
        while (peek(tokenType.equalEqual) || peek(tokenType.bangEqual)) {
            result = binary(result, eat(peek()), equality())
        }
        return result
    }

    fun bitwiseAnd(): expr {
        var result = equality()
        while (peek(tokenType.ampersand)) {
            result = binary(result, eat(peek()), bitwiseAnd())
        }
        return result
    }

    fun bitwiseXor(): expr {
        var result = bitwiseAnd()
        while (peek(tokenType.caret)) {
            result = binary(result, eat(peek()), bitwiseXor())
        }
        return result
    }

    fun bitwiseOr(): expr {
        var result = bitwiseXor()
        while (peek(tokenType.bar)) {
            result = binary(result, eat(peek()), bitwiseOr())
        }
        return result
    }

    fun logicalAnd(): expr {
        var result = bitwiseOr()
        while (peek(tokenType.and)) {
            result = binary(result, eat(peek()), logicalAnd())
        }
        return result
    }

    fun logicalOr(): expr {
        var result = logicalAnd()
        while (peek(tokenType.or)) {
            result = binary(result, eat(peek()), logicalOr())
        }
        return result
    }

    fun expr(): expr {
        var result = logicalOr()
        while (peek(tokenType.equal) || peek(tokenType.plusEq) || peek(tokenType.minusEq) || peek(tokenType.starEq) || peek(tokenType.slashEq) || peek(tokenType.percentEq) || peek(tokenType.leftShiftEq) || peek(tokenType.rightShiftEq) || peek(tokenType.ampersandEq) || peek(tokenType.caretEq) || peek(tokenType.barEq)) {
            result = binary(result, eat(peek()), expr())
        }
        return result
    }
}

