fun main(args: Array<String>) {
    val source = """
        (*% #include <stdio.h> *)
        
        type foo is 
        struct
            a is int;
            b is int;
            
            alloc is
            begin
                var me is *foo alloc sizeof foo;
                me.*a = 1;
                me.*b = 2;
                return me;
            end
            
            free(me) is
            begin
                free me;
            end
        end;
        
        func main(argc is int, argv is **char) int is 
        begin
            auto var x is *foo;
            x.*a = 23;
            x.*b = 43;
            printf("%d %d\n", x.*a, x.*b);
        end 
    """.trimIndent()
    val lexer = lexer(source)
    val tokens = lexer.scanTokens()
    for (token in tokens) {
        println(token)
    }
    val parser = parser(tokens)
    val statements = parser.parse()
    for (statement in statements) {
        println(statement)
    }
    val transpiler = transpiler()
    for (statement in statements) {
        println(statement.accept(transpiler).trim())
    }
}