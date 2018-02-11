package frontend;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import exceptions.NonDeclaredException;
import util.*;
import exceptions.NotExpectedException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class parser {
    private Token currToken;
    private IcGenerator icGen;
    private scanner sc;
    private int FP;

    public parser(String path) throws IOException{
        sc = new scanner(path);
        icGen = new IcGenerator();
        FP = 0;
        next();
    }

    public void parse(CFG cfg) throws NonDeclaredException, NotExpectedException, IOException{
        computation(cfg);
    }

    private void computation(CFG cfg) throws NonDeclaredException, NotExpectedException, IOException{
        if(currToken!=Token.MAIN){
            throw new NotExpectedException("MAIN is expected");
        }
        next();
        varDecl(cfg);
        next();
        funcDecl();
        next();
        if(currToken!= Token.LEFT_BRACE){
            throw new NotExpectedException("{ is expected");
        }
        next();
        statSequence(cfg.getBlock(cfg.getFirstBB()),cfg);
        if (currToken!=Token.RIGHT_BRACE){
            throw new NotExpectedException("} is expected");
        }

    }

    private void varDecl(CFG currcfg) throws NotExpectedException, IOException{
        if(!typeDecl()){
            return;
        }
        if(currToken!=Token.IDENT){
            throw new NotExpectedException("ident is expected");
        }
        while(currToken == Token.IDENT){
            int address = sc.getIdentId();
            currcfg.addVar(address);
            next();
            if(currToken != Token.COMMA){
                break;
            }
            next();
            if(currToken != Token.IDENT){
               throw new NotExpectedException("ident is expected");
            }
        }
        if(currToken!=Token.SEMICOMA){
            throw new NotExpectedException("; is expected");
        }
    }

    private boolean typeDecl() throws NotExpectedException, IOException{
        if(currToken!=Token.VAR && currToken != Token.ARRAY){
            return false;
        }
        if(currToken == Token.ARRAY){ //TODO: do I need to record the number ?
            next();
            while(currToken == Token.LEFT_BRACKET){
                next();
                if(currToken != Token.NUMBER){
                    throw new NotExpectedException("number is expected");
                }
                next();
                if(currToken != Token.RIGHT_BRACKET){
                    throw new NotExpectedException("] is expected");
                }
                next();
            }
        }else{
            next();
        }
        return true;

    }

    private void funcDecl()throws NonDeclaredException, NotExpectedException, IOException{
        if(currToken!=Token.FUNCTION && currToken != Token.PROCEDURE){
            return;
        }
        next();
        if(currToken!=Token.IDENT){
            throw new NotExpectedException("ident is expected");
        }
        int funcid = sc.getIdentId();
        icGen.createCfg(funcid);
        next();
        formalParam(funcid);
        next();
        if(currToken!=Token.SEMICOMA){
            throw new NotExpectedException("; is expected");
        }
        next();
        funcBody(funcid);
    }

    private void formalParam(int funcid)throws NotExpectedException, IOException{
        if(currToken!=Token.LEFT_PAR){
            return;
        }

        next();

        if (currToken!=Token.IDENT){
            throw new NotExpectedException("ident is expected");
        }

        CFG cfg = icGen.getCfg(funcid);
        while(currToken==Token.IDENT){
            Result x = new Result(Result.Type.variable, sc.getIdentId());
            cfg.addParam(x);
            next();
            if(currToken!=Token.COMMA){
                break;
            }
            next();
            if (currToken!=Token.IDENT){
                    throw new NotExpectedException("ident is expected");
            }
        }
        if(currToken!=Token.RIGHT_PAR){
            throw new NotExpectedException(") is expected");
        }
    }
    private void funcBody(int funcid)throws NonDeclaredException, NotExpectedException, IOException{
        CFG cfg = icGen.getCfg(funcid);
        varDecl(cfg);
        next();
        if (currToken != Token.LEFT_BRACE){
            throw new NotExpectedException("{ is expected");
        }
        next();
        statSequence(cfg.getBlock(cfg.getFirstBB()), cfg);
        if (currToken!= Token.RIGHT_BRACE){
            throw new NotExpectedException("{ is expected");
        }
    }

    private void statSequence(BasicBlock bb, CFG cfg)throws NonDeclaredException, NotExpectedException, IOException{
        if(!isStatement()){
            throw new NotExpectedException("statement is expected");
        }
        while(isStatement()){
            statement(bb,cfg);
            next();
            if(currToken !=Token.SEMICOMA){
                break;
            }
            next();
        }
    }

    private boolean isStatement(){
        return currToken == Token.LET || currToken == Token.CALL || currToken == Token.IF
                || currToken == Token.WHILE || currToken == Token.RETURN;
    }

    private void statement(BasicBlock bb, CFG cfg) throws NonDeclaredException, NotExpectedException, IOException{
        Result x = null;
        if(currToken == Token.LET){
            next();
            x = assignment(bb, cfg);
        }else if (currToken == Token.CALL){
            next();
            int funcid = sc.getIdentId();
            if(funcid == 1){
                x = icGen.combine(Opcode.read, null, null,  bb);
            }else if (funcid == 2){
                next();
                x = new Result(Result.Type.variable,sc.getIdentId());
                x = icGen.combine(Opcode.write, x, null, bb);
            }else if (funcid == 3){
                icGen.combine(Opcode.writeNL,x, null, bb);
            }else{
                CFG func = icGen.getCfg(funcid);
                x = funcCall(func);
                icGen.combine(Opcode.bra, x, null, bb);
            }
            next();
        }else if (currToken == Token.IF){
           x = ifStatement(bb, cfg);
           Instruction last = icGen.getInstruction(bb.getLastInstr());
           last.updateOp(x);
           next();
        }else if (currToken == Token.WHILE){
            if(bb.getFirstInstr()!=-1){
                int newid = cfg.createBB();
                BasicBlock newbb = cfg.getBlock(newid);
                bb.link(newbb);
                bb = newbb;
            }
            next();
            x = whileStatement(bb,cfg);
            Instruction last = icGen.getInstruction(bb.getLastInstr());
            last.updateOp(x);
            bb = cfg.getBlock(x.getBb_id());
            next();
        }else{
            next();
            x = expression(bb);
        }
    }

    private void relation(BasicBlock bb) throws NonDeclaredException, NotExpectedException, IOException{
        Result x = expression(bb);
        if(!isRelOp(currToken)){
            throw new NotExpectedException("relOp is expected");
        }
        Token relTk = currToken;
        Opcode op = Opcode.tokenToOp(currToken);
        next();
        Result y = expression(bb);
        y = icGen.combine(Opcode.cmp,x,y,bb);
        if(relTk == Token.EQ){
            icGen.combine(Opcode.bne, y, null, bb);
        }else if(relTk == Token.NEQ){
            icGen.combine(Opcode.beq,y, null, bb);
        }else if (relTk == Token.LE){
            icGen.combine(Opcode.bge, y, null, bb);
        }else if (relTk == Token.LEQ){
            icGen.combine(Opcode.blt, y, null, bb);
        }else if (relTk == Token.GT){
            icGen.combine(Opcode.ble, y, null, bb);
        }else if (relTk == Token.GEQ){
            icGen.combine(Opcode.ble, y, null, bb);
        }
    }

    private boolean isRelOp(Token tk){
        return tk == Token.EQ || tk == Token.NEQ || tk == Token.LE
                || tk == Token.LEQ || tk == Token.GT || tk == Token.GEQ;
    }

    private Result designator(BasicBlock currbb, CFG cfg) throws NonDeclaredException,NotExpectedException, IOException{
        if(currToken!=Token.IDENT){
            throw new NotExpectedException("IDENT is expected");
        }
        int baseaddr = sc.getIdentId();
        if(!cfg.isDeclared(baseaddr)){
            throw new NonDeclaredException(sc.getIdent()+"is not declared");
        }
        next();
        Result x = null, y = null;
        if(currToken != Token.LEFT_BRACKET){
            x = new Result(Result.Type.variable, baseaddr);
        }else{
            Result width = new Result(Result.Type.constant,4);
            Result base = new Result(Result.Type.constant,baseaddr);
            while(currToken == Token.LEFT_BRACKET){
                next();
                y = expression(currbb);
                if(currToken != Token.RIGHT_BRACKET){
                    throw new NotExpectedException("] is expected");
                }
                y = icGen.combine(Opcode.mul, width, y,currbb);
                base = icGen.combine(Opcode.add, base, y, currbb);
                next();
            }
            x = new Result(Result.Type.constant,FP);
            x = icGen.combine(Opcode.adda,x,base,currbb);
            x.isArray = true;
        }
        return x;
    }
    private Result expression(BasicBlock bb)throws NonDeclaredException, NotExpectedException, IOException{
        Result x = term(bb);
        while(currToken==Token.PLUS||currToken==Token.MINUS){
            Opcode op = null;
            if(currToken==Token.PLUS){
                op = Opcode.add;
            }else{
                op = Opcode.sub;
            }
            next();
            Result y = term(bb);
            x = icGen.combine(op,x,y,bb);
            next();
        }
        return x;
    }


    private Result term(BasicBlock bb) throws NonDeclaredException,  NotExpectedException, IOException{
        Result x = factor(bb);
        while(currToken==Token.MUL||currToken==Token.DIV){
            Opcode op = null;
            if(currToken == Token.MUL){
                op = Opcode.mul;
            }else{
                op = Opcode.div;
            }
            next();
            Result y = factor(bb);
            x = icGen.combine(op,x,y,bb);
        }
        return  x;
    }

    private Result factor(BasicBlock bb) throws NonDeclaredException, NotExpectedException, IOException{
        Result x = null;
        if(currToken==Token.NUMBER){
            x = new Result(Result.Type.constant,sc.getVal());
            next();
            return x;
        }
        if(currToken == Token.LEFT_PAR){
            next();
            x = expression(bb);
            next();
            if(currToken!=Token.RIGHT_PAR){
                throw new NotExpectedException(") is expected");
            }
            return  x;
        }
        CFG cfg = icGen.getCfg(bb.getCfgid());
        x = designator(bb,cfg);
        if(x == null){
            x = funcCall(cfg);
        }
        return  x;
    }

    private Result funcCall(CFG function) throws NonDeclaredException, NotExpectedException, IOException{
        Result x = null, y = null;
        BasicBlock firstbb = function.getBlock(function.getFirstBB());
        if (function.getParameters().size() != 0) {
            int temp = function.createBB();
            BasicBlock tempBB = function.getBlock(temp);
            ArrayList<Result> params = function.getParameters();
            for (int i = 0; i < params.size(); i++) {
                y = expression(tempBB);
                icGen.combine(Opcode.move, y, params.get(i), tempBB);
            }
            Instruction tempLast = icGen.getInstruction(tempBB.getLastInstr());
            tempLast.next = firstbb.getFirstInstr();
            x = new Result(Result.Type.instruction, tempBB.getFirstInstr());
        }else{
            x = new Result(Result.Type.instruction,firstbb.getFirstInstr());
        }
        return x;
    }

    private Result assignment(BasicBlock bb, CFG cfg) throws NonDeclaredException, NotExpectedException, IOException{
        Result x = null, y = null;
        x = designator(bb, cfg);
        next();
        if(currToken!=Token.DESIGN){
            throw new NotExpectedException("<- is expected");
        }
        y = expression(bb);
        if(x.isArray){
            x = icGen.combine(Opcode.store, y, x,bb);
        }else{
            x = icGen.combine(Opcode.move, y, x, bb);
        }
        return x;
    }

    private Result ifStatement(BasicBlock currBB, CFG currCFG)throws NonDeclaredException, NotExpectedException, IOException{
        Result x = null;
        relation(currBB);
        int joinid = currCFG.createBB();
        BasicBlock joinbb = currCFG.getBlock(joinid);
        joinbb.setSymbolTable(currBB.getSymbolTable());
        next();
        if (currToken!=Token.THEN){
            throw new NotExpectedException("THEN is expected");
        }
        next();
        Instruction last = icGen.getInstruction(currBB.getLastInstr());

        int newthenid = currCFG.createBB();
        BasicBlock newthenbb = currCFG.getBlock(newthenid);
        currBB.link(newthenbb);
        statSequence(newthenbb,currCFG);
        newthenbb.link(joinbb);
        joinbb.insertPhi(icGen,newthenbb.getSymbolTable());
        last.next = joinbb.getFirstInstr();
        next();
        if (currToken == Token.ELSE){
            int newelseid = currCFG.createBB();
            BasicBlock newelsebb = currCFG.getBlock(newelseid);
            currBB.link(newelsebb);
            next();
            statSequence(newelsebb,currCFG);
            newelsebb.link(joinbb);
            joinbb.insertPhi(icGen,newelsebb.getSymbolTable());
            x = new Result(Result.Type.instruction, newelsebb.getFirstInstr());
        }else{
            currBB.link(joinbb);
            x = new Result(Result.Type.instruction,joinbb.getFirstInstr());
        }
        next();
        if(currToken!=Token.FI){
            throw new NotExpectedException("FI is expected");
        }
        return x;
    }

    private Result whileStatement(BasicBlock bb, CFG cfg)throws NonDeclaredException, NotExpectedException, IOException{
        relation(bb);
        int newid = cfg.createBB();
        BasicBlock newbb = cfg.getBlock(newid);
        bb.link(newbb);
        next();
        if(currToken != Token.DO){
            throw  new NotExpectedException("DO is expected");
        }
        statSequence(newbb, cfg);
        next();
        if(currToken!=Token.OD){
            throw  new NotExpectedException("OD is expected");
        }
        newbb.link(bb);
        bb.insertPhi(icGen,newbb.getSymbolTable());
        int branchid = cfg.createBB();
        BasicBlock branchbb = cfg.getBlock(branchid);
        return new Result(Result.Type.block, branchid);

    }
    private void next() throws IOException{
        currToken = sc.getNextToken();
    }


}
