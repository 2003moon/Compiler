package backend;

import lombok.Getter;
import util.Instruction;

public class ImmOpcode {
    public static final int ADD = 0;
    public static final int SUB = 1;
    public static final int MUL = 2;
    public static final int DIV = 3;
    public static final int MOD = 4;
    public static final int CMP = 5;
    public static final int OR = 8;
    public static final int AND = 9;
    public static final int BIC = 10;
    public static final int XOR = 11;

    public static final int LSH = 12;
    public static final int ASH = 13;

    public static final int CHK = 14;

    public static final int ADDI = 16;
    public static final int SUBI = 17;
    public static final int MULI = 18;
    public static final int DIVI = 19;
    public static final int MODI = 20;
    public static final int CMPI = 21;
    public static final int ORI = 24;
    public static final int ANDI = 25;
    public static final int BICI = 26;
    public static final int XORI = 27;

    public static final int LSHI = 28;
    public static final int ASHI = 29;

    public static final int CHKI = 30;

    public static final int LDW = 32;
    public static final int LDX = 33;
    public static final int POP = 34;
    public static final int STW = 36;
    public static final int STX = 37;
    public static final int PSH = 38;

    public static final int BEQ = 40;
    public static final int BNE = 41;
    public static final int BLT = 42;
    public static final int BGE = 43;
    public static final int BLE = 44;
    public static final int BGT = 45;

    public static final int BSR = 46;
    public static final int JSR = 48;
    public static final int RET = 49;

    public static final int RDD = 50;
    public static final int WRD = 51;
    public static final int WRH = 52;
    public static final int WRL = 53;

    public static final int WORDLEN = 4;

    @Getter
    private int immOp;
    @Getter
    private Format form;

    public ImmOpcode(Instruction instr){
        immOp = compImmOp(instr);
        form = compForm(immOp);
    }

    private int compImmOp(Instruction instr){
        switch (instr.getOp()){
            case add:
                if(instr.hasConst()){
                    return ADDI;
                }else{
                    return ADD;
                }
            case sub:
                if(instr.hasConst()){
                    return SUBI;
                }else{
                    return SUB;
                }
            case mul:
                if(instr.hasConst()){
                    return MULI;
                }else{
                    return MUL;
                }
            case div:
                if(instr.hasConst()){
                    return DIVI;
                }else{
                    return DIV;
                }
            case cmp:
                if(instr.hasConst()){
                    return CMPI;
                }else{
                    return CMP;
                }
            case adda:
                if(instr.hasConst()){
                    return ADDI;
                }else{
                    return ADD;
                }
            case load:
                return LDW;
            case store:
                return STW;
            case bra:
                return BSR;
            case bne:
                return BNE;
            case beq:
                return BEQ;
            case ble:
                return BLE;
            case blt:
                return BLT;
            case bge:
                return BGE;
            case bgt:
                return BGT;
            case read:
                return RDD;
            case write:
                return WRD;
            case writeNL:
                return WRL;

        }
        return -1;
    }

    private Format compForm(int immOp){
        if(0<=immOp && immOp<=14 || immOp == 33 || immOp == 37 || immOp == 39){
            return Format.F2;
        }else if(16<=immOp && immOp <=30 ||immOp == 32 || immOp == 34 || immOp == 36 || immOp == 38
                || immOp >= 40 && immOp <=46){
            return Format.F1;
        }
        return Format.F3;
    }

}
