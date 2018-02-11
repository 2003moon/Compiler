package util;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Result {
    public enum Type{
        instruction,
        constant,
        variable,
        array,
        block;
    }
    public boolean isArray;

    private Type type;

    private int value;

    private int address;

    private int instr_id;

    private int bb_id;


    @Setter
    private int version; //for variable


    public Result(Type type, int val){
        if(type == Type.constant){
            value = val;
        }else if(type == Type.instruction){
            instr_id = val;
        }else if(type == Type.variable){
            address = val;
        }else if(type == Type.block){
            bb_id = val;
        }else{
            address = val;
        }

        isArray = false;
    }


    public Result(Result res){
        this.type = res.type;
        if (type == Type.variable){
            this.address = res.getAddress();
            this.version = res.version;
        }else if (type == Type.constant){
            this.value = res.getValue();
        }else if (type == Type.instruction){
            this.instr_id = res.getInstr_id();
        }else if(type == Type.block){
            this.bb_id = res.getBb_id();
        }else{
            this.address = res.getAddress();
        }
    }

}
