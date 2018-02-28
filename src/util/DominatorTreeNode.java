package util;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Getter
public class DominatorTreeNode {
    public int immDom;

    private BasicBlock bb;
    private ArrayList<Integer> child;
    private int nodeId;

    //TODO: add a field of immediate dom.

    public DominatorTreeNode(BasicBlock bb){
        this.bb = bb;
        immDom = -1;
        child = new ArrayList<>();
        nodeId = bb.getId();
    }

    public void addChild(int nodeId){
        child.add(nodeId);
    }

   /* public void addDom(int nodeId){
        dom.add(nodeId);
    }*/

   public boolean isDom(int dom){
       return immDom == dom;
   }

    //TODO: implement a function to find the Nodes doms.
}
