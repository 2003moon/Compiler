package frontend;
//TODO: the symbol table should be created when declaration. Env:P111 of dragon book
//TODO: error checking : duplicate declared in a single scope
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import exceptions.DuplicateDeclaredException;
import exceptions.NonDeclaredException;
import exceptions.NotDefinedException;
import lombok.Getter;
import util.*;
import exceptions.NotExpectedException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
@Getter
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

    public void parse() throws DuplicateDeclaredException, NonDeclaredException, NotDefinedException, NotExpectedException, IOException{
        computation(icGen.getCfg(0));
    }

    private void computation(CFG cfg) throws DuplicateDeclaredException, NonDeclaredException, NotDefinedException, NotExpectedException, IOException{
        if(currToken!=Token.MAIN){
            throw new NotExpectedException("MAIN is expected");
        }
        next();
        while(typeDecl()){
            varDecl(cfg);
        }

      //  next();
        while (isFuncDecl(currToken)){
            next();
            funcDecl();
        }

     //   next();
        if(currToken!= Token.LEFT_BRACE){
            throw new NotExpectedException("{ is expected");
        }
        next();
        BasicBlock bb = statSequence(cfg.getBlock(cfg.getFirstBB()),cfg);
        if (currToken!=Token.RIGHT_BRACE){
            throw new NotExpectedException("} is expected");
        }
        icGen.combine(Opcode.end,null,null,bb);

    }

    private void varDecl(CFG currcfg) throws NotExpectedException, DuplicateDeclaredException, IOException{
        if(currToken!=Token.IDENT){
            throw new NotExpectedException("ident is expected");
        }
        while(currToken == Token.IDENT){
            int address = sc.getIdentId();
            if(currcfg.isDeclared(address)){
                throw new DuplicateDeclaredException(sc.getIdent(address)+" has been declared");
            }
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
        next();
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
    private boolean isFuncDecl(Token tk){
        return tk == Token.FUNCTION || tk == Token.PROCEDURE;
    }
    private void funcDecl()throws DuplicateDeclaredException, NonDeclaredException, NotDefinedException, NotExpectedException, IOException{
        if(currToken!=Token.IDENT){
            throw new NotExpectedException("ident is expected");
        }
        int funcid = sc.getIdentId();
        String name = sc.getIdent(funcid);
        icGen.createCfg(funcid,name);
        next();
        formalParam(funcid);
        next();
        if(currToken!=Token.SEMICOMA){
            throw new NotExpectedException("; is expected");
        }
        next();
        funcBody(funcid);
        next();
        if(currToken != Token.SEMICOMA){
            throw new NotExpectedException("; is expected");
        }
        next();
    }

    private void formalParam(int funcid)throws NotExpectedException, IOException{
        if(currToken!=Token.LEFT_PAR){
            return;
        }

        next();
        CFG cfg = icGen.getCfg(funcid);
        while(currToken==Token.IDENT){
           // Result x = new Result(Result.Type.variable, sc.getIdentId());
            cfg.addParam(sc.getIdentId(),null);
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
    private void funcBody(int funcid)throws DuplicateDeclaredException, NonDeclaredException, NotDefinedException, NotExpectedException, IOException{
        CFG cfg = icGen.getCfg(funcid);
        while(typeDecl()){
            varDecl(cfg);
        }
  //      next();
        if (currToken != Token.LEFT_BRACE){
            throw new NotExpectedException("{ is expected");
        }
        next();
        BasicBlock bb = statSequence(cfg.getBlock(cfg.getFirstBB()), cfg);
        if (currToken!= Token.RIGHT_BRACE){
            throw new NotExpectedException("{ is expected");
        }
        icGen.combine(Opcode.end, null, null, bb);
    }

    private BasicBlock statSequence(BasicBlock bb, CFG cfg)throws NonDeclaredException, NotDefinedException, NotExpectedException, IOException{
        if(!isStatement()){
            throw new NotExpectedException("statement is expected");
        }
        while(isStatement()){
            bb = statement(bb,cfg);
      //      next();
            if(currToken !=Token.SEMICOMA){
                break;
            }
            next();
        }
        return bb;
    }

    private boolean isStatement(){
        return currToken == Token.LET || currToken == Token.CALL || currToken == Token.IF
                || currToken == Token.WHILE || currToken == Token.RETURN;
    }

    private BasicBlock statement(BasicBlock bb, CFG cfg) throws NonDeclaredException, NotExpectedException, NotDefinedException, IOException{
        Result x = null;
        if(currToken == Token.LET){
            next();
            x = assignment(bb, cfg);
        }else if (currToken == Token.CALL){
            next();
            x = funcCall(bb);
        }else if (currToken == Token.IF){
            next();
           x = ifStatement(bb, cfg);
           bb = cfg.getBlock(x.getBb_id());
           next();
        }else if (currToken == Token.WHILE){
            if(bb.getFirstInstr()!=-1){
                int newid = cfg.createBB();
                BasicBlock newbb = cfg.getBlock(newid);
                bb.link(newbb);
                newbb.setSymbolTable(bb.getSymbolTable());
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
            icGen.combine(Opcode.returnTo,x, null,bb);
        }
        return bb;
    }

    private void relation(BasicBlock bb) throws NonDeclaredException, NotExpectedException, NotDefinedException,IOException{
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

    private Result designator(BasicBlock currbb, CFG cfg) throws NonDeclaredException,NotDefinedException, NotExpectedException, IOException{
        if(currToken!=Token.IDENT){
            return null;
        }
        int baseaddr = sc.getIdentId();
        if(!cfg.isDeclared(baseaddr)&&!icGen.getMaincfg().isDeclared(baseaddr)&&cfg.isParameter(baseaddr)){
            throw new NonDeclaredException(sc.getIdent(baseaddr)+" is not declared");
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
                if(x == null){
                    x = y;
                }else{
                    x = icGen.combine(Opcode.add, x, y, currbb);
                }
              //  base = icGen.combine(Opcode.add, base, y, currbb);
                next();
            }
            Result fp = new Result(Result.Type.constant,FP);
            y = icGen.combine(Opcode.add, fp, base, currbb);
           // x = new Result(Result.Type.constant,FP);
            x = icGen.combine(Opcode.adda,x,y,currbb);
            x.isArray = true;
        }
        return x;
    }
    private Result expression(BasicBlock bb)throws NonDeclaredException, NotDefinedException,NotExpectedException, IOException{//call next inside
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
        //    next();
        }
        return x;
    }


    private Result term(BasicBlock bb) throws NonDeclaredException,  NotDefinedException, NotExpectedException, IOException{//next inside
        Result x = factor(bb);
      //  next();
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

    private Result factor(BasicBlock bb) throws NonDeclaredException, NotDefinedException, NotExpectedException, IOException{//need to next outside
        Result x = null;
        if(currToken==Token.NUMBER){
            x = new Result(Result.Type.constant,sc.getVal());
            next();
        }else if(currToken == Token.LEFT_PAR){
            next();
            x = expression(bb);
        //    next();
            if(currToken!=Token.RIGHT_PAR){
                throw new NotExpectedException(") is expected");
            }
            next();
        } else if(currToken == Token.IDENT){
            CFG cfg = icGen.getCfg(bb.getCfgid());
            x = designator(bb,cfg);
            return x;
        }else if(currToken == Token.CALL){
            next();
            x = funcCall(bb);
        //    next();
        }
        return  x;
    }

    private Result funcCall(BasicBlock bb) throws NonDeclaredException, NotDefinedException, NotExpectedException, IOException{
        Result x = null, y = null;
        if(currToken != Token.IDENT){
            throw new NotExpectedException("function name is expected");
        }
        int funcid = sc.getIdentId();
        if(funcid == 1){
            next();
            if (currToken!=Token.LEFT_PAR){
                throw new NotExpectedException("( is expected");
            }
            next();
            if(currToken!=Token.RIGHT_PAR){
                throw new NotExpectedException(")is expected");
            }

            next();
            x = icGen.combine(Opcode.read, null, null, bb);

        }else if (funcid == 2){
            next();
            if (currToken!=Token.LEFT_PAR){
                throw new NotExpectedException("( is expected");
            }
            next();
            if(currToken!=Token.RIGHT_PAR){
                throw new NotExpectedException(")is expected");
            }
            next();
            icGen.combine(Opcode.writeNL,null, null, bb);

        }else if (funcid == 3){
            next();
            if (currToken!=Token.LEFT_PAR){
                throw new NotExpectedException("( is expected");
            }
            next();
            if(currToken ==Token.RIGHT_PAR){
               // throw new NotExpectedException("IDENT is expected");
                throw new NotExpectedException("A parameter is expected");
            }
            next();
            if(currToken!=Token.RIGHT_PAR){
                throw new NotExpectedException(")is expected");
            }
            next();
            x = bb.getSymbolTable().get(sc.getIdentId());
            //    x = new Result(Result.Type.variable,sc.getIdentId());
            x = icGen.combine(Opcode.write, x, null, bb);

        }else{
            CFG func = icGen.getCfg(funcid);
            Iterator<Integer> it = func.getParameters().keySet().iterator();
            next();
            if(currToken == Token.LEFT_PAR) {
                next();
                while (currToken != Token.RIGHT_PAR) {
                    if (!it.hasNext()) {
                        throw new NotExpectedException("parameter is not expected");
                    }
                    y = expression(bb);
                    //          function.addParam(it.next(), y);
                 //   next();
                    if (currToken != Token.COMMA) {
                        if(currToken != Token.RIGHT_PAR){
                            throw new NotExpectedException(") is not expected");
                        }
                        next();
                        break;
                    }
                    next();
                }
            }
            Result branch  = new Result(Result.Type.function, func.getId());
            x = icGen.combine(Opcode.call, branch, null, bb);

        }
   //     next();
        return x;


   //     icGen.combine(Opcode.bra, branch, null, rsmBlock);
        //TODO: make branch
    /*    Result branch = new Result(Result.Type.instruction,function.getBlock(function.getFirstBB()).getFirstInstr());
        Instruction last = icGen.getInstruction(rsmBlock.getLastInstr());
        function.addResumePoints(last);
        BasicBlock lastFuncBB = function.getBlock(function.getLastBB());
        Instruction lastFuncInstr = icGen.getInstruction(lastFuncBB.getLastInstr());
        lastFuncInstr.next = last.getId();
        BasicBlock firstFuncbb = function.getBlock(function.getFirstBB());
        firstFuncbb.setSymbolTable(rsmBlock.getSymbolTable());
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
        return x;*/
    }

    private Result assignment(BasicBlock bb, CFG cfg) throws NonDeclaredException,NotDefinedException, NotExpectedException, IOException{
        Result x = null, y = null;
        x = designator(bb, cfg);
   //     next();
        if(currToken!=Token.DESIGN){
            throw new NotExpectedException("<- is expected");
        }
        next();
        y = expression(bb);
        if(x.isArray){
            x = icGen.combine(Opcode.store, y, x,bb);
        }else{
            x = icGen.combine(Opcode.move, y, x, bb);
        }
        return x;
    }

    private Result ifStatement(BasicBlock currBB, CFG currCFG)throws NonDeclaredException, NotDefinedException, NotExpectedException, IOException{
        Result x = null;
        relation(currBB);
        int joinid = currCFG.createBB();
        BasicBlock joinbb = currCFG.getBlock(joinid);
        joinbb.setSymbolTable(currBB.getSymbolTable());
    //    next();
        if (currToken!=Token.THEN){
            throw new NotExpectedException("THEN is expected");
        }
        next();
        Instruction last = icGen.getInstruction(currBB.getLastInstr());

        int newthenid = currCFG.createBB();
        BasicBlock newthenbb = currCFG.getBlock(newthenid);
        currBB.link(newthenbb);
        newthenbb = statSequence(newthenbb,currCFG);
        newthenbb.link(joinbb);
        joinbb.insertPhi(icGen,newthenbb.getSymbolTable());
      //  last.next = joinbb.getFirstInstr();
        last.next = newthenbb.getFirstInstr();
        //next();
        if (currToken == Token.ELSE){
            int newelseid = currCFG.createBB();
            BasicBlock newelsebb = currCFG.getBlock(newelseid);
            currBB.link(newelsebb);
            next();
            newelsebb = statSequence(newelsebb,currCFG);
            last.updateOp(new Result(Result.Type.instruction, newelsebb.getFirstInstr()));
            newelsebb.link(joinbb);
            joinbb.insertPhi(icGen,newelsebb.getSymbolTable());
            Instruction newelselast = icGen.getInstruction(newelsebb.getLastInstr());
            newelselast.next = joinbb.getFirstInstr();
            icGen.combine(Opcode.bra,new Result(Result.Type.instruction, joinbb.getFirstInstr()), null, newthenbb);
        }else{
           // currBB.link(joinbb);
            Instruction newthenlast = icGen.getInstruction(newthenbb.getLastInstr());
            newthenlast.next = joinbb.getFirstInstr();
            last.updateOp(new Result(Result.Type.instruction, joinbb.getFirstInstr()));
        }
     //   next();
        if(currToken!=Token.FI){
            throw new NotExpectedException("FI is expected");
        }
        x = new Result(Result.Type.block, joinbb.getId());
        return x;
    }

    private Result whileStatement(BasicBlock bb, CFG cfg)throws NonDeclaredException, NotDefinedException,NotExpectedException, IOException{
        relation(bb);
        int newid = cfg.createBB();
        BasicBlock newbb = cfg.getBlock(newid);
        bb.link(newbb);
 //       next();
        if(currToken != Token.DO){
            throw  new NotExpectedException("DO is expected");
        }
        next();
        newbb = statSequence(newbb, cfg);
    //    next();
        if(currToken!=Token.OD){
            throw  new NotExpectedException("OD is expected");
        }
        newbb.link(bb);
        bb.insertPhi(icGen,newbb.getSymbolTable());
        int branchid = cfg.createBB();
        BasicBlock branchbb = cfg.getBlock(branchid);
        bb.link(branchbb);
        return new Result(Result.Type.block, branchid);
    }
    private void next() throws IOException{
        currToken = sc.getNextToken();
    }


}
