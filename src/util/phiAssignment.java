package util;

import lombok.Getter;

@Getter
public class phiAssignment {

    public int backupV;

    private int address;
    private int[] versions;
    private int[] preds;
    private Instruction instr;

    public phiAssignment(int address){
        this.address = address;
        versions = new int[2];
        preds = new int[2];
        preds[0] = -1; preds[1] = -1;
        instr = null;
    }

    public void updateIth(int i,int newver, int bbid){
        versions[i] = newver;
        preds[i] = bbid;
        if(instr!=null){
            Result res = new Result(Result.Type.variable, address);
            res.setVersion(newver);
            instr.updatePhi(i, res, bbid);
        }

    }

    public void constructInstr(IcGenerator icGen, BasicBlock currBB){
        Result r1 = new Result(Result.Type.variable, address);
        Result r2 = new Result(Result.Type.variable, address);
        r1.setVersion(versions[0]); r2.setVersion(versions[1]);
        instr = new Instruction(r1,r2,Opcode.phi);
        instr.setId(icGen.getInstrTable().size());
        instr.pred1 = preds[0]; instr.pred2 = preds[1];
        currBB.addInstrHeader(icGen,instr);
    }


}
