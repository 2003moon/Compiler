package util;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Array;
import java.util.*;

@Getter
public class BasicBlock {

    public int immDom;
    private int firstInstr;
    private int lastInstr;
    private int id;
    private int cfgid;
    private Set<Integer> successors;
    private Set<Integer> predecessors;
    private ArrayList<Integer> child;
    private TreeMap<Integer, Result> symbolTable; //variable's version in the current BB
    private Map<Integer, phiAssignment> phiTable; // <address, phi function>
    private Map<Integer,Result> usageTable;

    @Setter
    private BrType brType;

    public enum BrType{
        then,
        others;
    }

    public BasicBlock(int id, int cfgid){
        this.id = id;
        this.cfgid = cfgid;
        successors = new HashSet<>();
        predecessors = new HashSet<>();
        child = new ArrayList<>();
        symbolTable = new TreeMap<>();
        phiTable = new HashMap<>();
        usageTable = new HashMap<>();
        firstInstr = -1;
        lastInstr = -1;
        brType = BrType.others;
        immDom = -1;
    }

    public void addChild(int nodeId){
        child.add(nodeId);
    }

    public boolean isDom(int dom){
        return immDom == dom;
    }

    public Result getSymbol(int address){
        return symbolTable.get(address);
    }
    public void addSuccessor(int id){
        successors.add(id);
    }

    public void addPredecessor(int id){
        predecessors.add(id);
    }

    public void addSymbol(Result res){
        symbolTable.put(res.getAddress(), res);
    }

    public void addUsage(Result res){
        usageTable.put(res.getAddress(), res);
    }
    public void setSymbolTable(TreeMap<Integer,Result> symbolTable){
        for(Map.Entry<Integer, Result> entry: symbolTable.entrySet()){
            this.symbolTable.put(entry.getKey(), new Result(entry.getValue()));
        }
    }

    public void updateSymbolUsage(TreeMap<Integer,Result> symbolTable){
        for(Map.Entry<Integer, Result> entry: usageTable.entrySet()){
            int key = entry.getKey();
            Result res = entry.getValue();
            if(symbolTable.containsKey(key)){
                Result res2 = symbolTable.get(key);
                if(res2.getVersion()!=res.getVersion()){
                    res.setVersion(res2.getVersion());
                }
            }
        }
    }

    public void addInstr(IcGenerator icGen, Instruction newInstruction){
        if(firstInstr == -1){
            firstInstr = newInstruction.getId();
            lastInstr = newInstruction.getId();
        }else{

            Instruction lastInstruction = icGen.getInstrTable().get(lastInstr);
            lastInstruction.next = newInstruction.getId();
            lastInstr = newInstruction.getId();
        }
        icGen.addinstraTable(newInstruction.getId(), newInstruction);
    }

    public void addInstrHeader(IcGenerator icGen, Instruction newInstruction){
        if(firstInstr == -1){
            firstInstr = newInstruction.getId();
            lastInstr = newInstruction.getId();
        }else{

            newInstruction.next = firstInstr;
            firstInstr = newInstruction.getId();
        }
        icGen.addinstraTable(newInstruction.getId(), newInstruction);
    }

    public void link(BasicBlock target){
        addSuccessor(target.getId());
        target.addPredecessor(id);
        if(target.getSymbolTable().size() == 0){
            target.setSymbolTable(symbolTable);
        }

    }

    public void insertPhi(IcGenerator icGen, Result r, BrType tp, int backup){
        int address = r.getAddress();
        phiAssignment pa = null;
        if(!phiTable.containsKey(address)){
            pa = new phiAssignment(address);
            pa.updateIth(0,r.getVersion());
            pa.updateIth(1,backup);
            pa.backupV = backup;
            phiTable.put(address, pa);
            pa.constructInstr(icGen, this);
        }else{
            pa = phiTable.get(address);
            if(tp == BrType.then){
                pa.updateIth(0, r.getVersion());
            }else{
                pa.updateIth(1, r.getVersion());
            }

        }
    }


    public void commitPhi(IcGenerator icGen, BasicBlock outerJoinNode){
        CFG cfg = icGen.getCfg(cfgid);
        for(Map.Entry<Integer, phiAssignment> entry: phiTable.entrySet()){
            int key = entry.getKey();
            phiAssignment pa = entry.getValue();
          //  Result newr = new Result(Result.Type.variable, key);
          //  newr.setVersion(instr_id);
            int address = pa.getAddress();
            cfg.updateVersion(address, pa.getInstr().getId());
            if(outerJoinNode!=null){
                Result r = new Result(Result.Type.variable, address);
                r.setVersion(cfg.getVersion(address));
                outerJoinNode.insertPhi(icGen,r,this.brType, pa.backupV);
            }
        }
    }

    public void addBranch(IcGenerator icGen, Opcode brOp, Result r1, Result r2){
        Instruction instr = new Instruction(r1, r2, brOp);
        instr.setId(icGen.getInstrTable().size());
        this.addInstr(icGen, instr);
    }

    public boolean hasPhi(int address){
        return phiTable.containsKey(address);
    }
}
