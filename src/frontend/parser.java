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

    public parser(String path) throws IOException{
        sc = new scanner(path);
        icGen = new IcGenerator();
        next();
    }

    public void parse() throws NonDeclaredException, NotExpectedException, IOException{
        computation();
    }

    private void computation() throws NonDeclaredException, NotExpectedException, IOException{
        if(currToken!=Token.MAIN){
            throw new NotExpectedException("MAIN is expected");
        }
        next();
        varDecl();
        funcDecl();
        if(currToken!= Token.LEFT_BRACE){
            throw new NotExpectedException("{ is expected");
        }
        next();
        statSequence();
        if (currToken!=Token.RIGHT_BRACE){
            throw new NotExpectedException("} is expected");
        }

    }

    private void varDecl() throws NotExpectedException, IOException{
        if(!typeDecl()){
            return;
        }
        if(currToken!=Token.IDENT){
            throw new NotExpectedException("ident is expected");
        }
        CFG currcfg = icGen.getCfg(icGen.getCurrcfg());
        while(currToken == Token.IDENT){
            int address = sc.getIdentId();
            boolean global = icGen.isGlobal();
            Result x = new Result(Result.Type.variable, address, global);
            x.setVersion(0);
            currcfg.addVarInfo(x);
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
            Result x = new Result(Result.Type.variable, sc.getIdentId(),false);
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
        icGen.setCurrcfg(funcid);
        varDecl();
        if (currToken != Token.LEFT_BRACE){
            throw new NotExpectedException("{ is expected");
        }
        next();
        statSequence();
        if (currToken!= Token.RIGHT_BRACE){
            throw new NotExpectedException("{ is expected");
        }
        next();
    }

    private void statSequence()throws NonDeclaredException, NotExpectedException, IOException{
        if(!isStatement()){
            throw new NotExpectedException("statement is expected");
        }
        while(isStatement()){
            statement();
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

    private Result statement() throws NonDeclaredException, NotExpectedException, IOException{
        Result x = null;
        if(currToken == Token.LET){
            next();
            x = assignment();
        }else if (currToken == Token.CALL){
            next();
            x = funcCall();
        }else if (currToken == Token.IF){

        }else if (currToken == Token.WHILE){

        }else{
            next();
            x = expression();
        }
        return  x;
    }


    private Result designator() throws NonDeclaredException,NotExpectedException, IOException{
        if(currToken!=Token.IDENT){
            return null;
        }
        int baseaddr = sc.getIdentId();
        CFG cfg = icGen.getCfg(icGen.getCurrcfg());
        Result x = null, y = null;
        if(!cfg.getVersionMap().containsKey(baseaddr)){
            throw new NonDeclaredException(sc.getIdent()+" is not declared");
        }


        if(currToken != Token.LEFT_BRACKET){
            x = new Result(Result.Type.variable, baseaddr, icGen.isGlobal());
            x.setVersion(cfg.getVersion(baseaddr));
            return x;
        }

        Result width = new Result(Result.Type.constant,4);
        while(currToken == Token.LEFT_BRACKET){
            next();
            y = expression();
            if(currToken != Token.RIGHT_BRACKET){
                throw new NotExpectedException("] is expected");
            }
            y = icGen.combine(Opcode.mul, width, y);
            if(x!=null){
                x = icGen.combine(Opcode.add,x,y);
            }else{
                x = y;
            }
            next();
        }
        Result base = new Result(Result.Type.constant,baseaddr);
        x = icGen.combine(Opcode.adda,x,base);
        return x;
    }
    private Result expression()throws NonDeclaredException, NotExpectedException, IOException{
        Result x = term();
        while(currToken==Token.PLUS||currToken==Token.MINUS){
            Opcode op = null;
            if(currToken==Token.PLUS){
                op = Opcode.add;
            }else{
                op = Opcode.sub;
            }
            next();
            Result y = term();
            x = icGen.combine(op,x,y);
            next();
        }
        return x;
    }


    private Result term() throws NonDeclaredException,  NotExpectedException, IOException{
        Result x = factor();
        while(currToken==Token.MUL||currToken==Token.DIV){
            Opcode op = null;
            if(currToken == Token.MUL){
                op = Opcode.mul;
            }else{
                op = Opcode.div;
            }
            next();
            Result y = factor();
            x = icGen.combine(op,x,y);
        }
        return  x;
    }

    private Result factor() throws NonDeclaredException, NotExpectedException, IOException{
        Result x = null;
        if(currToken==Token.NUMBER){
            x = new Result(Result.Type.constant,sc.getVal());
            next();
            return x;
        }
        if(currToken == Token.LEFT_PAR){
            next();
            x = expression();
            next();
            if(currToken!=Token.RIGHT_PAR){
                throw new NotExpectedException(") is expected");
            }
            return  x;
        }
        x = designator();
        if(x == null){
            x = funcCall();
        }
        return  x;
    }

    private Result funcCall() throws NonDeclaredException, NotExpectedException, IOException{
        Result x = null, y = null;
        int funcid = sc.getIdentId();
        if(funcid == 1){
            x = icGen.combine(Opcode.read, x, y);
        }else if (funcid == 2){
            next();
            x = new Result(Result.Type.variable,sc.getIdentId(),icGen.isGlobal());
            x = icGen.combine(Opcode.write, x, y);
        }else if (funcid == 3){
            icGen.combine(Opcode.writeNL,x, y);
        }else{
            x = new Result(Result.Type.branch, funcid);
            icGen.combine(Opcode.bra, x, y);
            next();
            if (currToken == Token.LEFT_PAR){
                next();
                y = expression();
                int index = 0;
                while(y!= null){
                    icGen.passParam(funcid, index,y);
                    index++;
                    next();
                    if (currToken!=Token.COMMA){
                        break;
                    }
                    next();
                    y = expression();
                }
            }
        }
        return x;
    }

    private Result assignment() throws NonDeclaredException, NotExpectedException, IOException{
        Result x = null, y = null;
        x = designator();
        if(x == null){
            throw new NotExpectedException("designator is expected");
        }
        if(currToken!=Token.DESIGN){
            throw new NotExpectedException("<- is expected");
        }
        y = expression();
        x = icGen.combine(Opcode.move, y, x);
        return x;
    }

    private void next() throws IOException{
        currToken = sc.getNextToken();
    }


}
