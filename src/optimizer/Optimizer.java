package optimizer;

import frontend.parser;
import lombok.Getter;
import util.*;

import java.util.*;

//TODO: there should be instruction used chain. For each instruction, we need information about what instruction result it produces and where it is used.
public class Optimizer {
    //TODO: implement instruction useage table here.
    private Map<Integer, Map<Integer, Result>> sourceTable;
    private Map<Integer,Map<Integer, ArrayList<Integer>>> targetsTable;
    private Map<Opcode, ArrayList<Integer>> archerTable;
    private Map<Integer,Result> replaceTable;
    @Getter
    private Map<Integer,Set<Integer>> usageTable;
    @Getter
    private Map<InstrNode,Set<InstrNode>> graph;
    @Getter
    private Map<Integer, InstrNode> idToNode;
    @Getter
    private parser ps;

    public Optimizer(parser ps){
        this.ps = ps;
        sourceTable = new HashMap<>();
        targetsTable = new HashMap<>();
        archerTable = new HashMap<>();
        replaceTable = new HashMap<>();
        usageTable = new HashMap<>();
    }

    public void optimize(){
        initArcherTable();
        DominatorTreeComputer dtComp = ps.getDtComp();
        DFS(dtComp.getNode(0));
        RegisterAllocator allocator = new RegisterAllocator(this);
        allocator.buildGraph();
        allocator.color();
        allocator.removePhi();
        graph = allocator.getGraph();
        idToNode = allocator.getIdToNode();

    }

    private void DFS(BasicBlock root){
        IcGenerator icGen = ps.getIcGen();
        CFG cfg = icGen.getMaincfg();
        if(root.isEmpty()){
          /*  Instruction dummy = root.getDummy();
            dummy.prev.next = dummy.next;
            dummy.next.prev = dummy.prev;*/
        }else{
            int first = root.getFirstInstr();
            while(true){
                Instruction instr = icGen.getInstruction(first);
                checkReplace(instr, cfg, icGen);
                if(instr.getBbid() == -1){
                    first = instr.next.getId();
                    continue;
                }
                if(instr.getOp() == Opcode.move){
                    putSourceTable(instr.oprand1, instr.oprand2, icGen);
                    if(first == root.getLastInstr()){
                        root.deleteInstr(icGen, instr);
                        System.out.println("Instruction_"+instr.getId()+" is removed due to Copy Propagation");
                        break;
                    }
                    first = instr.next.getId();
                    root.deleteInstr(icGen, instr);
                    System.out.println("Instruction_"+instr.getId()+" is removed due to Copy Propagation");
                    continue;
                }


                if(instr.getOp() == Opcode.phi){
                    Result source = new Result(Result.Type.instruction, instr.getId());
                    Result assigned = new Result(Result.Type.variable, instr.oprand1.getAddress());
                    assigned.setVersion(instr.getId());
                    putSourceTable(source, assigned, icGen);
                }

                if(instr.getOp()!=Opcode.end && instr.getOp() != Opcode.bra){
                    putTargetTable(instr,cfg, icGen);
                }

                if(instr.getBbid() == -1){
                    first = instr.next.getId();
                    continue;
                }

                putArcherTable(instr, cfg, icGen);
                if(first == root.getLastInstr()){
                    break;
                }
                first = instr.next.getId();
            }
        }


        for(Integer child : root.getChild()){
            DFS(cfg.getBlock(child));
        }
    }

    private void replacePhi(Instruction instr, IcGenerator icGen){
        int addr = instr.oprand1.getAddress(), ver1 = instr.oprand1.getVersion();
        int ver2 = instr.oprand2.getVersion();
        Result source = new Result(Result.Type.instruction, instr.getId());
        Result assigned = new Result(Result.Type.variable, addr);
        assigned.setVersion(instr.getId());
        if(sourceTable.containsKey(addr)){
            if(sourceTable.get(addr).containsKey(ver1)){
                Result newr = sourceTable.get(addr).get(ver1);
                instr.updateOp(0,newr);
            }
            if(sourceTable.get(addr).containsKey(ver2)){
                Result newr = sourceTable.get(addr).get(ver2);
                instr.updateOp(1,newr);
            }
        }
        putSourceTable(source, assigned, icGen);
    }

