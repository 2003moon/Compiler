package util;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Getter
public class DominatorTreeNode {
    private BasicBlock bb;
    private ArrayList<Integer> dom;
    private ArrayList<Integer> child;
    private int nodeId;

    public DominatorTreeNode(BasicBlock bb){
        this.bb = bb;
        dom = new ArrayList<>();
        child = new ArrayList<>();
        nodeId = bb.getId();
    }

    public void addChild(int nodeId){
        child.add(nodeId);
    }

    public void addDom(int nodeId){
        dom.add(nodeId);
    }
}
