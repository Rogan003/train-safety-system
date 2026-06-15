package com.ftn.sbnz.train.model.facts;

import java.io.Serializable;

public class RouteNode implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private boolean clear;
    private RouteNode left;
    private RouteNode right;

    public RouteNode() {}

    public RouteNode(String id, boolean clear) {
        this.id = id;
        this.clear = clear;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public boolean isClear() { return clear; }
    public void setClear(boolean clear) { this.clear = clear; }

    public RouteNode getLeft() { return left; }
    public void setLeft(RouteNode left) { this.left = left; }

    public RouteNode getRight() { return right; }
    public void setRight(RouteNode right) { this.right = right; }
}
