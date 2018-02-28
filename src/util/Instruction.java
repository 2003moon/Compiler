package util;

import frontend.scanner;
import lombok.Getter;
import lombok.Setter;

public class Instruction implements Comparable<Instruction> {
    @Getter @Setter
    private int id; // the size of the instruction list
    @Getter
    private Opcode op;
    private Result oprand1;
    private Result oprand2;
    @Getter @Setter
    private int bbid;
    //TODO: there should be a bb id to indicate the current bb the instruction is in.


    public int next;

    public Instruction(Result oprand1, Result oprand2, Opcode op){
        this.oprand1 = oprand1;
        this.oprand2 = oprand2;
        this.op = op;
    }

    public void updateOp(Result r){
        if(oprand1 == null){
            oprand1 = r;
        }else{
            oprand2 = r;
        }
    }

    public void updatePhi(int i, Result r){
        if(i == 0){
            oprand1 = r;
        }else{
            oprand2 = r;
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

}
