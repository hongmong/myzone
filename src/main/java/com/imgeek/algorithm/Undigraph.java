package com.imgeek.algorithm;

import java.util.ArrayList;

/**
 * 无向图
 *
 * 使用邻接表存储
 */

//边
class Edge<T> {
    UndigraphNode<T> node;
    Edge<T> nextEdge;

    public UndigraphNode<T> getNode() {
        return node;
    }

    public void setNode(UndigraphNode<T> node) {
        this.node = node;
    }

    public Edge<T> getNextEdge() {
        return nextEdge;
    }

    public void setNextEdge(Edge<T> nextEdge) {
        this.nextEdge = nextEdge;
    }
}

//顶点
class UndigraphNode<T> {
    T data;
    int nodeIndex;
    Edge firstEdge;

    public int getNodeIndex() {
        return nodeIndex;
    }

    public void setNodeIndex(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Edge getFirstEdge() {
        return firstEdge;
    }

    public void setFirstEdge(Edge firstEdge) {
        this.firstEdge = firstEdge;
    }
}

//图
public class Undigraph<T> {
    ArrayList<UndigraphNode<T>> nodeList;

    public Undigraph() {
        this.nodeList = new ArrayList<UndigraphNode<T>>();
    }

    public ArrayList<UndigraphNode<T>> getNodeList() {
        return nodeList;
    }

    public void setNodeList(ArrayList<UndigraphNode<T>> nodeList) {
        this.nodeList = nodeList;
    }

    public void addNode(UndigraphNode<T> node) {
        this.nodeList.add(node);
    }

    public int getNodeNum() {
        return this.nodeList.size();
    }


    //深度优先遍历
    public String depthFirstTraverse() {
        int nodeNum = getNodeNum();
        boolean[] nodeFlag = new boolean[nodeNum];
        for (int i = 0; i < nodeNum; i++) {
            nodeFlag[i] = false;
        }

        String result = "";
        for (int i = 0; i < nodeNum; i++) {
            if (nodeFlag[i] == false) {
                result += depthFirstSearch(this.nodeList.get(i), nodeFlag);
            }
        }
        return result;
    }

    private String depthFirstSearch(UndigraphNode<T> node, boolean[] nodeFlag) {
        nodeFlag[node.getNodeIndex()] = true;
        String result = node.getData().toString() + " ";

        Edge edge = node.getFirstEdge();
        while (edge != null) {
            UndigraphNode<T> nextNode = edge.getNode();
            if (nodeFlag[nextNode.getNodeIndex()] == false) {
                result += depthFirstSearch(nextNode, nodeFlag);
            }
            edge = edge.getNextEdge();
        }
        return result;
    }
}













