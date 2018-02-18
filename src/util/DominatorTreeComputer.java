package util;

import java.util.*;

public class DominatorTreeComputer {
    private CFG cfg;
    private Map<Integer, DominatorTreeNode> nodeMap;
    private DominatorTreeNode root;

    public DominatorTreeComputer(CFG cfg){
        this.cfg = cfg;
        nodeMap = new HashMap<>();
        initMap();
    }

    public void computing(){
        Queue<Integer> nodes = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        for(Integer succ : root.getBb().getSuccessors()){
            nodes.add(succ);
        }
        while(!nodes.isEmpty()){
            int size = nodes.size();
            for(int i = 0;i<size;i++){
                int succ = nodes.poll();
                visited.add(succ);
                DominatorTreeNode nd = nodeMap.get(succ);
                computeDom(nd);
                computeChild(nd);
                for(Integer ndSucc : nd.getBb().getSuccessors()){
                    if(!visited.contains(ndSucc)){
                        nodes.offer(ndSucc);
                    }

                }
            }
        }

    }

    public DominatorTreeNode getNode(int nodeId){
        return  nodeMap.get(nodeId);
    }

    private void computeDom(DominatorTreeNode nd){
        Set<Integer> preds = nd.getBb().getPredecessors();
        ArrayList<Integer> doms = new ArrayList<>();
        for(Integer id:preds){
            DominatorTreeNode predNd = getNode(id);
            Collections.sort(predNd.getDom());
            doms = intersectDom(predNd.getDom(),doms);
        }
        for(int i = 0;i<doms.size();i++){
            nd.addDom(doms.get(i));
        }
        nd.addDom(nd.getNodeId());
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

    private void computeChild(DominatorTreeNode nd){
        ArrayList<Integer> doms = nd.getDom();
        DominatorTreeNode parentNd = nodeMap.get(doms.get(doms.size()-2));
        parentNd.addChild(nd.getNodeId());
    }

    private void initMap(){
        for(Map.Entry<Integer, BasicBlock> entry : cfg.getBbMap().entrySet()){
            int key = entry.getKey();
            BasicBlock bb = entry.getValue();
            if(key == 0){
                root = new DominatorTreeNode(bb);
                nodeMap.put(0, root);
                root.addDom(0);

            }else{
                DominatorTreeNode node = new DominatorTreeNode(bb);
                nodeMap.put(key, node);
            }

        }
    }



}
