package util;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import frontend.scanner;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Result implements Comparable<Result> {
   //TODO: there should be a register type
    public enum Type{
        instruction,
        constant,
        variable,
        branch,
        register,
        function;
    }
    public boolean isArray;

    private Type type;

    private int value;

    private int address;

    private int instr_id;

    private int regNo;

    private int br_id;


    @Setter
    private int version; //for variable


    public Result(Type type, int val) {
        this.type = type;
        if(type == Type.constant){
            value = val;
        }else if(type == Type.instruction){
            instr_id = val;
        }else if(type == Type.variable){
            address = val;
        }else if(type == Type.branch){
            br_id = val;
        }else if(type == Type.register){
            regNo = val;
        } else{
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
        }else if(type == Type.branch){
            this.br_id = res.getBr_id();
        }else{
            this.address = res.getAddress();
        }
    }

    public String toString(scanner sc, CFG cfg){
        StringBuffer sb = new StringBuffer();
        if(type == Type.constant){
            sb.append(value);
        }else if(type == Type.variable){
            String tk = sc.getIdent(address);
            sb.append(tk);
            sb.append("_");
            sb.append(version);
        }else if(type == Type.instruction){
            sb.append("(");
            sb.append(instr_id);
            sb.append(")");
        }else if(type == Type.branch){
            sb.append("branch_"+br_id);
        }else if(type == Type.register){
            sb.append("register_"+regNo);
        }
        return sb.toString();
    }

    public int compareTo(Result r){
        if(r == null){
            return  -1;
        }
        if(this.type != r.type){
            return -1;
        }
        if(type == Type.variable){
            if(this.address != r.getAddress()){
                return -1;
            }
            return this.version - r.version;
        }
        if(type == Type.constant){
            return this.value - r.getValue();
        }

        if(type == Type.instruction){
            return this.instr_id - r.getInstr_id();
        }
        return -1;
    }

}
