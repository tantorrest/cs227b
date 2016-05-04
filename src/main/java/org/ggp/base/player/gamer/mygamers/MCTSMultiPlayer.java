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
    }

	@Override
    public Move stateMachineSelectMove(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		root = new MultiNode(getCurrentState(), null, null, 1, 0, true);
		explorationFactor = Math.sqrt(2);
		bestMove = game.getRandomMove(getCurrentState(), role);
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
    		for (MultiNode child : root.children) {
    			p("Child " + child.move + " value: " + child.getAveUtility());
        	}
    	}

    	// return the best value
		double bestUtility = Double.POSITIVE_INFINITY;
		p(root.children.size() + "");
		for (MultiNode child : root.children) {
			System.out.println("Child " + child.move + " value: " + child.getAveUtility());
    		if (child.getAveUtility() < bestUtility) {
    			bestUtility = child.getAveUtility();
    			bestMove = child.move;
    		}
    	}

    	p("Chosen Move: " + bestMove.toString());
		return bestMove;
    }


	// works as far as I know
    private double selectfn(MultiNode node) {
    	return (node.getAveUtility()) + explorationFactor * Math.sqrt(2 * Math.log(node.parent.visits) / node.visits);
    }

    // pretty sure this function does what it's meant to do
    private void backPropagate(MultiNode node, double score) {
    	if (node.isMax) {
    		node.utility += score;
    	} else {
    		node.utility -= score;
    	}
    	node.visits++;
    	if (node.parent != null) {
    		backPropagate(node.parent, score);
    	}
    }

    private void p(String message) {
    	System.out.println(message);
    }

    /* private variables */
    private Move bestMove;
    private StateMachine game;
    private Role role;
    private MultiNode root;
    private double explorationFactor;


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

    // find an expandable node
    private MultiNode select(MultiNode node) {
    	if (node.visits == 0 || game.findTerminalp(node.state)) return node;
    	for (int i = 0; i < node.children.size(); i++) {
    		if (node.children.get(i).visits == 0) return node.children.get(i);
    	}
    	double score = Double.NEGATIVE_INFINITY;
    	MultiNode result = node;
    	for (int i = 0; i < node.children.size(); i++) {
    		double newscore = selectfn(node.children.get(i));
    		if (newscore > score) {
    			score = newscore;
    			result = node.children.get(i);
    		}
    	}
    	return select(result);
    }
}