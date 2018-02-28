package main;
import exceptions.DuplicateDeclaredException;
import exceptions.NonDeclaredException;
import exceptions.NotDefinedException;
import exceptions.NotExpectedException;
import frontend.*;
import util.CFG;
import util.IcGenerator;
import util.Token;
import util.VcgPrinter;

import java.io.IOException;
import java.util.Iterator;

public class test {
    private parser ps;

    public test(String filename) throws IOException, DuplicateDeclaredException, NotExpectedException, NotDefinedException,NonDeclaredException {
        ps = new parser(filename);
        ps.parse();
    }
    private void testScanner(scanner sc){
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
    private void testCfgGraph(){
        IcGenerator icGen = ps.getIcGen();
        Iterator<Integer> it = icGen.getCfgMap().keySet().iterator();
        while(it.hasNext()){
            int cfgno = it.next();
            String outputname = "test22_"+cfgno;
            VcgPrinter printer = new VcgPrinter(outputname, ps);
            CFG cfg = icGen.getCfg(cfgno);
            printer.printCFG(cfg);
        }
    }

    private void testDt(){
        IcGenerator icGen = ps.getIcGen();
        Iterator<Integer> it = icGen.getCfgMap().keySet().iterator();
        while(it.hasNext()){
            int cfgno = it.next();
            String outputname = "test22_"+cfgno+"_Dt";
            VcgPrinter printer = new VcgPrinter(outputname, ps);
            CFG cfg = icGen.getCfg(cfgno);
            printer.printDT(cfg);
        }
    }

    public static void main(String[] args)throws IOException, DuplicateDeclaredException, NotExpectedException, NotDefinedException,NonDeclaredException {
    //   String outputname = "test2";
        String inputname = "testdata/test022.txt";
        test ts = new test(inputname);
        ts.testCfgGraph();
        ts.testDt();
    }
}


