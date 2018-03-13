package optimizer;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

public class InstrNode {

    public boolean isSpilled;
    public boolean isRemoved;
    public int regNo;
    @Getter
    private int instrId;

    public InstrNode(int instrId){
        this.instrId = instrId;
        isSpilled = false;
        isRemoved = false;
        regNo = -1;
    }

    public boolean spilled(){
        return isSpilled;
    }

    public boolean removed(){
        return isRemoved;
    }

    public boolean colored(){
        return regNo!=-1;
    }

    public boolean equals(Object obj){
        if(obj == null || obj.getClass() != getClass()){
            return false;
        }
        InstrNode node = (InstrNode) obj;
        return instrId == node.getInstrId();
    }

    public int hashCode(){
        return instrId;
    }

}
