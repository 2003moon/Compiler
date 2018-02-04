package frontend;

import util.Token;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
public class scanner {
    private Character currchar;
    private int val;
    private int id;
    private Map<String, Integer> identToId;
    private Map<Integer, String> idToIdent;
    private ArrayList<String> ids;
    private reader rd;
    public scanner(String path) throws IOException{
        val = -1;
        id = -1;
        identToId = new HashMap<String,Integer>();
        idToIdent = new HashMap<Integer, String>();
        identToId.put("InputNum",1);
        identToId.put("OutputNewLine",2);
        identToId.put("OutputNum",3);
        idToIdent.put(1,"InputNum");
        idToIdent.put(2,"OutputNewLine");
        idToIdent.put(3,"OutputNum");
        rd = new reader();
        rd.openfile(path);
        currchar = rd.getNextChar();
    }

    public Token getNextToken() throws IOException{
     /*   if(currchar == '$'){
            currchar = rd.getNextChar();
            return null;
        }*/
        Token tk = skipWhiteSpaceAndComment();
        if(tk!=null){
            return tk;
        }
        if(currchar == '*'){
            currchar = rd.getNextChar();
            return Token.MUL;
        }
        if(currchar == '+'){
            currchar = rd.getNextChar();
            return Token.PLUS;
        }
        if(currchar == '-'){
            currchar = rd.getNextChar();
            return Token.MINUS;
        }
        if(currchar == '<'){
            currchar = rd.getNextChar();
            if(currchar == '-'){
                currchar = rd.getNextChar();
                return Token.DESIGN;
            }
            if(currchar == '='){
                currchar = rd.getNextChar();
                return Token.LEQ;
            }
            return Token.LE;
        }
        if(currchar == '>'){
            currchar = rd.getNextChar();
            if(currchar == '='){
                currchar = rd.getNextChar();
                return Token.GEQ;
            }
            return Token.GT;

        }
        if(currchar == '='){
            currchar = rd.getNextChar();
            if(currchar == '='){
                currchar = rd.getNextChar();
                return Token.EQ;
            }
            return Token.ERROR;
        }
        if(currchar == '!'){
            currchar = rd.getNextChar();
            if(currchar == '='){
                currchar = rd.getNextChar();
                return Token.NEQ;
            }
            return Token.ERROR;
        }
        if(currchar == '('){
            currchar = rd.getNextChar();
            return Token.LEFT_PAR;
        }
        if(currchar == ')'){
            currchar = rd.getNextChar();
            return Token.RIGHT_PAR;
        }
        if(currchar == '['){
            currchar = rd.getNextChar();
            return Token.LEFT_BRACKET;
        }
        if(currchar == ']'){
            currchar = rd.getNextChar();
            return Token.RIGHT_BRACKET;
        }
        if(currchar == '{'){
            currchar = rd.getNextChar();
            return Token.LEFT_BRACE;
        }
        if(currchar == '}'){
            currchar = rd.getNextChar();
            return Token.RIGHT_BRACE;
        }
        if(currchar == '.'){
            currchar = rd.getNextChar();
            return  Token.PERIOD;
        }
        if(currchar == ','){
            currchar = rd.getNextChar();
            return Token.COMMA;
        }
        if(currchar == ';'){
            currchar = rd.getNextChar();
            return Token.SEMICOMA;
        }
        if(currchar == ':'){
            currchar = rd.getNextChar();
            return  Token.COLON;
        }
 /*       if('0'<=currchar && currchar <= '9'){
            val = currchar - '0';
            currchar = rd.getNextChar();
            return Token.NUMBER;
        }*/

        if(currchar == '~'){
            return Token.EOF;
        }


        String s = getWords();
        if(s.equals("od")){
            int test = 0;
        }
        tk = getNumberToken(s);
        if(tk!=null){
            return tk;
        }
        tk = getKeyWordToken(s);
        if(tk != null){
            return  tk;
        }

        return getIdentToken(s);

    }
    private Token getIdentToken(String s){
        if(identToId.containsKey(s)) {
            id = identToId.get(s);
        }else{
            id = identToId.size();
            identToId.put(s,id);
            idToIdent.put(id,s);
        }
        return Token.IDENT;
    }
    private Token getNumberToken(String s){
        char c = s.charAt(0);
        if(!Character.isDigit(c)){
            return null;
        }
        int sum = c-'0';
        for(int i =1 ;i<s.length();i++){
            c = s.charAt(i);
            if(!Character.isDigit(c)){
                return Token.ERROR;
            }
            sum += sum*10+c-'0';
        }
        val = sum;
        return Token.NUMBER;
    }
    public String getIdent(){
        return  idToIdent.get(id);
    }

    public int getIdentId(){
        return id;
    }

    public int getVal(){
        return  val;
    }
    private Token getKeyWordToken(String s){
        return Token.getKeyWord(s);
    }

    private String getWords() throws IOException{
        StringBuffer sb = new StringBuffer();
        while(Character.isLetterOrDigit(currchar)){
            sb.append(currchar);
            currchar = rd.getNextChar();
        }
        return sb.toString();
    }

    private Token skipWhiteSpaceAndComment() throws IOException{
        while(currchar == '\t'|| currchar == '\r' || currchar == '\n'||currchar == ' '||currchar == '#'||currchar == '/'){
            if(currchar == '#'){
                rd.getNextLine();
           //     currchar = rd.getNextChar();
            }
            if(currchar == '/'){
                currchar = rd.getNextChar();
                if(currchar == '/'){
                    rd.getNextLine();
               //     currchar = rd.getNextChar();
                }else{
                //    currchar = rd.getNextChar();
                    return Token.DIV;
                }
            }
            currchar = rd.getNextChar();
        }
        return null;
    }

}
