import kotlin.system.exitProcess

class lexer(val source: String) {
    var tokens = mutableListOf<token>()
    var start = 0
    var current = 0
    var line = 1

    val keywords = mapOf(
        "proc" to tokenType.proc,
        "func" to tokenType.func,
        "is" to tokenType.is_,
        "var" to tokenType.var_,
        "if" to tokenType.if_,
        "else" to tokenType.else_,
        "while" to tokenType.while_,
        "for" to tokenType.for_,
        "return" to tokenType.return_,
        "break" to tokenType.break_,
        "continue" to tokenType.continue_,
        "and" to tokenType.and,
        "or" to tokenType.or,
        "true" to tokenType.true_,
        "false" to tokenType.false_,
        "auto" to tokenType.auto,
        "sizeof" to tokenType.sizeof,
        "ref" to tokenType.ref,
        "deref" to tokenType.deref,
        "enum" to tokenType.enum,
        "register" to tokenType.register,
        "static" to tokenType.static,
        "extern" to tokenType.extern,
        "const" to tokenType.const,
        "struct" to tokenType.struct,
        "union" to tokenType.union,
        "s8" to tokenType.s8,
        "s16" to tokenType.s16,
        "s32" to tokenType.s32,
        "s64" to tokenType.s64,
        "u8" to tokenType.u8,
        "u16" to tokenType.u16,
        "u32" to tokenType.u32,
        "u64" to tokenType.u64,
        "f32" to tokenType.f32,
        "f64" to tokenType.f64,
        "bool" to tokenType.bool,
        "sbool" to tokenType.sBool,
        "volatile" to tokenType.volatile,
        "switch" to tokenType.switch,
        "case" to tokenType.case,
        "default" to tokenType.default,
        "next" to tokenType.next,
        "goto" to tokenType.goto,
        "gotoptr" to tokenType.gotoptr,
        "alias" to tokenType.alias,
        "import" to tokenType.import,
        "label" to tokenType.label,
        "type" to tokenType.type,
        "inc" to tokenType.inc,
        "dec" to tokenType.dec,
        "to" to tokenType.to,
        "asm" to tokenType.asm,
        "alloc" to tokenType.alloc,
        "free" to tokenType.free,
        "labelref" to tokenType.labelref,
        "begin" to tokenType.begin,
        "end" to tokenType.end,
        "do" to tokenType.do_,
        "as" to tokenType.as_,
        "char" to tokenType.char,
        "int" to tokenType.int,
        "double" to tokenType.double,
        "float" to tokenType.float,
        "long" to tokenType.long,
        "longlong" to tokenType.longlong,
        "longdouble" to tokenType.longdouble,
        "short" to tokenType.short,
        "void" to tokenType.void
    )

    fun isAtEnd(): Boolean {
        return current >= source.length
    }

    fun advance(): Char {
        current++
        return source[current - 1]
    }

    fun addToken(type: tokenType) {
        val text = source.substring(start, current)
        tokens.add(token(line, type, text))
    }

    fun addToken(type: tokenType, text: String) {
        tokens.add(token(line, type, text))
    }

    fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source[current] != expected) return false

        current++
        return true
    }

    fun peek(): Char {
        if (isAtEnd()) return '\u0000'
        return source[current]
    }

    fun peekNext(): Char {
        if (current + 1 >= source.length) return '\u0000'
        return source[current + 1]
    }

    fun string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++
            advance()
        }

        if (isAtEnd()) {
            error("Unterminated string.")
        }

        advance()

        val value = source.substring(start + 1, current - 1)
        addToken(tokenType.string, value)
    }

    fun number() {
        while (peek().isDigit()) advance()

        if (peek() == '.' && peekNext().isDigit()) {
            advance()

            while (peek().isDigit()) advance()
            addToken(tokenType.floatLiteral)
        } else {
            addToken(tokenType.intLiteral)
        }
    }

    fun identifier() {
        while (peek().isLetterOrDigit() || peek() == '_') advance()

        val text = source.substring(start, current)

        val type = keywords[text]
        if (type == null) addToken(tokenType.identifier)
        else addToken(type)
    }

    fun literalIdentifier() {
        while (peek() != '`') advance()

        advance()

        val text = source.substring(start + 1, current - 1)

        addToken(tokenType.identifier, text)
    }

    fun skipComment() {
        while (peek() != '*' && peekNext() != ')' && !isAtEnd()) {
            if (peek() == '\n') line++
            advance()
        }

        if (isAtEnd()) {
            error("unterminated comment")
        }

        advance()
        advance()
    }

    fun scanToken() {
        when (val c = advance()) {
            '(' -> {
                if (match('*')) {
                    if (match('%')) {
                        skipComment()

                        val text = source.substring(start + 3, current - 2)
                        addToken(tokenType.cInsert, text)
                    } else {
                        skipComment()
                    }
                } else {
                    addToken(tokenType.leftParen)
                }
            }
            ')' -> addToken(tokenType.rightParen)
            '[' -> addToken(tokenType.leftBracket)
            ']' -> addToken(tokenType.rightBracket)
            ',' -> addToken(tokenType.comma)
            '.' -> addToken(if (match('*')) tokenType.dotStar else tokenType.dot)
            ':' -> addToken(tokenType.colon)
            '-' -> addToken(if (match('=')) tokenType.minusEq else tokenType.minus)
            '+' -> addToken(if (match('=')) tokenType.plusEq else tokenType.plus)
            ';' -> addToken(tokenType.semicolon)
            '*' -> addToken(if (match('=')) tokenType.starEq else tokenType.star)
            '%' -> addToken(if (match('=')) tokenType.percentEq else tokenType.percent)
            '!' -> addToken(if (match('=')) tokenType.bangEqual else tokenType.bang)
            '=' -> addToken(if (match('=')) tokenType.equalEqual else tokenType.equal)
            '>' -> addToken(
                if (match('=')) tokenType.greaterEqual
                else if (match('>'))
                    if (match('=')) tokenType.rightShiftEq
                    else tokenType.rightShift
                else tokenType.greater
            )
            '<' -> addToken(
                if (match('=')) tokenType.lessEqual
                else if (match('<'))
                    if (match('=')) tokenType.leftShiftEq
                    else tokenType.leftShift
                else tokenType.less
            )
            '&' -> addToken(if (match('=')) tokenType.ampersandEq else tokenType.ampersand)
            '|' -> addToken(if (match('=')) tokenType.barEq else tokenType.bar)
            '~' -> addToken(tokenType.tilde)
            '^' -> addToken(if (match('=')) tokenType.caretEq else tokenType.caret)
            '/' -> {
                if (match('=')) {
                    addToken(tokenType.slashEq)
                } else {
                    addToken(tokenType.slash)
                }
            }
            '`' -> literalIdentifier()
            ' ', '\r', '\t' -> {}
            '\n' -> line++
            '"' -> string()
            '\'' -> {
                if (peek() == '\\') advance()
                advance()
                if (peek() != '\'') error("unterminated character literal.")
                advance()
                addToken(tokenType.charLiteral, source.substring(start + 1, current - 1))
            }
            else -> {
                if (c.isDigit()) {
                    number()
                } else if (c.isLetter()) {
                    identifier()
                } else {
                    error("unexpected character.")
                }
            }
        }
    }

    fun scanTokens(): List<token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }

        tokens.add(token(line, tokenType.eof, ""))
        return tokens
    }

    fun error(message: String) {
        println("error at line $line: $message")
        exitProcess(1)
    }
}