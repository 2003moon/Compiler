package backend;


import frontend.parser;
import optimizer.InstrNode;
import optimizer.Optimizer;
import util.*;

import java.util.LinkedList;
import java.util.Queue;

public class CodeGenerator {
    //TODO: there is no deallocation of registers
    //TODO: arithmic Instruction: for two constants type, it should be processed while register allocation.
    private Optimizer opti;
    private parser ps;
    private CFG cfg;
    private IcGenerator icGen;
    private Queue<Integer> programs;

    public CodeGenerator(Optimizer opti){
        this.opti = opti;
        this.ps = opti.getPs();
        this.icGen = ps.getIcGen();
        this.cfg = icGen.getCfg(0);
        programs = new LinkedList<>();
    }


    public int[] generate(){
        BasicBlock firstBlock = cfg.getBlock(0);
        Instruction firstInstr = getFirstInstr(firstBlock);
        while(firstInstr != null){
            Instruction test = icGen.getInstruction(21);
            if(firstInstr.isDummy()){
                firstInstr = firstInstr.next;
                continue;
            }
            if(firstInstr.isPhi()){
                firstInstr = firstInstr.next;
                continue;
            }
            if(firstInstr.isBranch()){
                firstInstr = generateBranchCode(firstInstr);
            }else if(firstInstr.isIO()){
                firstInstr = generateIoCode(firstInstr);

            }else if(firstInstr.isEnd()){
                putF3(ImmOpcode.RET, 0);
                firstInstr = firstInstr.next;
            }else if(firstInstr.getOp() == Opcode.move){
                firstInstr = generateMoveCode(firstInstr);

            }else{
                firstInstr = generateArithmicCode(firstInstr);
            }
        }
        int size = programs.size();
        int[] codes = new int[size];
        for(int i = 0;i<size;i++){
            codes[i] = programs.poll();
        }
        return codes;
    }

    private Instruction generateIoCode(Instruction curr){
        if(curr.getOp() == Opcode.write){
            ImmOpcode imm = new ImmOpcode(curr);
            Result opr1 = curr.oprand1;
            int regNo = 8;
            if(opr1.getType() == Result.Type.constant){
                load(opr1,regNo);
            }else{
               regNo = getReg(icGen.getInstruction(opr1.getInstr_id()));

            }
            putF2(imm.getImmOp(), 0, regNo, 0);
        }
        return  curr.next;
    }

    private Instruction  generateArithmicCode(Instruction curr){//TODO: this function should be recursive with input Instruction and the queue
        int regNo = getReg(curr);
        Result opr1 = curr.oprand1;
        Result opr2 = curr.oprand2;
        ImmOpcode imm = new ImmOpcode(curr);
        int immOp = imm.getImmOp();
        if(opr1.getType() == Result.Type.constant && opr2.getType() == Result.Type.constant){
            load(opr1,regNo);
        }
        if(0<=immOp && immOp<=14){
            Instruction instr1 = icGen.getInstruction(opr1.getInstr_id());
            Instruction instr2 = icGen.getInstruction(opr2.getInstr_id());
            putF2(immOp,regNo,getReg(instr1),getReg(instr2));
        }else if(16<=immOp && immOp<=30){
            if(opr2.getType() == Result.Type.constant){
                int regNo1 = opr1.getRegNo();
                if(opr1.getType() == Result.Type.instruction){
                    regNo1 = getReg(icGen.getInstruction(opr1.getInstr_id()));
                }
                putF1(immOp,regNo,regNo1,opr2.getValue());
            }else{
                int regNo2 = opr2.getRegNo();
                if(opr2.getType() == Result.Type.instruction){
                    regNo2 = getReg(icGen.getInstruction(opr1.getInstr_id()));
                }
                putF1(immOp,regNo,regNo2,opr1.getValue());
            }
        }
        return curr.next;
    }

    private Instruction generateMoveCode(Instruction curr){
        Result opr1 = curr.oprand1;
        Result opr2 = curr.oprand2;
        load(opr1, opr2.getRegNo());
        curr = curr.next;
        return  curr;
    }

