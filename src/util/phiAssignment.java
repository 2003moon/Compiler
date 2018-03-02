package util;

import lombok.Getter;

@Getter
public class phiAssignment {

    public int backupV;

    private int address;
    private int[] versions;
    private Instruction instr;

    public phiAssignment(int address){
        this.address = address;
        versions = new int[2];
        instr = null;
    }

    public void updateIth(int i,int newver){
        versions[i] = newver;
        if(instr!=null){
            Result res = new Result(Result.Type.variable, address);
            res.setVersion(newver);
            instr.updatePhi(i, res);
        }

    }

    public void constructInstr(IcGenerator icGen, BasicBlock currBB){
        Result r1 = new Result(Result.Type.variable, address);
        Result r2 = new Result(Result.Type.variable, address);
        r1.setVersion(versions[0]); r2.setVersion(versions[1]);
        instr = new Instruction(r1,r2,Opcode.phi);
        instr.setId(icGen.getInstrTable().size());
        currBB.addInstrHeader(icGen,instr);
    }


}
