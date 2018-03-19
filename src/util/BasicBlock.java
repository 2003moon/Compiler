package util;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Array;
import java.util.*;

@Getter
public class BasicBlock {
    //TODO: implement branch remove for constant folding.

    public int immDom;
    private int firstInstr;
    private int lastInstr;
    private int totalInstrs;
    private int id;
    private int cfgid;
    private Set<Integer> successors;
    private Set<Integer> predecessors;
    private ArrayList<Integer> child;
    private TreeMap<Integer, Result> symbolTable; //variable's version in the current BB
    @Getter
    private Map<Integer, phiAssignment> phiTable; // <address, phi function>
    private Map<Integer,Result> usageTable;
    @Getter
    private Instruction dummy;


    @Setter
    private BrType brType;

    public enum BrType{
        Then,
        Elese,
        If,
        While,
        Loopbody,
        body;

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
        totalInstrs = 0;
        brType = BrType.body;
        immDom = -1;
    }

    public void addChild(int nodeId){
        child.add(nodeId);
    }

    public void increTotalInstr(){
        totalInstrs++;
    }



    public boolean isDom(int dom){
        return immDom == dom;
    }

    public Instruction createDummyInstr(){
        dummy = new Instruction(null, null, Opcode.dummy);
        dummy.setId(-2);
        dummy.setBbid(id);
        return dummy;
    }

    public boolean isEmpty(){
        return firstInstr == -1;
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

    public void addInstrBefore(IcGenerator icGen, Instruction newInstruction, Instruction next){
        next.linkPrevInstr(newInstruction);
        if(next.getId() == firstInstr){
            firstInstr = newInstruction.getId();
        }
        newInstruction.setBbid(id);
        icGen.addinstraTable(newInstruction.getId(), newInstruction);
        totalInstrs++;

    }

    public void addInstr(IcGenerator icGen, Instruction newInstruction){
        if(firstInstr == -1){
            firstInstr = newInstruction.getId();
            lastInstr = newInstruction.getId();
            if(dummy!=null){
               dummy.linkNextInstr(newInstruction);
            }
        }else{
            Instruction lastInstruction = icGen.getInstrTable().get(lastInstr);
            lastInstruction.linkNextInstr(newInstruction);
        /*    newInstruction.next = lastInstruction.next;
            lastInstruction.next = newInstruction;
            newInstruction.prev = lastInstruction;
            if(newInstruction.next!=null){
                newInstruction.next.prev = newInstruction;
            }*/

            lastInstr = newInstruction.getId();
        }
        newInstruction.setBbid(id);
        icGen.addinstraTable(newInstruction.getId(), newInstruction);
        totalInstrs++;
    }

    public void deleteInstr(IcGenerator icGen, Instruction instr){//TODO: if deletion causes empty block, dummy instruction should be inserted to keep the link information of the last instruction and the next.
        int instr_id = instr.getId();
        if(instr_id == 4){
            int test = 0;
        }
        Instruction prev = instr.prev;
        Instruction next = instr.next;

        if(instr_id == firstInstr && instr_id == lastInstr){
            firstInstr = -1;
            lastInstr = -1;
            createDummyInstr();
            if(prev!=null){
                prev.next = dummy;
            }

            if(next!=null){
                next.prev = dummy;
            }
            dummy.prev = prev;
            dummy.next = next;

        }else{
            if(instr_id == firstInstr){
                firstInstr = next.getId();
            }else if (instr_id == lastInstr){
                lastInstr = prev.getId();
            }
            if(prev!=null){
                prev.next = next;
            }
            if(next!=null){
                next.prev = prev;
            }

        }
        totalInstrs--;
        instr.setBbid(-1);
    }

    public void addInstrHeader(IcGenerator icGen, Instruction newInstruction){
        if(firstInstr == -1){
            firstInstr = newInstruction.getId();
            lastInstr = newInstruction.getId();
        }else{
            Instruction first = icGen.getInstruction(firstInstr);
            first.prev = newInstruction;
            newInstruction.next = first;
            firstInstr = newInstruction.getId();
        }
        newInstruction.setBbid(id);
        icGen.addinstraTable(newInstruction.getId(), newInstruction);
        totalInstrs++;

    }

    public void insertBetween(Instruction prev, Instruction next){

    }

    public void link(BasicBlock target){
        addSuccessor(target.getId());
        target.addPredecessor(id);
        if(target.getSymbolTable().size() == 0){
            target.setSymbolTable(symbolTable);
        }

    }

    public void insertPhi(IcGenerator icGen, Result r, BasicBlock bb, int backup){
        int address = r.getAddress();
        phiAssignment pa = null;
        if(!phiTable.containsKey(address)){
            pa = new phiAssignment(address);
            pa.updateIth(0,r.getVersion(), bb.getId());
            pa.updateIth(1,backup,-1);
            pa.backupV = backup;
            phiTable.put(address, pa);
            pa.constructInstr(icGen, this);
        }else{
            pa = phiTable.get(address);
            if(bb.getBrType() == BrType.Then){
                pa.updateIth(0, r.getVersion(), bb.getId());
            }else{
                pa.updateIth(1, r.getVersion(), bb.getId());
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
                outerJoinNode.insertPhi(icGen,r,this, pa.backupV);
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

    public int findPred(int target, int helper){
        if(target == -1){
            for(Integer bbid : predecessors){
                if(bbid != helper){
                    return bbid;
                }
            }
        }

        return target;
    }
}
