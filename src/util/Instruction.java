package util;

import frontend.scanner;
import lombok.Getter;
import lombok.Setter;

public class Instruction {
    @Getter @Setter
    private int id; // the size of the instruction list
    @Getter
    private Opcode op;
    private Result oprand1;
    private Result oprand2;


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

}
