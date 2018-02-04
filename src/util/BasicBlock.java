package util;

import lombok.Getter;

import java.util.*;


public class BasicBlock {

    @Getter
    private ArrayList<Instruction> instructions;
    @Getter
    private int id;
    @Getter
    private Set<Integer> successors;
    @Getter
    private Map<Integer, Integer> symbolTable; //variable's version in the current BB

    public BasicBlock(int id){
        this.id = id;
        instructions = new ArrayList<>();
        successors = new HashSet<>();
        symbolTable = new HashMap<>();
    }

    public Instruction getInstr(int instr_id){
        if(instr_id<instructions.size()){
            return instructions.get(instr_id);
        }
        return  null;
    }

    public void addInstr(Instruction instr){
        instructions.add(instr);
    }

    public void addSuccessor(Integer succ_id){
        successors.add(succ_id);
    }

    public void addSymbol(Result res){
        symbolTable.put(res.getAddress(),res.getVersion());
    }
    public void setSymbolTable(Map<Integer,Integer> symbolTable){
        this.symbolTable.putAll(symbolTable);
    }


}
