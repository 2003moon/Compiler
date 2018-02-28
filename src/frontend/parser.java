package frontend;
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
    private DominatorTreeComputer DtComp;
    private scanner sc;
    private int FP;

    public parser(String path) throws IOException{
        sc = new scanner(path);
        icGen = new IcGenerator();
        DtComp = new DominatorTreeComputer();
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

        while (isFuncDecl(currToken)){
            next();
            funcDecl();
        }

        if(currToken!= Token.LEFT_BRACE){
            throw new NotExpectedException("{ is expected");
        }
        next();

        //TODO: initiate the first DT  node(root)
        BasicBlock firstBB = cfg.getBlock(cfg.getFirstBB());
        initDtNode(firstBB);
        BasicBlock bb = statSequence(firstBB,null, null, cfg);
        if (currToken!=Token.RIGHT_BRACE){
            throw new NotExpectedException("} is expected");
        }
        icGen.combine(Opcode.end,null,null,bb, null, null);

    }

    private void initDtNode(BasicBlock bb){
        if(!DtComp.hasNode(bb.getId())){
            DtComp.addNode(bb);
        }
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
            currcfg.addVar(address,-1);
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
        BasicBlock bb = statSequence(cfg.getBlock(cfg.getFirstBB()), null, null, cfg);
        if (currToken!= Token.RIGHT_BRACE){
            throw new NotExpectedException("{ is expected");
        }
        icGen.combine(Opcode.end, null, null, bb, null, null);
    }

    private BasicBlock statSequence(BasicBlock bb, BasicBlock joinBB, Map<Integer, ArrayList<Integer>> useChain, CFG cfg)throws NonDeclaredException, NotDefinedException, NotExpectedException, IOException{
        if(!isStatement()){
            throw new NotExpectedException("statement is expected");
        }
        while(isStatement()){
            bb = statement(bb,joinBB, useChain, cfg);
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

    private BasicBlock statement(BasicBlock bb, BasicBlock joinBB, Map<Integer, ArrayList<Integer>> useChain, CFG cfg) throws NonDeclaredException, NotExpectedException, NotDefinedException, IOException{
        Result x = null;
        if(currToken == Token.LET){
            next();
            x = assignment(bb, joinBB, useChain, cfg);
        }else if (currToken == Token.CALL){
            next();
            x = funcCall(bb,joinBB, useChain);
        }else if (currToken == Token.IF){
            next();
           x = ifStatement(bb, joinBB, useChain, cfg);
           bb = cfg.getBlock(x.getBr_id());
           next();
        }else if (currToken == Token.WHILE){
            next();
            HashMap<Integer, ArrayList<Integer>> usageChain = new HashMap<Integer, ArrayList<Integer>>();
            x = whileStatement(bb,joinBB, usageChain, cfg);
            bb = cfg.getBlock(x.getBr_id());
            next();
        }else{
            next();
            x = expression(bb, joinBB, useChain);
            icGen.combine(Opcode.returnTo,x, null,bb, joinBB, useChain);
        }
        return bb;
    }

    private void relation(BasicBlock bb, BasicBlock joinBB, Map<Integer, ArrayList<Integer>> useChain) throws NonDeclaredException, NotExpectedException, NotDefinedException,IOException{
        Result x = expression(bb, joinBB, useChain);
        if(!isRelOp(currToken)){
            throw new NotExpectedException("relOp is expected");
        }
        Token relTk = currToken;
        Opcode op = Opcode.tokenToOp(currToken);
        next();
        Result y = expression(bb, joinBB, useChain);

        y = icGen.combine(Opcode.cmp,x,y,bb,joinBB, useChain);
        if(relTk == Token.EQ){
            bb.addBranch(icGen, Opcode.bne, y, null);
        }else if(relTk == Token.NEQ){
            bb.addBranch(icGen, Opcode.beq, y, null);
        }else if (relTk == Token.LE){
            bb.addBranch(icGen, Opcode.bge, y, null);
        }else if (relTk == Token.LEQ){
            bb.addBranch(icGen, Opcode.blt, y, null);
        }else if (relTk == Token.GT){
            bb.addBranch(icGen,Opcode.ble, y, null);
        }else if (relTk == Token.GEQ){
            bb.addBranch(icGen, Opcode.ble, y, null);
        }
    }

    private boolean isRelOp(Token tk){
        return tk == Token.EQ || tk == Token.NEQ || tk == Token.LE
                || tk == Token.LEQ || tk == Token.GT || tk == Token.GEQ;
    }

    private Result designator(BasicBlock currbb, BasicBlock joinBB, Map<Integer,ArrayList<Integer>> useChain, CFG cfg) throws NonDeclaredException,NotDefinedException, NotExpectedException, IOException{
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
            x.setVersion(cfg.getVersion(baseaddr));
        }else{
            Result width = new Result(Result.Type.constant,4);
            Result base = new Result(Result.Type.constant,baseaddr);
            while(currToken == Token.LEFT_BRACKET){
                next();
                y = expression(currbb, joinBB, useChain);
                if(currToken != Token.RIGHT_BRACKET){
                    throw new NotExpectedException("] is expected");
                }
                y = icGen.combine(Opcode.mul, width, y,currbb, joinBB, useChain);
                if(x == null){
                    x = y;
                }else{
                    x = icGen.combine(Opcode.add, x, y, currbb, joinBB, useChain);
                }
              //  base = icGen.combine(Opcode.add, base, y, currbb);
                next();
            }
            Result fp = new Result(Result.Type.constant,FP);
            y = icGen.combine(Opcode.add, fp, base, currbb, joinBB, useChain);
           // x = new Result(Result.Type.constant,FP);
            x = icGen.combine(Opcode.adda,x,y,currbb, joinBB, useChain);
            x.isArray = true;
        }
        return x;
    }
    private Result expression(BasicBlock bb, BasicBlock joinBB, Map<Integer, ArrayList<Integer>> useChain)throws NonDeclaredException, NotDefinedException,NotExpectedException, IOException{//call next inside
        Result x = term(bb, joinBB, useChain);
        while(currToken==Token.PLUS||currToken==Token.MINUS){
            Opcode op = null;
            if(currToken==Token.PLUS){
                op = Opcode.add;
            }else{
                op = Opcode.sub;
            }
            next();
            Result y = term(bb, joinBB, useChain);
            x = icGen.combine(op,x,y,bb, joinBB, useChain);
        //    next();
        }
        return x;
    }


    private Result term(BasicBlock bb, BasicBlock joinBB, Map<Integer, ArrayList<Integer>> useChain) throws NonDeclaredException,  NotDefinedException, NotExpectedException, IOException{//next inside
        Result x = factor(bb, joinBB, useChain);
    /*    if(x.getType() == Result.Type.variable){
            x = bb.getSymbol(x.getAddress());
        }*/
        while(currToken==Token.MUL||currToken==Token.DIV){
            Opcode op = null;
            if(currToken == Token.MUL){
                op = Opcode.mul;
            }else{
                op = Opcode.div;
            }
            next();
            Result y = factor(bb, joinBB, useChain);
            if(y.getType() == Result.Type.variable){
                y = bb.getSymbol(y.getAddress());
            }
            x = icGen.combine(op,x,y,bb, joinBB, useChain);
        }
        return  x;
    }

    private Result factor(BasicBlock bb, BasicBlock joinBB, Map<Integer, ArrayList<Integer>> useChain) throws NonDeclaredException, NotDefinedException, NotExpectedException, IOException{//need to next outside
        Result x = null;
        if(currToken==Token.NUMBER){
            x = new Result(Result.Type.constant,sc.getVal());
            next();
        }else if(currToken == Token.LEFT_PAR){
            next();
            x = expression(bb, joinBB, useChain);
        //    next();
            if(currToken!=Token.RIGHT_PAR){
                throw new NotExpectedException(") is expected");
            }
            next();
        } else if(currToken == Token.IDENT){
            CFG cfg = icGen.getCfg(bb.getCfgid());
            x = designator(bb,joinBB, useChain, cfg);
            return x;
        }else if(currToken == Token.CALL){
            next();
            x = funcCall(bb, joinBB, useChain);
        //    next();
        }
        return  x;
    }

    private Result funcCall(BasicBlock bb, BasicBlock joinBB, Map<Integer,ArrayList<Integer>> useChain) throws NonDeclaredException, NotDefinedException, NotExpectedException, IOException{
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
            x = icGen.combine(Opcode.read, null, null, bb, joinBB, useChain);

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
            icGen.combine(Opcode.writeNL,null, null, bb, joinBB, useChain);

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
            int addr = sc.getIdentId();
            CFG cfg = icGen.getCfg(bb.getCfgid());
            x = new Result(Result.Type.variable,addr);
            if(!cfg.isDefined(addr)){
                throw new NotDefinedException("variable at "+addr+" is not defined");
            }
            x.setVersion(cfg.getVersion(addr));
            x = icGen.combine(Opcode.write, x, null, bb, joinBB, useChain);

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
                    y = expression(bb, joinBB, useChain);
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
            x = icGen.combine(Opcode.call, branch, null, bb, joinBB, useChain);

        }
   //     next();
        return x;


   //     icGen.combine(Opcode.bra, branch, null, rsmBlock);
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

    private Result assignment(BasicBlock bb, BasicBlock joinBB, Map<Integer, ArrayList<Integer>> useChain, CFG cfg) throws NonDeclaredException,NotDefinedException, NotExpectedException, IOException{
        Result x = null, y = null;
        x = designator(bb, joinBB, useChain, cfg);
   //     next();
        if(currToken!=Token.DESIGN){
            throw new NotExpectedException("<- is expected");
        }
        next();
        y = expression(bb, joinBB, useChain);
        if(x.isArray){
            x = icGen.combine(Opcode.store, y, x,bb, joinBB, useChain);
        }else{
            x = icGen.combine(Opcode.move, y, x, bb, joinBB, useChain);
        }
        return x;
    }

    private Result ifStatement(BasicBlock currBB, BasicBlock joinNode, Map<Integer, ArrayList<Integer>> useChain, CFG currCFG)throws NonDeclaredException, NotDefinedException, NotExpectedException, IOException{
        //TODO: empty block will cause exception.
        Result x = null;
        relation(currBB, joinNode, useChain);
        int joinid = currCFG.createBB();
        BasicBlock joinbb = currCFG.getBlock(joinid);
        initDtNode(joinbb);
        currBB.addChild(joinid);
        joinbb.immDom = currBB.getId();

        //TODO: make joinbb be the child of currbb
        joinbb.setBrType(currBB.getBrType());
        if (currToken!=Token.THEN){
            throw new NotExpectedException("THEN is expected");
        }
        next();
        Instruction last = icGen.getInstruction(currBB.getLastInstr());

        int newthenid = currCFG.createBB();
        BasicBlock newthenbb = currCFG.getBlock(newthenid);
        initDtNode(newthenbb);
        newthenbb.setBrType(BasicBlock.BrType.then);
        currBB.link(newthenbb);
        currBB.addChild(newthenid);
        newthenbb.immDom = currBB.getId();
        //TODO: make thenbb be the child of currbb
        newthenbb = statSequence(newthenbb,joinbb, useChain, currCFG);
        newthenbb.link(joinbb);
        last.next = newthenbb.getFirstInstr();

        icGen.resetVersion(currCFG, joinbb);
        //next();
        if (currToken == Token.ELSE){
            int newelseid = currCFG.createBB();
            BasicBlock newelsebb = currCFG.getBlock(newelseid);
            initDtNode(newelsebb);
            currBB.link(newelsebb);
            currBB.addChild(newelseid);
            newelsebb.immDom = currBB.getId();
            last.updatePhi(1, new Result(Result.Type.branch, newelseid));
            next();
            //TODO: make elsebb be the child of currbb
            newelsebb = statSequence(newelsebb,joinbb, useChain, currCFG);
            newelsebb.link(joinbb);
            Instruction newelselast = icGen.getInstruction(newelsebb.getLastInstr());
            newelselast.next = joinbb.getFirstInstr();
            newthenbb.addBranch(icGen,Opcode.bra, new Result(Result.Type.branch,joinid),null);
        }else{
           // currBB.link(joinbb);
            Instruction newthenlast = icGen.getInstruction(newthenbb.getLastInstr());
            newthenlast.next = joinbb.getFirstInstr();
            last.updatePhi(1, new Result(Result.Type.branch, joinid));
        }
     //   next();
        if(currToken!=Token.FI){
            throw new NotExpectedException("FI is expected");
        }

        joinbb.commitPhi(icGen, joinNode);
        x = new Result(Result.Type.branch, joinbb.getId());
        return x;
    }

    private Result whileStatement(BasicBlock bb, BasicBlock joinBB, Map<Integer, ArrayList<Integer>> usageChain,  CFG cfg)throws NonDeclaredException, NotDefinedException,NotExpectedException, IOException{
        BasicBlock whileBB = bb;
        if(bb.getFirstInstr()!=-1){
           int whileid = cfg.createBB();
           whileBB = cfg.getBlock(whileid);
           initDtNode(whileBB);
            bb.link(whileBB);
            bb.addChild(whileid);
            whileBB.immDom = bb.getId();

            //TODO: make while bb be child of bb(DT tree)
        }
      //  HashMap<Integer, ArrayList<Integer>> usageChain = new HashMap<Integer, ArrayList<Integer>>();

        relation(whileBB, whileBB, usageChain);

        if(currToken != Token.DO){
            throw  new NotExpectedException("DO is expected");
        }
        next();

        int loopid = cfg.createBB();
        BasicBlock loopBB = cfg.getBlock(loopid);
        initDtNode(loopBB);
        whileBB.link(loopBB);
        whileBB.addChild(loopid);
        loopBB.immDom = whileBB.getId();
        //TODO:make loopbb be the child of whilebb.
        loopBB = statSequence(loopBB, whileBB, usageChain, cfg);
        loopBB.addBranch(icGen, Opcode.bra, new Result(Result.Type.branch,whileBB.getId()), null);
        loopBB.link(whileBB);
    //    next();
        if(currToken!=Token.OD){
            throw  new NotExpectedException("OD is expected");
        }
        whileBB.commitPhi(icGen, joinBB);
        icGen.updateUsage(cfg.getId(), usageChain);
        int branchid = cfg.createBB();
        //TODO: make breanch bb be the child of while bb
        BasicBlock branchbb = cfg.getBlock(branchid);
        initDtNode(branchbb);
        Instruction whileLast = icGen.getInstruction(whileBB.getLastInstr());
        whileLast.updatePhi(1, new Result(Result.Type.branch, branchid));
        whileBB.link(branchbb);
        whileBB.addChild(branchid);
        branchbb.immDom = whileBB.getId();

        return new Result(Result.Type.branch, branchid);
    }
    private void next() throws IOException{
        currToken = sc.getNextToken();
    }


}
