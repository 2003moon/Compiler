package util;

import frontend.parser;
import optimizer.InstrNode;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;

public class VcgPrinter {
    private PrintWriter ptw;
    private parser ps;
    private CFG cfg;
    private DominatorTreeComputer dtComp;
    private IcGenerator icGen;

    public VcgPrinter(String filename, parser ps){
        try{
            ptw = new PrintWriter(new FileWriter("vcg/"+filename+".vcg"));
        }catch (IOException er){
            System.out.println(er.getMessage());
        }
        this.ps = ps;
        this.icGen = ps.getIcGen();

    }

    public void printCFG(CFG cfg){
        this.cfg = cfg;
        ptw.println("graph: { title: \"Control Flow Graph:"+cfg.getId() + "\"");
        ptw.println("layoutalgorithm: dfs");
        ptw.println("manhattan_edges: yes");
        ptw.println("smanhattan_edges: yes");
        for(BasicBlock bb : cfg.getBbMap().values()){
            printCfgNode(bb);
        }
        ptw.println("}");
        ptw.close();
    }

    public void printDT(CFG cfg) {
        this.dtComp = ps.getDtComp();
      //  dtComp.computing();
        BasicBlock root = dtComp.getNode(0);
        ptw.println("graph: { title: \"Dominant Tree\"");
        ptw.println("layoutalgorithm: dfs");
        ptw.println("manhattan_edges: yes");
        ptw.println("smanhattan_edges: yes");

        printDtDFS(root);

        ptw.println("}");
        ptw.close();
    }

    public void printGraph(Map<InstrNode, Set<InstrNode>> graph){
        for(Map.Entry<InstrNode, Set<InstrNode>> entry:graph.entrySet()){
            InstrNode firstNode = entry.getKey();
            break;
        }
        ptw.println("graph: { title: \"Graph\"");
        ptw.println("layoutalgorithm: dfs");
        ptw.println("manhattan_edges: yes");
        ptw.println("smanhattan_edges: yes");



    }

    private void printGraphDfs(InstrNode nd, Map<InstrNode, Set<InstrNode>> graph, Map<Integer, Boolean> visited){
        if(visited.get(nd.getInstrId())){
            return;
        }
        visited.put(nd.getInstrId(), true);
        printGraphNode(nd, graph.get(nd));
        for(InstrNode child : graph.get(nd)){
            printGraphDfs(child, graph, visited);
        }
    }


    private void printGraphNode(InstrNode nd, Set<InstrNode> children){
        ptw.println("node: {");
        ptw.println("title: \"" + nd.getInstrId() + "\"");
        ptw.println("label: \"" + nd.getInstrId() + "[");

        if(nd.removed()){
            ptw.println("spilled");
        }else{
            ptw.println("regNo = "+nd.regNo);
        }

        ptw.println("]\"");
        ptw.println("}");

        if(!nd.removed()){
            for(InstrNode nb : children){
                if(!nb.removed()){
                    printEdge(nd.getInstrId(), nb.getInstrId());
                }
            }
        }

    }

    private  void printDtDFS(BasicBlock node){
        printDtNode(node);
        ArrayList<Integer> childs = node.getChild();
        for(int i = 0;i<childs.size();i++){
            printDtDFS(dtComp.getNode(childs.get(i)));
        }
    }

    private void printDtNode(BasicBlock node){
        ptw.println("node: {");
        ptw.println("title: \"" + node.getId() + "\"");
        ptw.println("label: \"" + node.getId() + "[");


        printInstruction(node);

        ptw.println("]\"");
        ptw.println("}");

        ArrayList<Integer> childs = node.getChild();
        for(int i = 0;i<childs.size();i++){
            printEdge(node.getId(),childs.get(i));
        }
    }
    private void printCfgNode(BasicBlock bb){
        ptw.println("node: {");
        ptw.println("title: \"" + bb.getId() + "\"");
        ptw.println("label: \"" + bb.getId() + "[");

        printInstruction(bb);
 /*       int inid = bb.getFirstInstr();
        IcGenerator icGen = ps.getIcGen();
        while(inid != bb.getLastInstr()){
            Instruction instr = icGen.getInstruction(inid);
            printInstruction(instr);
            inid = instr.next;
        }
        Instruction instr = icGen.getInstruction(inid);
        printInstruction(instr);*/
        ptw.println("]\"");
        ptw.println("}");
        for(Integer bb_id:bb.getSuccessors()){
            printEdge(bb.getId(),bb_id);
        }
    }

    private void printInstruction(BasicBlock bb){
        int first = bb.getFirstInstr();
        if(first == -1){
            ptw.println("empty block");
        }else{
            while(first != bb.getLastInstr()){
                Instruction instr = icGen.getInstruction(first);
                String line = instr.toString(ps.getSc(), cfg);
                ptw.println(line);
                first = instr.next;
            }
            Instruction instr = icGen.getInstruction(first);
            String line = instr.toString(ps.getSc(), cfg);
            ptw.println(line);
        }

    }

    public void printEdge(int source, int target){
        ptw.println("edge: { sourcename: \"" + source + "\"");
        ptw.println("targetname: \"" + target + "\"");
        ptw.println("color: blue");
        ptw.println("}");
    }
}
