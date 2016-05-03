package org.ggp.base.player.gamer.mygamers;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

public class MCTSNode {
    private List<MCTSNode> children = new ArrayList<MCTSNode>();
    private MCTSNode parent = null;
    private int visits = 0;
    private MachineState state = null;
    private double utility = 0;
    private Move move = null;

    public MCTSNode(MachineState state, Move move, int visits, double utility) {
    	this.visits = visits;
    	this.utility = utility;
        this.state = state;
        this.move = move;
        this.parent = null;
    }

    public List<MCTSNode> getChildren() {
        return children;
    }

    public void setParent(MCTSNode parent) {
        this.parent = parent;
    }

    public void addChild(MCTSNode child) {
        child.setParent(this);
        this.children.add(child);
    }

    public int getVisits() {
    	return this.visits;
    }

    public double getUtility() {
    	return this.utility;
    }

    public void updateUtilityAndVisits(double score) {
//    	this.utility = ((this.utility * this.visits) + score) / (this.visits + 1);
    	this.utility += score;
    	this.visits++;
    }

    public MCTSNode getParent() {
    	return parent;
    }

    public MachineState getState() {
    	return this.state;
    }

    public Move getMove() {
    	return this.move;
    }

    public double getAveReward() {
    	return (this.visits != 0) ? this.utility / this.visits : 0;
    }
}
