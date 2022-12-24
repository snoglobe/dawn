enum class tokenType {
    leftParen, rightParen, comma, dot, dotStar, minus, minusEq, plus, plusEq, semicolon, slash, slashEq, star,
    starEq, percent, percentEq, leftShift, leftShiftEq, rightShift, rightShiftEq, ampersand, ampersandEq,
    bar, barEq, caret, caretEq, tilde, bang, bangEqual, equal, equalEqual, greater, greaterEqual, less, lessEqual, colon,
    leftBracket, rightBracket,

    proc, func, is_, var_, if_, else_, while_, for_, return_, break_, continue_, begin, end, do_,
    and, or, true_, false_, auto, sizeof, ref, deref, enum, register, static, extern, const,
    struct, union, u8, u16, u32, u64, s8, s16, s32, s64, f32, f64, bool, sBool, char, int, double, float, long, longlong, longdouble, short, void,
    volatile, switch, case, default, next, goto, alias, import, label, type, inc, dec, to, asm, alloc, free,
    gotoptr, labelref, as_,

    // finish adding keywords

    identifier, string, charLiteral, intLiteral, floatLiteral, cInsert, eof
}