package util;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Getter
public class CFG {
    private int firstBB;
    private int currBB;
    private Map<Integer,BasicBlock> bbMap;
    private ArrayList<Instruction> totalInstructions;
 //   private Map<Integer, Result> localVarMap;
    private Map<Integer, HashMap<Integer, Integer>> versionMap;
    private ArrayList<Result> parameters;

    public CFG(){
        createFirstBB();
        this.currBB = firstBB;
//        localVarMap = new HashMap<>();
        versionMap = new HashMap<>();
        totalInstructions = new ArrayList<>();
        parameters = new ArrayList<>();
    }

    public int getTotalInstrNum(){
        return totalInstructions.size();
    }

    public void addInstr(Instruction instr){
        totalInstructions.add(instr);
        bbMap.get(currBB).addInstr(instr);
    }

    public void addVarInfo(Result r){
        HashMap bb_ver = new HashMap<>();
        bb_ver.put(currBB,r.getVersion());
        versionMap.put(r.getAddress(),bb_ver);
    }

    public void updateLocalVersion(Result r, int version){
        r.setVersion(version);
        HashMap bb_ver = versionMap.get(r.getAddress());
        bb_ver.put(currBB, version);
        versionMap.put(r.getAddress(),bb_ver);
    }

    public void addParam(Result x){
        parameters.add(x);
        addVarInfo(x);
    }


    public int getVersion(int address){
        HashMap<Integer, Integer> bb_ver = versionMap.get(address);
        return bb_ver.get(currBB);
    }

    public BasicBlock getBlock(int id){
        if(bbMap.containsKey(id)){
            return bbMap.get(id);
        }
        return null;
    }


    private void createFirstBB(){
        this.firstBB = 0;
        BasicBlock bb = new BasicBlock(firstBB);
        bbMap = new HashMap<>();
        bbMap.put(this.firstBB, bb);

    }


}
