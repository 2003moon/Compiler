package util;

import frontend.parser;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

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
