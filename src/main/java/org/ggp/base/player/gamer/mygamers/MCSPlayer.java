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

public class MCSPlayer extends SampleGamer {

    @Override
    public void stateMachineMetaGame(long timeout)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        finishBy = 0;
        depthChargeFromRootTime = 100;
        stateExpansionTime = 200;
        numDepthChargesPerNode = 4;
        expansionDepth = 4;
    	printData();
    }

    @Override
    public Move stateMachineSelectMove(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	finishBy = timeout - 1500;
    	return bestMove(getRole(), getCurrentState());
    }

    private Move bestMove(Role role, MachineState state)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	StateMachine game = getStateMachine();
        List<Move> actions = game.findLegals(role, state);
        Move action = actions.get(0);
        double alpha = 0;
        double beta = 100;
        double score = 0;
        for (int i = 0; i < actions.size(); i++) {
        	double result = minscore(role, actions.get(i), state, alpha, beta, 0);
        	if (result == 100) return actions.get(i);
        	if (result > score) {
        		score = result;
        		action = actions.get(i);
        		if (System.currentTimeMillis() > finishBy) return action;
        	}
        }
        return action;
    }

    private double maxscore(Role role, MachineState state, double alpha, double beta, int level)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	StateMachine game = getStateMachine();
    	if (game.findTerminalp(state)) {  return game.findReward(role, state); }
    	if (level >= expansionDepth || System.currentTimeMillis() > finishBy) { return monteCarlo(role, state); }
    	List<Move> actions = game.findLegals(role, state);
    	for (int i = 0; i < actions.size(); i++) {
    		double result = minscore(role, actions.get(i), state, alpha, beta, level);
    		alpha = Math.max(alpha, result);
    		if (alpha >= beta) return beta;
    	}
    	return alpha;
    }

	private double minscore(Role role, Move action, MachineState state, double alpha, double beta, int level)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	StateMachine game = getStateMachine();
    	for (List<Move> jointMove : game.getLegalJointMoves(state, role, action)) {
    		MachineState newstate = game.getNextState(state, jointMove);
    		double result = maxscore(role, newstate, alpha, beta, level + 1);
    		beta = Math.min(beta, result);
    		if (beta <= alpha) return alpha;
    	}
    	return beta;
    }

    private double monteCarlo(Role role, MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
    	double total = 0.0;
    	for (int i = 0; i < numDepthChargesPerNode; i++) {
    		total += depthCharge(role, state);
    	}
    	System.out.println("monteCarlo: " + total / numDepthChargesPerNode);
    	return total / numDepthChargesPerNode;
    }

    private double depthCharge(Role role, MachineState state)
    		throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
    	StateMachine game = getStateMachine();
    	if (game.findTerminalp(state)) { System.out.println("terminal: " + game.findReward(role, state)); return game.findReward(role, state); }
    	List<Move> moves = game.getRandomJointMove(state);
    	return depthCharge(role, game.getNextState(state, moves));
    }

    private void printData() {
    	System.out.println("Expansion depth--------------------" + expansionDepth);
    	System.out.println("Time for Depth Charge from Root----" + depthChargeFromRootTime + "ms");
    	System.out.println("Time for State Expanson------------" + stateExpansionTime + "ms");
    	System.out.println("Num Depth Charges per Node---------" + numDepthChargesPerNode);
    	System.out.println("");
    	System.out.println();
    }

    private long expansionDepth;
    private long finishBy;
    private long depthChargeFromRootTime;
    private long stateExpansionTime;
    private long numDepthChargesPerNode;
}

