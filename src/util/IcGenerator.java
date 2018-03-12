package util;

import exceptions.NotDefinedException;
import lombok.Getter;

import java.io.PrintWriter;
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


    public Result combine(Opcode op, Result r1, Result r2, BasicBlock bb, BasicBlock joinBlock, Map<Integer, ArrayList<Integer>> useChain) throws NotDefinedException{
        Result res= null;

        if(op!= Opcode.store && op!= Opcode.load && r1!=null && r2 != null && r1.getType()==Result.Type.constant && r2.getType()==Result.Type.constant) {
            return constantOp(op, r1, r2);
        }

        CFG cfg = cfgMap.get(bb.getCfgid());

        Instruction instr = new Instruction(r1,r2,op);
        instr.setId(instrTable.size());
        bb.addInstr(this, instr);


        if(r1!=null&&r1.getType() == Result.Type.variable){
            if(!cfg.isDefined(r1.getAddress())){
                throw new NotDefinedException("variable at "+ r1.getAddress() + " not defined");
            }
            if(joinBlock!=null && !joinBlock.hasPhi(r1.getAddress())){
                addUsage(r1.getAddress(),instr.getId(), useChain);
            }

        }

        if (op == Opcode.move){
            //TODO: before inserting phi, need to check if the variable has been assigned globally.
            r2.setVersion(instr.getId());
            if(joinBlock!=null){
                int backup = r2.getVersion();
                if(cfg.isDefined(r2.getAddress())){
                    backup = cfg.getVersion(r2.getAddress());
                }

                joinBlock.insertPhi(this,r2,bb,backup);
            }
            cfg.updateVersion(r2.getAddress(),r2.getVersion());// r2 is the one assigned.
        }else{
            if(r2!=null && r2.getType() == Result.Type.variable){
                if(!cfg.isDefined(r2.getAddress())){
                    throw new NotDefinedException("variable at "+ r2.getAddress() + " not defined");
                }

                if(joinBlock!=null && !joinBlock.hasPhi(r2.getAddress())){
                    addUsage(r2.getAddress(),instr.getId(), useChain);
                }

            }
        }
     //   instrTable.put(instr.getId(), instr);


        res = new Result(Result.Type.instruction, instr.getId());
        return res;
    }

    public void updateUsage(int cfgid, Map<Integer,ArrayList<Integer>> useChain){
        CFG cfg = cfgMap.get(cfgid);
        for(Map.Entry<Integer, ArrayList<Integer>> entry : useChain.entrySet()){
            int key = entry.getKey();
            ArrayList<Integer> instrs = entry.getValue();
            for(Integer pc : instrs){
                Instruction instr = instrTable.get(pc);
                Result res = instr.getOprand(key);
                int version = cfg.getVersion(key);
                res.setVersion(version);
            }
        }
    }


    public void resetVersion(CFG cfg, BasicBlock joinNode){
        for(Map.Entry<Integer, phiAssignment> entry : joinNode.getPhiTable().entrySet()){
            int addr = entry.getKey();
            phiAssignment phi = entry.getValue();
            cfg.updateVersion(addr, phi.backupV);
        }
    }

    public Result constantOp(Opcode op, Result r1, Result r2){
        Result res = null;
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

    private void addUsage(int address,  int pc, Map<Integer, ArrayList<Integer>> useChain){
        if(useChain == null){
            return;
        }
        if(!useChain.containsKey(address)){
            useChain.put(address, new ArrayList<>());
        }
        useChain.get(address).add(pc);
    }






}
