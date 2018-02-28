package util;

import java.util.*;

public class DominatorTreeComputer {
    private Map<Integer, BasicBlock> nodeMap;
    private BasicBlock root;

    public DominatorTreeComputer(){
        nodeMap = new HashMap<>();
    }


    public boolean hasNode(int nodeId){
        return nodeMap.containsKey(nodeId);
    }

    public void addNode(BasicBlock bb){
        nodeMap.put(bb.getId(), bb);
    }
    public BasicBlock getNode(int nodeId){
        return  nodeMap.get(nodeId);
    }


    private ArrayList<Integer> intersectDom(ArrayList<Integer> predDom, ArrayList<Integer> ndDom){
        int ptr1 = 0, ptr2 = 0;
        TreeSet<Integer> res = new TreeSet<>();
        while(ptr1<predDom.size() && ptr2 <ndDom.size()){
            int dom1 = predDom.get(ptr1);
            int dom2 = predDom.get(ptr2);
            if(dom1 == dom2){
                res.add(dom1);
                ptr1++;
                ptr2++;
            }else if(dom1<dom2){
                ptr1++;
            }else{
                ptr2++;
            }
        }

        if(ndDom.size() == 0){
            for(int i= 0;i<predDom.size();i++){
                ndDom.add(predDom.get(i));
            }
        }
         return new ArrayList<>(ndDom);
    }


    private void initMap(CFG cfg){
        for(Map.Entry<Integer, BasicBlock> entry : cfg.getBbMap().entrySet()){
            int key = entry.getKey();
            BasicBlock bb = entry.getValue();
            if(key == 0){
                root = bb;
            }
            nodeMap.put(key, bb);
        }
    }



}
