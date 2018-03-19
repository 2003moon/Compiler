package main;
import backend.CodeGenerator;
import com.sun.tools.javac.jvm.Code;
import exceptions.DuplicateDeclaredException;
import exceptions.NonDeclaredException;
import exceptions.NotDefinedException;
import exceptions.NotExpectedException;
import frontend.*;
import optimizer.Optimizer;
import util.*;

import java.io.IOException;
import java.util.Iterator;

public class test {
    private parser ps;
    private Optimizer opt;

    public test(String filename) throws IOException, DuplicateDeclaredException, NotExpectedException, NotDefinedException,NonDeclaredException {
        ps = new parser(filename);
        ps.parse();
        opt = new Optimizer(ps);
    }

    public void testCodeGenerate() throws IOException{
        CodeGenerator cdGen = new CodeGenerator(opt);
        int[] programs = cdGen.generate();
        DLX.load(programs);
        DLX.execute();
    }
    public void optimize(){
        opt.optimize();
    }

    public void testOptCfgGraph(String prefixname){
        testCfgGraph(prefixname);
    }

    public void testOptDt(String prefixname){
        testDt(prefixname);
    }

    public void testScanner(scanner sc){
        try{
            Token tk = sc.getNextToken();
            while(tk != Token.EOF){
                System.out.print(tk);
                if(tk == Token.NUMBER){
                    System.out.print(" "+sc.getVal());
                }
                if(tk == Token.IDENT){
                    System.out.print(" "+sc.getIdent(sc.getIdentId()));
                }
                System.out.print('\n');
                tk = sc.getNextToken();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void testCfgGraph(String prefixname){
        IcGenerator icGen = ps.getIcGen();
        Iterator<Integer> it = icGen.getCfgMap().keySet().iterator();
        while(it.hasNext()){
            int cfgno = it.next();
            String outputname = prefixname+cfgno;
            VcgPrinter printer = new VcgPrinter(outputname, ps);
            CFG cfg = icGen.getCfg(cfgno);
            printer.printCFG(cfg);
        }
    }

    public void testDt(String prefixname){
        IcGenerator icGen = ps.getIcGen();
        Iterator<Integer> it = icGen.getCfgMap().keySet().iterator();
        while(it.hasNext()){
            int cfgno = it.next();
            String outputname = prefixname+cfgno;
            VcgPrinter printer = new VcgPrinter(outputname, ps);
            CFG cfg = icGen.getCfg(cfgno);
            printer.printDT(cfg);
        }
    }

    public void testColorGraph(String prefixname){
        VcgPrinter printer = new VcgPrinter(prefixname, ps);
        printer.printGraph(opt.getGraph());
    }

    public static void main(String[] args)throws IOException, DuplicateDeclaredException, NotExpectedException, NotDefinedException,NonDeclaredException {
    //   String outputname = "test2";
        String inputname = "testdata/test008.txt";
        test ts = new test(inputname);
        String cfgname = "test8_";
        String dtname = "test8_Dt_";
        String optcfgname = "test8_opt_";
        String optdtname = "test8_Dt_opt_";
        ts.testCfgGraph(cfgname);
        ts.testDt(dtname);
        ts.optimize();
        ts.testOptCfgGraph(optcfgname);
        ts.testOptDt(optdtname);

        String allocatename = "test8_colored_graph";
        ts.testColorGraph(allocatename);

        ts.testCodeGenerate();

    }
}


