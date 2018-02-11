package util;

import lombok.Getter;

import java.util.*;

@Getter
public class BasicBlock {

    private int firstInstr;
    private int lastInstr;
    private int id;
    private int cfgid;
    private Set<Integer> successors;
    private Set<Integer> predecessors;
    private TreeMap<Integer, Result> symbolTable; //variable's version in the current BB

    public BasicBlock(int id, int cfgid){
        this.id = id;
        this.cfgid = cfgid;
        successors = new HashSet<>();
        predecessors = new HashSet<>();
        symbolTable = new TreeMap<>();
        firstInstr = -1;
        lastInstr = -1;
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

    public void setSymbolTable(TreeMap<Integer,Result> symbolTable){
        for(Map.Entry<Integer, Result> entry: symbolTable.entrySet()){
            this.symbolTable.put(entry.getKey(), entry.getValue());
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
    }


    public void link(BasicBlock target){
        addSuccessor(target.getId());
        target.addPredecessor(id);
    }

    public void insertPhi(IcGenerator icGen, TreeMap<Integer,Result> newSymbolTable){
        Iterator currItr = symbolTable.keySet().iterator();
        Iterator newItr = newSymbolTable.keySet().iterator();
        while(currItr.hasNext()&&newItr.hasNext()){
            int var1 = (Integer)currItr.next();
            int var2 = (Integer)newItr.next();
            if(var1 == var2){
                Result r1 = symbolTable.get(var1);
                Result r2 = newSymbolTable.get(var2);
                if(r1.getVersion() != r2.getVersion()){
                    Instruction instr = new Instruction(r1, r2, Opcode.phi);
                    instr.setId(icGen.getInstrTable().size());
                    icGen.addinstraTable(instr.getId(), instr);
                    instr.next = firstInstr;
                    firstInstr = instr.getId();
                }
            }
        }
    }
}
