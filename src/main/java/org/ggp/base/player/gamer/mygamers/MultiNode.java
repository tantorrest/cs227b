package org.ggp.base.player.gamer.mygamers;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

public class MultiNode {
    public List<MultiNode> children = new ArrayList<MultiNode>();
    public MultiNode parent = null;
    public int visits = 0;
    public MachineState state = null;
    public double utility = 0;
    public Move move = null;
    public boolean isMax = true;
    public ArrayList<Double> utilities = new ArrayList<Double>();
    public List<Move> jointMoves = new ArrayList<Move>();

    public MultiNode(MachineState state, Move move, List<Move> jointMoves, int visits, double utility, boolean isMax) {
    	this.visits = visits;
    	this.utility = utility;
        this.state = state;
        this.move = move;
        this.parent = null;
        this.isMax = isMax;
        this.jointMoves = jointMoves;
    }

    public void setParent(MultiNode parent) {
        this.parent = parent;
    }

    public void addChild(MultiNode child) {
        child.setParent(this);
        this.children.add(child);
    }

    public void updateUtilityAndVisits(double score) {
    	this.utility += score;
    	this.visits++;
    }

    public double getAveUtility() {
    	return (this.visits != 0) ? this.utility / this.visits : 0;
    }
}
