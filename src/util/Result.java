package util;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Result {
    public enum Type{
        instruction,
        constant,
        variable,
        branch;
    }

    private Type type;

    private int value;

    private int address;

    private int instr_id;

    @Setter
    private int version; //for variable

    private int branch;

    private boolean global;

    public Result(Type type, int val){
        if(type == Type.constant){
            value = val;
        }else if(type == Type.instruction){
            instr_id = val;
        }else{
            branch = val;
        }
    }

    public Result(Type type, int addr, boolean global){
        this.type = type;
        address = addr;
        this.global = global;
    }

}