    private void checkReplace(Instruction instr, CFG cfg, IcGenerator icGen){
        Result r1 = instr.oprand1;
        Result r2 = instr.oprand2;
        if(r1!=null && r1.getType() == Result.Type.instruction){
            //    r1 = findSource(r1);
            if(replaceTable.containsKey(r1.getInstr_id())){
                instr.updateOp(0, replaceTable.get(r1.getInstr_id()));
            }
            if(r1.getType() == Result.Type.instruction){
                putUsageTable(r1.getInstr_id(), instr.getId());
            }
        }

        if(r2!=null && r2.getType() == Result.Type.instruction){
          //  r2 = findSource(r2);
            if(replaceTable.containsKey(r2.getInstr_id())){
                instr.updateOp(1,replaceTable.get(r2.getInstr_id()));
            }

            if(r2.getType() == Result.Type.instruction){
                putUsageTable(r2.getInstr_id(), instr.getId());
            }
        }

        replaceConstant(instr,cfg, icGen);


    }


    private void replaceConstant(Instruction instr, CFG cfg, IcGenerator icGen){
        //TODO: implement the situation for cmp and bge.
        if(instr.oprand1!=null && instr.oprand2!=null && instr.oprand1.getType() == Result.Type.constant && instr.oprand2.getType() == Result.Type.constant){
            Opcode op = instr.getOp();
            Result res = icGen.constantOp(op, instr.oprand1, instr.oprand2);
            if(res!=null){
                replaceTable.put(instr.getId(), res);
                BasicBlock bb = cfg.getBlock(instr.getBbid());
                bb.deleteInstr(icGen,instr);
            }
        }
    }

    private void putUsageTable(int key, int value){
        if(!usageTable.containsKey(key)){
            usageTable.put(key, new HashSet<>());
        }
        usageTable.get(key).add(value);
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
                    System.out.println("Instruction_"+instr.getId()+" is replaced by instruction_"+prev);
                    return;
                }
            }
        }
        archerTable.get(instr.getOp()).add(instr.getId());

    }

    private void putSourceTable(Result source, Result assigned,  IcGenerator icGen){
        //TODO: implement the usageTable
        if(!sourceTable.containsKey(assigned.getAddress())){
            sourceTable.put(assigned.getAddress(), new HashMap<>());
        }
        int srcAddr = source.getAddress();
        int srcVer = source.getVersion();
        while(sourceTable.containsKey(srcAddr) && sourceTable.get(srcAddr).containsKey(srcVer)){
            source = sourceTable.get(srcAddr).get(srcVer);
            if(source.getType() != Result.Type.variable){
                break;
            }
            srcAddr = source.getAddress();
            srcVer = source.getVersion();
        }

        sourceTable.get(assigned.getAddress()).put(assigned.getVersion(),source);

        int tgAddr = assigned.getAddress();
        int tgVer = assigned.getVersion();
        if(source.getType() == Result.Type.constant){
            source = new Result(Result.Type.constant,source.getValue());
        }
        if(targetsTable.containsKey(tgAddr) && targetsTable.get(tgAddr).containsKey(tgVer)){
            for(Integer id : targetsTable.get(tgAddr).get(tgVer)){
                Instruction cached = icGen.getInstruction(id);
                if(cached.oprand1.compareTo(assigned)==0){
                    cached.updateOp(0, source);
                }else{
                    cached.updateOp(1, source);
                }
                if(source.getType() == Result.Type.instruction){
                    putUsageTable(source.getInstr_id(), id);
                }
                //TODO: check if source is instruction, if so, update its usage table.
            }
            targetsTable.get(tgAddr).remove(tgVer);
        }

    }

    private void putTargetTable(Instruction instr, CFG cfg, IcGenerator icGen){
        updateTargetTable(instr.oprand1, instr, 0);
        updateTargetTable(instr.oprand2, instr, 1);
        replaceConstant(instr,cfg,icGen);
    }

    private void updateTargetTable(Result r, Instruction instr, int i){
        if(r==null ||  r.getType() != Result.Type.variable){
            return;
        }

        int address = r.getAddress();
        int version = r.getVersion();

        Result newr = null;
        while(sourceTable.containsKey(address) && sourceTable.get(address).containsKey(version)){
            newr = sourceTable.get(address).get(version);
            address = newr.getAddress();
            version = newr.getVersion();
        }

        if(newr!=null){
            if(newr.getType() == Result.Type.constant){
                newr = new Result(Result.Type.constant,newr.getValue());
            }
            instr.updateOp(i, newr);
            if(newr.getType() == Result.Type.instruction){
                putUsageTable(newr.getInstr_id(),instr.getId());
            }
            //TODO: check if newr is instruction and update instruction usage table.
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

    private Result findSource(Result r) {
        Result replace = r;
        if (replaceTable.containsKey(r.getInstr_id())) {
            replace = replaceTable.get(r.getInstr_id());
            if (replace.getType() == Result.Type.variable) {
                if (sourceTable.containsKey(replace.getAddress()) && sourceTable.get(replace.getAddress()).containsKey(replace.getVersion())) {
                    replace = sourceTable.get(replace.getAddress()).get(replace.getVersion());
                }
            }
        }
        return replace;
    }

}
