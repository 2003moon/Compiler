package util;

import lombok.Getter;

import java.util.*;

@Getter
//TODO: CFG needs to manage a sysmbol table including all of the variables with their newes version.joinBlock should commit this table to CFG

public class CFG {
    private int firstBB;
    private int lastBB;
    private Map<Integer,BasicBlock> bbMap;
 //   private Map<Integer, Result> localVarMap;
    private Set<Integer> varSet;
    private Map<Integer,Result> parameters;
    private Queue<Instruction> resumePoints;
    private String name;
    private int id;

    public CFG(int id,String name){
        this.id = id;
        this.name = name;
        bbMap = new HashMap<>();
        firstBB = 0;
        createBB();
        this.lastBB = firstBB;
        parameters = new HashMap<>();
        varSet = new HashSet<>();
        resumePoints = new LinkedList<>(); //TODO: when generate the code, the point should always points to the header.
    }


    public void addParam(int id, Result x){
        parameters.put(id, x);
    }


    public void addVar(int address){
        varSet.add(address);
    }

    public boolean isDeclared(int address){
        return varSet.contains(address);
    }

    public BasicBlock getBlock(int id){
        if(bbMap.containsKey(id)){
            return bbMap.get(id);
        }
        return null;
    }

    public int createBB(){
        int id = bbMap.size();
        BasicBlock bb = new BasicBlock(id, this.id);
        bbMap.put(id,bb);
        return id;
    }

    public boolean isParameter(int param_id){
        return parameters.containsKey(param_id);
    }

    public void addResumePoints(Instruction instr){
        resumePoints.offer(instr);
    }
}
