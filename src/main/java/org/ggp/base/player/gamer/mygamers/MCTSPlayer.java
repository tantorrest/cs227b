package org.ggp.base.player.gamer.mygamers;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MCTSPlayer extends SampleGamer {

    @Override
    public void stateMachineMetaGame(long timeout)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	game = getStateMachine();
    	role = getRole();
    	explorationFactor = Math.sqrt(2);
    	p("Doing metagaming");
    }

	@Override
    public Move stateMachineSelectMove(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		root = new MCTSNode(getCurrentState(), null, 0, 0);
    	bestMove = game.getRandomMove(getCurrentState(), role);

    	while (System.currentTimeMillis() < timeout - 1000) {
    		MCTSNode selected = select2(root);
    		expand(selected);
    		MachineState terminal = game.performDepthCharge(selected.getState(), null);
    		double score = (game.findReward(role, terminal) - 50) / 50.0; // range between -1 and 1
    		backPropagate(selected, score);
    	}

		for (MCTSNode s : root.getChildren()) {
			p("move " + s.getMove() + " has value " + selectfn(s));
		}

    	double bestUtility = Double.NEGATIVE_INFINITY;
    	for (MCTSNode child : root.getChildren()) {
    		if (child.getAveReward() > bestUtility) {
    			p("Updating child utility: " + selectfn(child) + " move: " + child.getMove());
    			bestUtility = child.getAveReward();
    			bestMove = child.getMove();
    		}
    	}

    	p(bestMove.toString());
		return bestMove;
    }

    private double maxscore(Role role, MachineState state, double alpha, double beta)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	StateMachine game = getStateMachine();
    	if (game.findTerminalp(state)) return game.findReward(role, state);
    	List<Move> actions = game.findLegals(role, state);
    	for (int i = 0; i < actions.size(); i++) {
    		double result = minscore(role, actions.get(i), state, alpha, beta);
    		alpha = Math.max(alpha, result);
    		if (alpha >= beta) return beta;
    	}
    	return alpha;
    }

    private double minscore(Role role, Move action, MachineState state, double alpha, double beta)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	StateMachine game = getStateMachine();
    	for (List<Move> jointMove : game.getLegalJointMoves(state, role, action)) {
    		MachineState newstate = game.getNextState(state, jointMove);
    		double result = maxscore(role, newstate, alpha, beta);
    		beta = Math.min(beta, result);
    		if (beta <= alpha) return alpha;
    	}
    	return beta;
    }

    // optimized version
    private MCTSNode select2(MCTSNode node) {
    	if (node.getVisits() == 0) return node;

    	while (node.getVisits() != 0) {
        	double score = Double.NEGATIVE_INFINITY;
        	MCTSNode result = node; // is this okay?
        	for (int i = 0; i < node.getChildren().size(); i++) {
        		if (node.getChildren().get(i).getVisits() == 0) return node.getChildren().get(i);
        		double newscore = selectfn(node.getChildren().get(i));
        		if (newscore > score) {
        			score = newscore;
        			result = node.getChildren().get(i);
        		}
        	}
        	node = result;
    	}
    	return node;
    }

    private double selectfn(MCTSNode node) {
    	return (node.getAveReward()) + explorationFactor * Math.sqrt(2 * Math.log(node.getParent().getVisits()) / node.getVisits());
    }

    private void expand(MCTSNode node)
    		throws MoveDefinitionException, TransitionDefinitionException {
    	List<Move> actions = game.getLegalMoves(node.getState(), role);
    	for (int i = 0; i < actions.size(); i++) {
    		List<Move> jointMove = new ArrayList<Move>();
    		jointMove.add(actions.get(i));
    		MachineState newstate = game.getNextState(getCurrentState(), jointMove); // todo for multiplayer
    		MCTSNode newnode = new MCTSNode(newstate, actions.get(i), 0, 0); // index of our move
    		node.addChild(newnode);
    	}
    }

    private void backPropagate(MCTSNode node, double score) {
    	node.updateUtilityAndVisits(score);
    	if (node.getParent() != null) {
    		backPropagate(node.getParent(), score);
    	}
    }

    private void p(String message) {
    	System.out.println(message);
    }
    /* private variables */
    private Move bestMove;
    private StateMachine game;
    private Role role;
    private MCTSNode root;
    private double explorationFactor;
}