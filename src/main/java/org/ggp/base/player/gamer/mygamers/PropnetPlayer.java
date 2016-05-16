package org.ggp.base.player.gamer.mygamers;

import java.util.List;

import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.DualStateMachine;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.propnet.SamplePropNetStateMachine;

public class PropnetPlayer extends SampleGamer {

    @Override
    public void stateMachineMetaGame(long timeout)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	p("Debug Metagaming Phase Propnet");
    	StateMachine prover = getProverStateMachine();
        prover.initialize(getMatch().getGame().getRules());

    	StateMachine propnet = getStateMachine();
    	game = new DualStateMachine(prover, propnet);
    	role = getRole();
    	root = new MultiNode(getCurrentState(), null, null, 1, 0, true);
		expand(root);
		performMCTS(root, timeout - 1000);
    }

//    @Override
//    public void stateMachineMetaGame(long timeout)
//    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
//    	p("Metagaming Phase Propnet");
//    	game = getStateMachine();
//    	role = getRole();
//    	root = new MultiNode(getCurrentState(), null, null, 1, 0, true);
//		expand(root);
//		performMCTS(root, timeout - 1000);
//    }

    @Override
	public StateMachine getInitialStateMachine() {
    	return new CachedStateMachine(new SamplePropNetStateMachine());
    }

	public StateMachine getProverStateMachine() {
    	return new CachedStateMachine(new SamplePropNetStateMachine());
    }


	@Override
    public Move stateMachineSelectMove(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	root = new MultiNode(getCurrentState(), null, null, 1, 0, true);
    	expand(root);
    	isFirstMove = false;
    	performMCTS(root, timeout - 1000);
    	return getBestMove();
    }

	/************* major helper functions *****************/
    private MultiNode select(MultiNode node) {
    	if (node.visits == 0 || game.findTerminalp(node.state)) return node;
    	for (int i = 0; i < node.children.size(); i++) {
    		if (node.children.get(i).visits == 0) return node.children.get(i);
    	}
    	if (node.isMax) {
    		double score = selectfnMax(node.children.get(0));
        	MultiNode result = node.children.get(0);
        	for (int i = 1; i < node.children.size(); i++) {
        		double newscore = selectfnMax(node.children.get(i));
        		if (newscore > score) {
        			score = newscore;
        			result = node.children.get(i);
        		}
        	}
        	return select(result);
    	} else {
        	double score = selectfnMin(node.children.get(0));
        	MultiNode result = node.children.get(0);
        	for (int i = 1; i < node.children.size(); i++) {
        		double newscore = selectfnMin(node.children.get(i));
        		if (newscore > score) {
        			score = newscore;
        			result = node.children.get(i);
        		}
        	}
        	return select(result);
    	}
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

    private void backPropagate(MultiNode node, double score, int depth) {
    	if (depth <= 1 && score == 0) {
    		p("spotted forced loss");
    	}

    	node.utility += score;
    	node.visits++;
    	node.utilities.add(node.utility);
    	if (node.parent != null) {
    		backPropagate(node.parent, score, depth);
    	}
    }

	/************* minor helper functions *****************/
    private void performMCTS(MultiNode root, long timeout)
    		throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
    	int[] depth = { 0 };
    	int numDepthCharges = 0;
    	while (System.currentTimeMillis() < timeout) {
    		double score = 0;
    		MachineState terminal = null;
    		MultiNode selected = select(root);
    		if (!game.findTerminalp(selected.state)) {
    			expand(selected);
        		terminal = game.performDepthCharge(selected.state, depth);
        		numDepthCharges++;
    		} else {
    			terminal = selected.state;
    		}
    		score = game.findReward(role, terminal) / 100.0;
    		backPropagate(selected, score, depth[0]);
    	}
    	p("Num Depth Charges PP: " + numDepthCharges);
    }

    private Move getBestMove() throws MoveDefinitionException {
    	double bestUtility = 0;
		for (MultiNode child : root.children) {
    		if (child.getAveUtility() > bestUtility) {
    			bestUtility = child.getAveUtility();
    			bestMove = child.move;
    		}
    	}
		p("utility PP: " + bestUtility);
		return (bestUtility != 0) ? bestMove : game.getRandomMove(getCurrentState(), role);
    }

    private double selectfnMax(MultiNode node) {
    	return (node.getAveUtility()) + explorationFactor * Math.sqrt(tunedFunction(node));
    }

    private double selectfnMin(MultiNode node) {
    	return (-node.getAveUtility()) + explorationFactor * Math.sqrt(tunedFunction(node));
    }

    private double tunedFunction(MultiNode node) {
    	if (useUCBTuned) {
    		double result = Math.log(node.parent.visits) / node.visits;
        	double factor = Math.min(0.25, adjustedVariance(node));
        	return Math.sqrt(result * factor);
    	} else {
    		return 2 * Math.log(node.parent.visits) / node.visits;
    	}
    }

    private double adjustedVariance(MultiNode node) {
    	double result = 0;
    	for (double utility : node.utilities) {
    		result += Math.pow(utility, 2);
    	}
    	return (0.5 * result) - (Math.pow(node.getAveUtility(), 2)) + (Math.sqrt(2 * Math.log(node.parent.visits) / node.visits));
    }

    /*********************** variables *******************/
    /* dynamic game state data */
    private Move bestMove = null;
    private StateMachine game = null;
    private Role role = null;
    private MultiNode root = null;

    /* game information data */
    private boolean isFirstMove = true;
    private boolean useUCBTuned = false;

    /* game paramter data */
    private double explorationFactor = Math.sqrt(2.1);

    private void p(String message) { System.out.println(message); }
}