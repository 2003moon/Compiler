package main;
import exceptions.DuplicateDeclaredException;
import exceptions.NonDeclaredException;
import exceptions.NotDefinedException;
import exceptions.NotExpectedException;
import frontend.*;
import optimizer.Optimizer;
import util.CFG;
import util.IcGenerator;
import util.Token;
import util.VcgPrinter;

import java.io.IOException;
import java.util.Iterator;

public class test {
    private parser ps;
    private Optimizer opt;

    public test(String filename) throws IOException, DuplicateDeclaredException, NotExpectedException, NotDefinedException,NonDeclaredException {
        ps = new parser(filename);
        ps.parse();
        opt = new Optimizer(ps);
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

    public static void main(String[] args)throws IOException, DuplicateDeclaredException, NotExpectedException, NotDefinedException,NonDeclaredException {
    //   String outputname = "test2";
        String inputname = "testdata/test007.txt";
        test ts = new test(inputname);
        String cfgname = "test7_";
        String dtname = "test7_Dt_";
        String optcfgname = "test7_opt_";
        String optdtname = "test7_Dt_opt_";
        ts.testCfgGraph(cfgname);
        ts.testDt(dtname);
        ts.testOptCfgGraph(optcfgname);
        ts.testOptDt(optdtname);

    }
}


