package org.ggp.base.player.gamer.mygamers;

import java.util.List;

import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MCTSMultiPlayer extends SampleGamer {

    @Override
    public void stateMachineMetaGame(long timeout)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	game = getStateMachine();
    	role = getRole();
    	p("Doing metagaming");

        finishBy = timeout - 2000;

        /* calculate game parameters based on game */
        branchingFactor = getBranchingFactor(getRole(), getCurrentState(), 0, 0);

        long start = System.currentTimeMillis();
        MachineState state  = getCurrentState();
        final int[] theDepth = { 0 };
        long count = 0;
        while (System.currentTimeMillis() < finishBy) {
        	MachineState stateForCharge = state.clone();
    		game.performDepthCharge(stateForCharge, theDepth);
    		averageDepth += theDepth[0];
    		count++;
        }
        depthChargeFromRootTime = ((double) System.currentTimeMillis() - start) / count;
        averageDepth /= count;
        getNextStateTime = Math.max(1, depthChargeFromRootTime / averageDepth);
        printMetaGameData();
    }

	@Override
    public Move stateMachineSelectMove(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		root = new MultiNode(getCurrentState(), null, null, 1, 0, true);
		explorationFactor = Math.sqrt(2);
		expand(root);
    	while (System.currentTimeMillis() < timeout - 1000) {
    		double score = 0;
    		MachineState terminal = null;
    		MultiNode selected = select(root);
    		if (!game.findTerminalp(selected.state)) {
    			expand(selected);
        		terminal = game.performDepthCharge(selected.state, null);
    		} else {
    			terminal = selected.state;
    		}
    		score = game.findReward(role, terminal) / 100.0;
    		backPropagate(selected, score);
//    		for (MultiNode child : root.children) {
//    			p("Child " + child.move + " value: " + child.getAveUtility());
//        	}
    	}

    	// return the best value
		double bestUtility = Double.POSITIVE_INFINITY;
		p(root.children.size() + "");
		for (MultiNode child : root.children) {
    		if (child.getAveUtility() < bestUtility) {
    			p("Child " + child.move + " value: " + child.getAveUtility());
    			bestUtility = child.getAveUtility();
    			bestMove = child.move;
    		}
    	}

    	p("Chosen Move: " + bestMove.toString());
		return (bestUtility != 0) ? bestMove : game.getRandomMove(getCurrentState(), role);
    }


	// works as far as I know
    private double selectfn(MultiNode node) {
    	return (node.getAveUtility()) + explorationFactor * Math.sqrt(2 * Math.log(node.parent.visits) / node.visits);
    }

    private double selectfn2(MultiNode node) {
    	return (node.getAveUtility()) + explorationFactor * Math.sqrt(tunedFunction(node));
    }

    private double tunedFunction(MultiNode node) {
    	double result = Math.log(node.parent.visits) / node.visits;
    	double factor = Math.min(0.25, adjustedVariance(node));
//    	p("tunedFunction: " + Math.sqrt(result * factor));
    	return Math.sqrt(result * factor);
    }

    private double adjustedVariance(MultiNode node) {
    	double result = 0;
    	for (double utility : node.utilities) {
    		result += Math.pow(utility, 2);
    	}
    	return (0.5 * result) - (Math.pow(node.getAveUtility(), 2)) + (Math.sqrt(2 * Math.log(node.parent.visits) / node.visits));
    }

    // pretty sure this function does what it's meant to do
    private void backPropagate(MultiNode node, double score) {
    	if (node.isMax) {
    		node.utility += score;
    	} else {
    		node.utility -= score;
    	}
    	node.utilities.add(node.utility);
    	node.visits++;
    	if (node.parent != null) {
    		backPropagate(node.parent, score);
    	}
    }

    private void p(String message) {
    	System.out.println(message);
    }

    private void expand(MultiNode node)
    		throws MoveDefinitionException, TransitionDefinitionException {
    	if (node.isMax) {
    		List<Move> moves = game.getLegalMoves(node.state, role);
    		for (Move move : moves) {
        		MultiNode newnode = new MultiNode(node.state, move, null, 0, 0, !node.isMax); // alternate state
        		node.addChild(newnode);
    		}
    	} else {
    		List<List<Move>> jointMoves = game.getLegalJointMoves(node.state, role, node.move);
    		for (List<Move> jointMove : jointMoves) {
    			MachineState nextState = game.getNextState(node.state, jointMove);
    			MultiNode newnode = new MultiNode(nextState, null, jointMove, 0, 0, !node.isMax);
    			node.addChild(newnode);
    		}
    	}
    }

    private MultiNode select(MultiNode node) {
    	if (node.visits == 0 || game.findTerminalp(node.state)) return node;
    	for (int i = 0; i < node.children.size(); i++) {
    		if (node.children.get(i).visits == 0) return node.children.get(i);
    	}
    	double score = Double.NEGATIVE_INFINITY;
    	MultiNode result = node;
    	for (int i = 0; i < node.children.size(); i++) {
    		double newscore = selectfn(node.children.get(i));
//    		double newscore = selectfn2(node.children.get(i));
    		if (newscore > score) {
    			score = newscore;
    			result = node.children.get(i);
    		}
    	}
    	return select(result);
    }

    private long getBranchingFactor(Role role, MachineState state, long factor, long depth)
    		throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
    	StateMachine game = getStateMachine();
    	if (game.findTerminalp(state)) { return (long) Math.ceil(factor / depth); }
    	List<Move> moves = game.getRandomJointMove(state);
    	return getBranchingFactor(role, game.getNextState(state, moves), factor + game.getLegalJointMoves(state).size(), depth + 1);
    }

    /* game data */
    private void printMetaGameData() {
    	System.out.println("Time for Depth Charge from Root----" + depthChargeFromRootTime + "ms");
    	System.out.println("Branching Factor-------------------" + branchingFactor);
    	System.out.println("Average Depth----------------------" + averageDepth);
    	System.out.println("Time to Get Next State-------------" + getNextStateTime);
    }

    /*********************** variables *******************/

    /* private variables */
    private Move bestMove;
    private StateMachine game;
    private Role role;
    private MultiNode root;
    private double explorationFactor;


    /* metagaming data */
    private long expansionDepth = 4;
    private long finishBy = 1;
    private double depthChargeFromRootTime = 40;
    private long numDepthChargesPerNode = 100;
    private long branchingFactor = 5;
    private long averageDepth = 1;
    private long averageExploredStates = 1;
    private double exploreEarlyStatesTime = 1;
    private double getNextStateTime = 1000;
    private double depthChargeFromCutoffTime = 1;
    private double completeChargesTime = 0;
}