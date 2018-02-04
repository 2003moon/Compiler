package frontend;

import java.io.*;

public class reader {
    private Character currChar;
    private String currLine;
    private FileInputStream fis;
    private BufferedReader br;
    int currCharpos;
    int linelength;

    public reader(){
        currChar = null;
        currLine = null;
        currCharpos = 0;
        linelength = 0;
    }

    public void openfile(String path) throws FileNotFoundException, IOException{
        File file = new File(path);
        fis = new FileInputStream(file);
        br = new BufferedReader(new InputStreamReader(fis));
        currLine = br.readLine();
        linelength = currLine.length();
    }

    public void closefile() throws IOException{
        br.close();
        linelength = 0;
        currCharpos = -1;
        currChar = null;
        currLine = null;
    }

    public Character getNextChar(){

        if(currLine == null){
            return '~';//indicate the end of a file
        }

        if(currCharpos>=linelength){
       //     getNextLine();
            currCharpos = 0;
            return '#'; //indicate the end of a line
        }

        Character res = currLine.charAt(currCharpos);
        currCharpos++;
        return res;
    }

    public void getNextLine() throws IOException{
        currLine = br.readLine();
  //      currLine.trim();
        currCharpos = 0;
        if(currLine != null){
            linelength = currLine.length();
        }
    }

}
