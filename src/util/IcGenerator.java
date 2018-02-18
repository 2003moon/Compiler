package util;

import exceptions.NotDefinedException;
import lombok.Getter;


import java.util.*;

@Getter
public class IcGenerator {

    private Map<Integer,CFG> cfgMap;
    private Map<Integer, Result> returnTable; //<address, Result>
    private Map<Integer, Instruction> instrTable;
    private Map<Integer, Instruction> tempInstr;
    private CFG maincfg;

    public IcGenerator(){
        cfgMap = new HashMap<>();
        createCfg(0, "main");
        maincfg = cfgMap.get(0);
        returnTable = new HashMap<>();
        instrTable = new HashMap<>();
        tempInstr = new HashMap<>();
    }

    public CFG getCfg(int id){
        return cfgMap.get(id);
    }

    public void createCfg(int funcid, String name){
        CFG cfg = new CFG(funcid,name);
        cfgMap.put(funcid,cfg);
    }

    public void addReturnTable(int funcid, Result res ){
        returnTable.put(funcid, res);
    }

    public void addinstraTable(int pc, Instruction instr){
        instrTable.put(pc, instr);
    }

    public Instruction getInstruction(int pc){
        return instrTable.get(pc);
    }


    public Result combine(Opcode op, Result r1, Result r2, BasicBlock bb) throws NotDefinedException{
        Result res= null;

        if(op!= Opcode.store && op!= Opcode.load && r1!=null && r2 != null && r1.getType()==Result.Type.constant && r2.getType()==Result.Type.constant) {
            if (op == Opcode.add || op == Opcode.adda) {
                res = new Result(Result.Type.constant, r1.getValue() + r2.getValue());
            } else if (op == Opcode.sub) {
                res = new Result(Result.Type.constant, r1.getValue() - r2.getValue());
            } else if (op == Opcode.mul) {
                res = new Result(Result.Type.constant, r1.getValue() * r2.getValue());
            } else if (op == Opcode.div) {
                res = new Result(Result.Type.constant, r1.getValue() / r2.getValue());
            }
            return res;
        }

        Instruction instr = new Instruction(r1,r2,op);
        instr.setId(instrTable.size());
        instrTable.put(instr.getId(), instr);
        bb.addInstr(this, instr);
        setVersion(r1,bb);

        if (op == Opcode.move){
            r2.setVersion(instr.getId());
            bb.addSymbol(r2); // r2 is the one assigned.
        }else{
            setVersion(r2,bb);
        }


        res = new Result(Result.Type.instruction, instr.getId());
        return res;
    }

    private void setVersion(Result res, BasicBlock bb)throws NotDefinedException{
        if(res!=null && res.getType()== Result.Type.variable && !maincfg.isDeclared(res.getAddress())){
            Result ver_res = bb.getSymbol(res.getAddress());
            if(ver_res==null){
                throw new NotDefinedException("variable at"+res.getAddress()+"is not defined");
            }else{
                res.setVersion(ver_res.getVersion());
            }
        }
    }

  /*  public void passParam(int cfgid, int index, Result x){
        CFG cfg = cfgMap.get(cfgid);
        Result y = cfg.getParameters().get(index);
        combine(Opcode.move,x,y);
    }*/

}
