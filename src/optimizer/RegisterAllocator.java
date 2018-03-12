package optimizer;


import frontend.parser;
import lombok.Getter;
import util.*;

import java.util.*;

public class RegisterAllocator {

    private final static int regNum = 8;

    private Optimizer opti;
    @Getter
    private Map<InstrNode, Set<InstrNode>> graph;
    private Map<InstrNode,Integer> degree;
    private Stack<InstrNode> nds;



    public RegisterAllocator(Optimizer opti){
        this.opti = opti;
        graph = new HashMap<>();
        degree = new HashMap<>();
        nds = new Stack<>();
    }

    public void buildGraph(){
        LivenessAnalysis();
    }

    public void color(){
        for(Map.Entry<InstrNode, Set<InstrNode>> entry : graph.entrySet()){
            degree.put(entry.getKey(), entry.getValue().size());
        }
        simplify();
        select();

    }

    private void simplify(){
        for(InstrNode nd : graph.keySet()){
            dfsSimplify(nd);
        }
    }

    private void dfsSimplify(InstrNode nd){
        if(nd.removed()){
            return;
        }
        nds.push(nd);
        nd.isRemoved = true;
        for(InstrNode nb: graph.get(nd)){
            int dg = degree.get(nb);
            dg--;
            degree.put(nb,dg);
            if(dg < regNum){
                dfsSimplify(nb);
            }
        }
        if(degree.get(nd)>= regNum){
            nd.isSpilled = true;
        }
    }

    private void select(){
        while(!nds.isEmpty()){
            InstrNode nd = nds.pop();
            Set<InstrNode> neighbors = graph.get(nds);
            Set<Integer> unavaliableColor = new HashSet<Integer>();
            for(InstrNode nb: neighbors){
                if(!nb.removed()&& nb.colored()){
                    unavaliableColor.add(nb.regNo);
                }
            }
            for(int i = 1;i<regNum;i++){
                if(!unavaliableColor.contains(i)){
                    nd.regNo = i;
                    nd.isRemoved = false;
                    break;
                }
            }
            if(nd.spilled()&&!nd.removed()){
                nd.isSpilled = false;
            }
        }
    }

    private void LivenessAnalysis(){
        parser ps = opti.getPs();
        IcGenerator icGen = ps.getIcGen();
        CFG cfg = icGen.getCfg(0);
        Map<Integer, Set<Integer>> usageTable = opti.getUsageTable();
        for(Map.Entry<Integer, Set<Integer>> entry : usageTable.entrySet()){
            Set<Integer> visitedBB = new HashSet<>();
            int instr_id = entry.getKey();
            Instruction var = icGen.getInstruction(instr_id);
            for(Integer use_id : entry.getValue()){
                Instruction instr = icGen.getInstruction(use_id);
                if(instr.getBbid() == -1){
                    continue;
                }
                if(instr.getOp() == Opcode.phi){
                    //TODO: how to find the ith predecessors?
                    BasicBlock bb = cfg.getBlock(instr.getBbid());
                    if(instr.oprand1.getType() == Result.Type.instruction && instr.oprand1.getInstr_id() == var.getId()){
                        int pred_id = bb.findPred(instr.getPred1(), instr.getPred2());
                        LiveOutAtBlock(cfg.getBlock(pred_id),var, visitedBB);
                    }else{
                        int pred_id = bb.findPred(instr.getPred2(), instr.getPred1());
                        LiveOutAtBlock(cfg.getBlock(pred_id),var,visitedBB);
                    }

                }else{
                    LiveInAtStatement(instr, var, visitedBB);
                }
            }
        }

    }

    private void LiveInAtStatement(Instruction instr, Instruction var, Set<Integer> M){
        parser ps = opti.getPs();
        CFG cfg = ps.getIcGen().getCfg(0);
        BasicBlock bb = cfg.getBlock(instr.getBbid());
        if(instr.getId() == bb.getFirstInstr()){
            for(Integer bbid: bb.getPredecessors()){
                BasicBlock pred = cfg.getBlock(bbid);
                LiveOutAtBlock(pred, var, M);
            }
        }else{
            Instruction pred_instr = ps.getIcGen().getInstruction(instr.getPrev());
            LiveOutAtStatement(pred_instr, var, M);
        }

    }

    private void LiveOutAtBlock(BasicBlock bb, Instruction var, Set<Integer> M){
        parser ps = opti.getPs();
        if(!M.contains(bb.getId()) && bb.getFirstInstr()!=-1){
            M.add(bb.getId());
            Instruction last = ps.getIcGen().getInstruction(bb.getLastInstr());
            LiveOutAtStatement(last, var, M);
        }
    }

    private void LiveOutAtStatement(Instruction instr, Instruction var, Set<Integer> M){
        if(instr.getId()!=var.getId() && instr.isRegisterNeed()){
            addEdges(instr,var);
        }
        if(instr.getId()!=var.getId()){
            LiveInAtStatement(instr,var,M);
        }
    }

    private void addEdges(Instruction instr1, Instruction instr2){
        InstrNode nd1 = new InstrNode(instr1.getId());
        InstrNode nd2 = new InstrNode(instr2.getId());
        if(!graph.containsKey(nd1)){
            graph.put(nd1, new HashSet<>());
        }

        if(!graph.containsKey(nd2)){
            graph.put(nd2, new HashSet<>());
        }

        graph.get(nd1).add(nd2);
        graph.get(nd2).add(nd1);
    }


}
