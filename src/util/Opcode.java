package util;

public enum Opcode {
    neg,
    add,
    sub,
    mul,
    div,
    cmp,
    adda,
    load,
    store,
    move,
    phi,
    end,
    bra,
    bne,
    beq,
    ble,
    blt,
    bge,
    bgt,
    read,
    write,
    writeNL;

    public static Opcode getOp(int op){
        switch (op){
            case 0:
                return neg;
            case 1:
                return add;
            case 2:
                return sub;
            case 3:
                return mul;
            case 4:
                return div;
            case 5:
                return cmp;
            case 6:
                return adda;
            case 7:
                return load;
            case 8:
                return store;
            case 9:
                return move;
            case 10:
                return phi;
            case 11:
                return end;
            case 12:
                return bra;
            case 13:
                return bne;
            case 14:
                return beq;
            case 15:
                return ble;
            case 16:
                return blt;
            case 17:
                return bge;
            case 18:
                return bgt;
            case 19:
                return read;
            case 20:
                return write;
            case 21:
                return writeNL;
        }
        return null;
    }

    public static Opcode tokenToOp(Token tk){
        switch (tk) {
            case PLUS:
                return add;
            case MUL:
                return mul;
            case MINUS:
                return sub;
            case DIV:
                return div;
            case EQ:
                return bne;
            case LE:
                return blt;
            case LEQ:
                return ble;
            case GT:
                return bgt;
            case GEQ:
                return bge;
            case NEQ:
                return bne;

        }
        return null;
    }
}
