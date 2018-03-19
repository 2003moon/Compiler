package util;

import jdk.nashorn.internal.runtime.regexp.joni.constants.OPCode;

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
    writeNL,
    call,
    returnTo,
    dummy,
    deallocate;

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
            case 22:
                return call;
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

    public String toString(Opcode op){
        switch (op){
            case neg:
                return "neg";
            case add:
                return "add";
            case sub:
                return "sub";
            case mul:
                return "mul";
            case div:
                return "div";
            case cmp:
                return "cmp";
            case adda:
                return "adda";
            case load:
                return "load";
            case store:
                return "store";
            case move:
                return "move";
            case phi:
                return "phi";
            case end:
                return "end";
            case bra:
                return "bra";
            case bne:
                return "bne";
            case beq:
                return "beq";
            case ble:
                return "ble";
            case blt:
                return "blt";
            case bge:
                return "bge";
            case bgt:
                return "bgt";
            case read:
                return "read";
            case write:
                return "write";
            case writeNL:
                return "writeNL";
            case call:
                return "call";
            case returnTo:
                return "returnTo";
        }
        return null;
    }
}
