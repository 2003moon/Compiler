package util;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
@Getter
public class IcGenerator {
    private static int maincfg;
    @Setter
    private int currcfg;
    private Map<Integer,CFG> cfgMap;

    public IcGenerator(){
        createMainCfg();
        currcfg = maincfg;
    }

    public CFG getCfg(int id){
        return cfgMap.get(id);
    }

    public void createCfg(int identid){
        CFG cfg = new CFG();
        cfgMap.put(identid,cfg);
    }

    public Result combine(Opcode op, Result r1, Result r2){
        Result res= null;

        if(r1!=null && r2 != null && r1.getType()==Result.Type.constant && r2.getType()==Result.Type.constant){
            if(op==Opcode.add){
                res = new Result(Result.Type.constant,r1.getValue()+r2.getValue());
            }else if(op == Opcode.sub){
                res = new Result(Result.Type.constant,r1.getValue()-r2.getValue());
            }else if(op == Opcode.mul){
                res = new Result(Result.Type.constant,r1.getValue()*r2.getValue());
            }else if(op == Opcode.div){
                res = new Result(Result.Type.constant,r1.getValue()/r2.getValue());
            }else{
                return res;
            }
        }else{
            CFG currCfg = cfgMap.get(currcfg);
            Instruction instr = new Instruction(r1,r2,op,currCfg.getCurrBB());
            instr.setId(currCfg.getTotalInstrNum());
            currCfg.addInstr(instr);
            if (op == Opcode.move){
                currCfg.updateLocalVersion(r2,instr.getId()); // r2 is the one assigned.
            }
            res = new Result(Result.Type.instruction, instr.getId(), isGlobal());
        }
        return res;
    }

    public void passParam(int cfgid, int index, Result x){
        CFG cfg = cfgMap.get(cfgid);
        Result y = cfg.getParameters().get(index);
        combine(Opcode.move,x,y);
    }

    public boolean isGlobal(){
        return currcfg == maincfg;
    }

    private void createMainCfg(){
        maincfg = 0;
        CFG cfg = new CFG();
        cfgMap = new HashMap<>();
        cfgMap.put(maincfg,cfg);
    }
}
