package util;


public enum Token {
    /**keyword**/
    LET, CALL, IF, THEN, FI, WHILE, DO, OD, RETURN, VAR, ARRAY, FUNCTION, PROCEDURE, MAIN,

    /** designator **/
    DESIGN, //<-

    /** operator **/
    PLUS, MINUS, MUL, DIV,

    /** Comparison **/
    EQ, NEQ, LE, GT, GEQ, LEQ,

    /** block **/
    LEFT_PAR, RIGHT_PAR, //(,)
    LEFT_BRACKET, RIGHT_BRACKET, //[,]
    LEFT_BRACE, RIGHT_BRACE, //{,}

    /** punctuation **/
    PERIOD, COMMA, SEMICOMA, COLON,
    /** other **/
    NUMBER,
    IDENT,
    EOF,
    ERROR;

    public static Token getKeyWord(String s){
        switch(s){
            case "let":
                return LET;
            case "call":
                return CALL;
            case "if":
                return IF;
            case "then":
                return THEN;
            case "fi":
                return FI;
            case "while":
                return WHILE;
            case "do":
                return DO;
            case "od":
                return OD;
            case "return":
                return RETURN;
            case "var":
                return VAR;
            case "array":
                return ARRAY;
            case "function":
                return FUNCTION;
            case "procedure":
                return PROCEDURE;
            case "main":
                return MAIN;

        }
        return null;
    }



}
