package optimizer;


import frontend.parser;
import lombok.Getter;
import util.*;

import java.util.*;

public class RegisterAllocator {
//TODO: after succesfully allocate register, remove phi function(replace the register)
    private final static int regNum = 8;

    private Optimizer opti;
    @Getter
    private Map<InstrNode, Set<InstrNode>> graph;
    @Getter
    private Map<Integer, InstrNode> idToNode;
    private Map<InstrNode,Integer> degree;
    private Set<Integer> phis;
    private Stack<InstrNode> nds;
    private boolean[] regStat;



    public RegisterAllocator(Optimizer opti){
        this.opti = opti;
        graph = new HashMap<>();
        degree = new HashMap<>();
        idToNode = new HashMap<>();
        phis = new HashSet<>();
        nds = new Stack<>();
        regStat = new boolean[regNum];
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

    public void removePhi(){//TODO: it is possible that result is a register.
        parser ps = opti.getPs();
        IcGenerator icGen = ps.getIcGen();
        CFG cfg = icGen.getCfg(0);
        for(Integer instrid : phis){
            InstrNode nd = idToNode.get(instrid);
            if(nd.removed()){

            }else{
                if(instrid==24){
                    int test = 0;
                }
                Instruction instr = icGen.getInstruction(instrid);
                BasicBlock bb = cfg.getBlock(instr.getBbid());
                Result opr1 = instr.oprand1;
                Result opr2 = instr.oprand2;
                int pred1 = bb.findPred(instr.getPred1(), instr.getPred2());
                int pred2 = bb.findPred(instr.getPred2(), instr.getPred1());
                BasicBlock predBB1 = cfg.getBlock(pred1);
                BasicBlock predBB2 = cfg.getBlock(pred2);
                Result res = new Result(Result.Type.register, nd.regNo);

                if(opr1.getType() == Result.Type.constant && opr2.getType() == Result.Type.constant){
                   Instruction instr1 = new Instruction(opr1,res, Opcode.move);
                   instr1.setId(icGen.getInstrTable().size());
                   predBB1.addInstr(icGen,instr1);
                   Instruction instr2 = new Instruction(opr2, res, Opcode.move);//TODO: instr's id should be set.
                   instr2.setId(icGen.getInstrTable().size());
                   predBB2.addInstr(icGen,instr2);
                }else if(opr1.getType() == Result.Type.constant){
                    Instruction instr1 = new Instruction(opr1,res,Opcode.move);
                    instr1.setId(icGen.getInstrTable().size());
                    predBB1.addInstr(icGen, instr1);
                    InstrNode nd2 = idToNode.get(opr2.getInstr_id());
                    if(nd.regNo != nd2.regNo){
                        Instruction instr2 = new Instruction(opr2,res,Opcode.move);
                        instr2.setId(icGen.getInstrTable().size());
                        predBB2.addInstr(icGen,instr2);
                    }
                }else if(opr2.getType() == Result.Type.constant){
                    Instruction instr2 = new Instruction(opr2,res,Opcode.move);
                    instr2.setId(icGen.getInstrTable().size());
                    predBB2.addInstr(icGen, instr2);
                    InstrNode nd1 = idToNode.get(opr1.getInstr_id());
                    if(nd.regNo != nd1.regNo){
                        Instruction instr1 = new Instruction(opr1,res,Opcode.move);
                        instr1.setId(icGen.getInstrTable().size());
                        predBB1.addInstr(icGen,instr1);
                    }
                }else{
                    InstrNode nd1 = idToNode.get(opr1.getInstr_id());
                    InstrNode nd2 = idToNode.get(opr2.getInstr_id());
                    if(nd.regNo != nd1.regNo){
                        Instruction instr1 = new Instruction(opr1, res, Opcode.move);
                        instr1.setId(icGen.getInstrTable().size());
                        predBB1.addInstr(icGen, instr1);
                    }

                    if(nd.regNo != nd2.regNo){
                        Instruction instr2 = new Instruction(opr2, res, Opcode.move);
                        instr2.setId(icGen.getInstrTable().size());
                        predBB2.addInstr(icGen, instr2);
                    }

                }
            }
        }
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
            if(nb.removed()){
                continue;
            }
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
        IcGenerator icGen =  opti.getPs().getIcGen();
        CFG cfg = icGen.getCfg(0);
        while(!nds.isEmpty()){
            InstrNode nd = nds.pop();
            if(nd.getInstrId() == 6){
                int test = 0;
            }
            Set<InstrNode> neighbors = graph.get(nd);
            Set<Integer> unavaliableColor = new HashSet<Integer>();
            for(InstrNode nb: neighbors){
                if(!nb.removed()&& nb.colored()){
                    unavaliableColor.add(nb.regNo);
                }
            }
            for(int i = 1;i<regNum;i++){
                if(!unavaliableColor.contains(i)){
                    Instruction instr = icGen.getInstruction(nd.getInstrId());
                    Result res = new Result(Result.Type.register, i);
                /*    if(instr.getOp()!=Opcode.phi && instr.oprand1!=null &&  instr.oprand2!=null && instr.oprand1.getType() == Result.Type.constant
                            && instr.oprand2.getType() == Result.Type.constant){
                        instr.oprand1.setType(Result.Type.register);
                        instr.oprand1.setRegNo(i);
                        Instruction load = new Instruction(new Result(Result.Type.constant, instr.oprand1.getValue()),
                                res, Opcode.move);
                        load.setId(icGen.getInstrTable().size());
                        BasicBlock bb = cfg.getBlock(instr.getBbid());
                        bb.addInstrBefore(icGen,load,instr);
                    }*/
                    nd.regNo = i;
                    nd.isRemoved = false;
                    regStat[i] = true;
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
            if(instr_id==24){
                int test = 0;
            }
            Instruction var = icGen.getInstruction(instr_id);
            if(var.isPhi()){
                phis.add(var.getId());
            }
            if(var.getBbid() == -1){
                continue;
            }
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
            Instruction pred_instr = instr.getPrev();
            if(!pred_instr.isDummy()){
                LiveOutAtStatement(pred_instr, var, M);
            }
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
        IcGenerator icGen = opti.getPs().getIcGen();
        if(instr.isRegisterNeed()){
            addEdges(instr,var);
       /*     Result opr1 = instr.oprand1;
            Result opr2 = instr.oprand2;
            if(opr1!=null && opr1.getType() == Result.Type.instruction){
                addEdges(icGen.getInstruction(opr1.getInstr_id()), var);
            }
            if(opr2!=null && opr2.getType() == Result.Type.instruction){
                addEdges(icGen.getInstruction(opr2.getInstr_id()),var);
            }*/
        }
        if(instr.getId()!=var.getId()){
            LiveInAtStatement(instr,var,M);
        }
    }

    private void addEdges(Instruction instr1, Instruction instr2){


        if(!idToNode.containsKey(instr1.getId())){
            InstrNode nd1 = new InstrNode(instr1.getId());
            graph.put(nd1, new HashSet<>());
            idToNode.put(instr1.getId(), nd1);
        }

        if(!idToNode.containsKey(instr2.getId())){
            InstrNode nd2 = new InstrNode(instr2.getId());
            graph.put(nd2, new HashSet<>());
            idToNode.put(instr2.getId(), nd2);
        }

        InstrNode nd1 = idToNode.get(instr1.getId());
        InstrNode nd2 = idToNode.get(instr2.getId());

        if(!nd1.equals(nd2)){
            graph.get(nd1).add(nd2);
            graph.get(nd2).add(nd1);
        }


    }


}
