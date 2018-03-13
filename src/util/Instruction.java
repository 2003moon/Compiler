package util;

import frontend.scanner;
import lombok.Getter;
import lombok.Setter;
@Getter
public class Instruction implements Comparable<Instruction> {
    @Setter
    private int id; // the size of the instruction list
    private Opcode op;

    @Setter
    private int bbid;

    public int pred1 =-1;
    public int pred2 =-1;

    public int next;
    public int prev;
    public Result oprand1;
    public Result oprand2;

    public Instruction(Result oprand1, Result oprand2, Opcode op){
        this.oprand1 = oprand1;
        this.oprand2 = oprand2;
        this.op = op;
    }

    public void updateOp(int i, Result r){
        if(i == 0){
            oprand1 = r;
        }else{
            oprand2 = r;
        }
    }

    public void updatePhi(int i, Result r, int bbid){
        updateOp(i, r);
        if(i == 0){
            pred1 = bbid;
        }else{
            pred2 = bbid;
        }
    }

    public Result getOprand(int id) {
        if(id == oprand1.getAddress()){
            return oprand1;
        }
        return oprand2;
    }

    public String toString(scanner sc, CFG cfg){
        StringBuffer sb = new StringBuffer();
        sb.append(id+": ");
        String operation = op.toString();
        sb.append(operation+" ");
        if(oprand1!=null){
            sb.append(oprand1.toString(sc,cfg));
        }
        sb.append(" ");
        if(oprand2!=null){
            sb.append(oprand2.toString(sc,cfg));
        }
        return sb.toString();
    }

    public int compareTo(Instruction instr){

        if(oprand1!=null && oprand2 != null){
            if(oprand1.compareTo(instr.oprand1)==0){
                if(oprand2.compareTo(instr.oprand2)==0){
                    return 0;
                }
            }else if(oprand1.compareTo(instr.oprand2)==0){
                if(oprand2.compareTo(instr.oprand1)==0){
                    return 0;
                }
            }
        }else if(oprand1 == null){
            if(instr.oprand1 == null && (oprand2 == instr.oprand2 || oprand2.compareTo(instr.oprand2) ==0)){
                return 0;
            }
            if(instr.oprand2 == null && (oprand2 == instr.oprand1 || oprand2.compareTo(instr.oprand1) == 0)){
                return 0;
            }
        }else if(oprand2 == null){
            if(instr.oprand2 == null && oprand1.compareTo(instr.oprand1) ==0){
                return 0;
            }
            if(instr.oprand1 == null && oprand1.compareTo(instr.oprand2) == 0){
                return 0;
            }
        }
        return  -1;
    }
    public boolean isBranch(){
        return op == Opcode.bra || op == Opcode.bne || op == Opcode.beq || op == Opcode.ble
                || op == Opcode.blt || op == Opcode.bge || op == Opcode.bgt;
    }
    public boolean isRegisterNeed(){
        return op!=Opcode.store && op!=Opcode.end && op!= Opcode.write && op!= Opcode.writeNL && !isBranch();
    }

}
