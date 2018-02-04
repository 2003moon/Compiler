package util;

import lombok.Getter;
import lombok.Setter;

public class Instruction {
    @Getter @Setter
    private int id; // the size of the instruction list
    @Getter
    private int bb_id;
    @Getter
    private Opcode op;
    private Result oprand1;
    private Result oprand2;


    public Instruction(Result oprand1, Result oprand2, Opcode op, int bb_id){
        this.oprand1 = oprand1;
        this.oprand2 = oprand2;
        this.op = op;
        this.bb_id = bb_id;
    }




}