    private Instruction generateBranchCode(Instruction curr){
        Instruction next = curr.next;
        ImmOpcode imm = new ImmOpcode(curr);
        if(next!=null){
            while(next.getBbid() == curr.getBbid() && next.getOp()== Opcode.move){
               next = generateMoveCode(next);
                if(next == null){
                    return  null;
                }
            }
            int immOp = imm.getImmOp();
            BasicBlock currBB = cfg.getBlock(curr.getBbid());
            BasicBlock nonTgBB = cfg.getBlock(next.getBbid());
            int c = 0;
           //
               /* BasicBlock nonTgBB = cfg.getBlock(next.getBbid());

                putF1(immOp, getReg(instr_a),0, nonTgBB.getTotalInstrs()+1);*/
            if(currBB.getBrType() != BasicBlock.BrType.Loopbody){
                c = DFSNonLoop(nonTgBB)+1;
            }else{
                nonTgBB = cfg.getBlock(curr.prev.getBbid());
                c = DFSLoop(nonTgBB,curr.oprand1.getBr_id());
                c = -(c-2);
            }
            if(40<=immOp && immOp <= 45){
                Instruction instr_a = icGen.getInstruction(curr.oprand1.getInstr_id());
                putF1(immOp, getReg(instr_a),0, c);
            }else{
                putF1(immOp, 0,0, c);
            }
        }

        return next;
    }

    private int DFSNonLoop(BasicBlock bb){
        int c = bb.getTotalInstrs();
        for(Integer childid : bb.getChild()){
            BasicBlock child = cfg.getBlock(childid);
            c += DFSNonLoop(child);
        }
        return c;
    }

    private int DFSLoop(BasicBlock bb, int stopId){
        int c = 0;
        while(true){
            c += bb.getTotalInstrs();
            if(bb.getId() == stopId){
                break;
            }
            bb = cfg.getBlock(bb.immDom);
        }
        return c;
    }


    private void load(Result x, int regNo){
        if(x.getType() == Result.Type.constant){
            putF1(ImmOpcode.ADDI, regNo, regNo, x.getValue());
            x.setType(Result.Type.register);
            x.setRegNo(regNo);
        }else{
            putF1(ImmOpcode.ADDI,regNo,x.getRegNo(), 0);
        }

    }

    private void putF1(int op, int a, int b, int c){
        if (c < 0) c ^= 0xFFFF0000;
        if ((a & ~0x1F | b & ~0x1F | c & ~0xFFFF) != 0) {
            System.out.println("Illegal Operand(s) for F1 Format.");
            DLX.bug(1);
        }
        int code =  op << 26 | a << 21 | b << 16 | c & 0xffff;
        programs.offer(code);
    }

    private void putF2(int op,int a, int b, int c){
        if (c < 0) c ^= 0xFFFF0000;
        if ((a & ~0x1F | b & ~0x1F | c & ~0xFFFF) != 0) {
            System.out.println("Illegal Operand(s) for F1 Format.");
            DLX.bug(1);
        }
        int code = op << 26 | a << 21 | b << 16 | c;
        programs.offer(code);
    }

    private void putF3(int op, int c){
        if ((c < 0)) {
            System.out.println("Operand for F3 Format is referencing " +
                    "non-existent memory location.");
            DLX.bug(1);
        }
        int code = op << 26 | c;
        programs.offer(code);
    }

    private int getReg(Instruction instr){
        InstrNode nd = opti.getIdToNode().get(instr.getId());
        return opti.getIdToNode().get(instr.getId()).regNo;
    }
    private Instruction getFirstInstr(BasicBlock bb){
        if(bb.isEmpty()){
            if(bb.getSuccessors().size()!=0){
                bb = cfg.getBlock(bb.getSuccessors().iterator().next());
                return getFirstInstr(bb);
            }else{
                return null;
            }
        }
        return icGen.getInstruction(bb.getFirstInstr());
    }
}
