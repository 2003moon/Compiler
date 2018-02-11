package util;

import lombok.Getter;

import java.util.*;

@Getter
public class CFG {
    private int firstBB;
    private int currBB;
    private Map<Integer,BasicBlock> bbMap;
 //   private Map<Integer, Result> localVarMap;
    private Set<Integer> varSet;
    private ArrayList<Result> parameters;
    private int id;

    public CFG(int id){
        this.id = id;
        bbMap = new HashMap<>();
        firstBB = 0;
        createBB();
        this.currBB = firstBB;
        parameters = new ArrayList<>();
        varSet = new HashSet<>();
    }


    public void addParam(Result x){
        parameters.add(x);
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

}
