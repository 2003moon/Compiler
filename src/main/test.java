package main;
import frontend.*;
import util.Token;

import java.io.IOException;

public class test {
    public static void main(String[] args){
        try{
            scanner sc = new scanner("test005.txt");
            Token tk = sc.getNextToken();
            while(tk != Token.EOF){
                System.out.print(tk);
                if(tk == Token.NUMBER){
                    System.out.print(" "+sc.getVal());
                }
                if(tk == Token.IDENT){
                    System.out.print(" "+sc.getIdent());
                }
                System.out.print('\n');
                tk = sc.getNextToken();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}


