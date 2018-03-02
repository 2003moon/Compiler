package optimizer;

import frontend.parser;
import util.*;

import java.beans.IntrospectionException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Optimizer {
    private Map<Integer, Map<Integer, Result>> sourceTable;
    private Map<Integer,Map<Integer, ArrayList<Integer>>> targetsTable;
    private Map<Opcode, ArrayList<Integer>> archerTable;
    private Map<Integer,Result> replaceTable;
    private parser ps;

    public Optimizer(parser ps){
        this.ps = ps;
        sourceTable = new HashMap<>();
        targetsTable = new HashMap<>();
        archerTable = new HashMap<>();
        replaceTable = new HashMap<>();
    }

    public void optimize(){
        initArcherTable();
        DominatorTreeComputer dtComp = ps.getDtComp();
        DFS(dtComp.getNode(0));
    }

    private void DFS(BasicBlock root){
        IcGenerator icGen = ps.getIcGen();
        CFG cfg = icGen.getMaincfg();
        int first = root.getFirstInstr();
        while(true){
            Instruction instr = icGen.getInstruction(first);
            checkReplace(instr, cfg, icGen);
            if(instr.getBbid() == -1){
                first = instr.next;
                continue;
            }
            if(instr.getOp() == Opcode.move){
                putSourceTable(instr, icGen);
                if(first == root.getLastInstr()){
                    root.deleteInstr(icGen, instr);
                    break;
                }
                first = instr.next;
                root.deleteInstr(icGen, instr);
                System.out.println("Instruction_"+instr.getId()+" is removed due to Copy Propagation");
                continue;
            }
            if(instr.getOp()!=Opcode.end && instr.getOp() != Opcode.bra){
                putTargetTable(instr);
            }

            putArcherTable(instr, cfg, icGen);
            if(first == root.getLastInstr()){
                break;
            }
            first = instr.next;
        }

        for(Integer child : root.getChild()){
            DFS(cfg.getBlock(child));
        }
    }

    private void checkReplace(Instruction instr, CFG cfg, IcGenerator icGen){
        Result r1 = instr.oprand1;
        Result r2 = instr.oprand2;
        if(r1.getType() == Result.Type.instruction){
            if(replaceTable.containsKey(r1.getInstr_id())){
                instr.updatePhi(0, replaceTable.get(r1.getInstr_id()));
            }
        }

        if(r2.getType() == Result.Type.instruction){
            if(replaceTable.containsKey(r2.getInstr_id())){
                instr.updatePhi(1,replaceTable.get(r2.getInstr_id()));
            }
        }

        if(instr.oprand1.getType() == Result.Type.constant && instr.oprand2.getType() == Result.Type.constant){
            Opcode op = instr.getOp();
            Result res = icGen.constantOp(op, instr.oprand1, instr.oprand2);
            if(res!=null){
                replaceTable.put(instr.getId(), res);
                BasicBlock bb = cfg.getBlock(instr.getBbid());
                bb.deleteInstr(icGen,instr);
            }
        }
    }

    private void putArcherTable(Instruction instr, CFG cfg, IcGenerator icGen){
        if(!archerTable.containsKey(instr.getOp())){
            return;
        }
        BasicBlock bb = cfg.getBlock(instr.getBbid());
        for(Integer id : archerTable.get(instr.getOp())){
            Instruction prev = icGen.getInstruction(id);
            if(bb.isDom(prev.getBbid())||instr.getBbid() == prev.getBbid()){
                if(instr.compareTo(prev) == 0){
                    Result replaced = new Result(Result.Type.instruction, prev.getId());
                    replaceTable.put(instr.getId(), replaced);
                    bb.deleteInstr(icGen, instr);
                    return;
                }
            }
        }
        archerTable.get(instr.getOp()).add(instr.getId());

    }

    private void putSourceTable(Instruction instr, IcGenerator icGen){
        Result source = instr.oprand1;
        Result assigned = instr.oprand2;
        if(!sourceTable.containsKey(assigned.getAddress())){
            sourceTable.put(assigned.getAddress(), new HashMap<>());
        }
        sourceTable.get(assigned.getAddress()).put(assigned.getVersion(),source);

        int tgAddr = assigned.getAddress();
        int tgVer = assigned.getVersion();
        if(targetsTable.containsKey(tgAddr) && targetsTable.get(tgAddr).containsKey(tgVer)){
            for(Integer id : targetsTable.get(tgAddr).get(tgVer)){
                Instruction cached = icGen.getInstruction(id);
                if(cached.oprand1.compareTo(assigned)==0){
                    cached.updatePhi(0, source);
                }else{
                    cached.updatePhi(1, source);
                }
            }
        }
        targetsTable.get(tgAddr).remove(tgVer);
    }

    private void putTargetTable(Instruction instr){
        updateTargetTable(instr.oprand1, instr, 0);
        updateTargetTable(instr.oprand2, instr, 1);
    }

    private void updateTargetTable(Result r, Instruction instr, int i){
        if(r.getType() != Result.Type.variable){
            return;
        }

        int address = r.getAddress();
        int version = r.getVersion();

        if(sourceTable.containsKey(address) && sourceTable.get(address).containsKey(version)){
            Result newr = sourceTable.get(address).get(version);
            instr.updatePhi(i, newr);
        }else{
            if(!targetsTable.containsKey(address)){
                targetsTable.put(address,new HashMap<>());
            }
            if(!targetsTable.get(address).containsKey(version)){
                targetsTable.get(address).put(version, new ArrayList<>());
            }
            targetsTable.get(address).get(version).add(instr.getId());
        }
    }


    private void initArcherTable(){
        archerTable.put(Opcode.neg, new ArrayList<>());
        archerTable.put(Opcode.add, new ArrayList<>());
        archerTable.put(Opcode.sub, new ArrayList<>());
        archerTable.put(Opcode.mul, new ArrayList<>());
        archerTable.put(Opcode.div, new ArrayList<>());
        archerTable.put(Opcode.cmp, new ArrayList<>());
        archerTable.put(Opcode.adda, new ArrayList<>());
        archerTable.put(Opcode.load, new ArrayList<>());
        archerTable.put(Opcode.store, new ArrayList<>());

    }

}
