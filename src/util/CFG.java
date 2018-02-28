package util;

import lombok.Getter;

import java.util.*;

@Getter

public class CFG {
    private int firstBB;
    private int lastBB;
    private Map<Integer,BasicBlock> bbMap;
 //   private Map<Integer, Result> localVarMap;
    private Map<Integer,Integer> varTable;
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
        varTable= new HashMap<>();
        resumePoints = new LinkedList<>(); //TODO: when generate the code, the point should always points to the header.
    }


    public void addParam(int id, Result x){
        parameters.put(id, x);
    }


    public void addVar(int address, int version){
        varTable.put(address,version);
    }

    public int getVersion(int address){
        return varTable.get(address);
    }
    public boolean isDeclared(int address){
        return varTable.containsKey(address);
    }

    public boolean isDefined(int address){
        return varTable.get(address)!=-1;
    }

    public void updateVersion(int address, int newver){
        varTable.put(address,newver);
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
