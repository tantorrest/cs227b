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
        finishBy = timeout - 1000;
        /* defaults */
        stateExpansionTime = 1;
        numDepthChargesPerNode = 100;
        expansionDepth = 4;
        firstMove = false;

        /* calculate above values based on game */
        StateMachine game = getStateMachine();
        long start = System.currentTimeMillis();
        game.getNextStates(getCurrentState());
        branchingFactor = getBranchingFactor(getRole(), getCurrentState(), 0, 0);
        start = System.currentTimeMillis();
        long count = 0;
        while (System.currentTimeMillis() < finishBy) {
    		depthCharge(getRole(), getCurrentState());
    		count++;
        }
        depthChargeFromRootTime = ((double) System.currentTimeMillis() - start) / count;
    	printData();
    }

    @Override
    public Move stateMachineSelectMove(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	if (firstMove) {
    		finishBy = timeout - 2000;
        	long start = System.currentTimeMillis();
        	expansionDepth = (long) Math.floor((Math.log(finishBy - start) / Math.log(branchingFactor))) - 1;
        	numDepthChargesPerNode = (long) Math.ceil((finishBy - start) / Math.pow(branchingFactor, expansionDepth - 1));
        	System.out.println("Expansion Depth---" + expansionDepth);
        	System.out.println("Charges per Node--" + numDepthChargesPerNode);
        	firstMove = true;
    	}

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


	/********** helper functions ************/

    private double monteCarlo(Role role, MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
    	double total = 0.0;
    	int count = 0;
    	for (int i = 0; i < numDepthChargesPerNode; i++) {
    		if (System.currentTimeMillis() > finishBy) { return total / count; } // or heuristic
    		total += depthCharge(role, state);
    		count++;
    	}
    	System.out.println("monteCarlo: " + total / numDepthChargesPerNode);
    	return total / count;
    }

    private long getBranchingFactor(Role role, MachineState state, long factor, long depth)
    		throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
    	StateMachine game = getStateMachine();
    	if (game.findTerminalp(state)) { return (long) Math.ceil(factor / depth); }
    	List<Move> moves = game.getRandomJointMove(state);
    	return getBranchingFactor(role, game.getNextState(state, moves), factor + game.getLegalJointMoves(state).size(), depth + 1);
    }

    private double depthCharge(Role role, MachineState state)
    		throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
    	StateMachine game = getStateMachine();
    	if (game.findTerminalp(state)) {
//    		System.out.println("terminal: " + game.findReward(role, state));
    		return game.findReward(role, state);
    	}
    	List<Move> moves = game.getRandomJointMove(state);
    	return depthCharge(role, game.getNextState(state, moves));
    }

    private void printData() {
    	System.out.println("Expansion depth--------------------" + expansionDepth);
    	System.out.println("Time for Depth Charge from Root----" + depthChargeFromRootTime + "ms");
    	System.out.println("Time for State Expanson------------" + stateExpansionTime + "ms");
    	System.out.println("Num Depth Charges per Node---------" + numDepthChargesPerNode);
    	System.out.println("Branching Factor-------------------" + branchingFactor);
    }

    private long expansionDepth;
    private long finishBy;
    private double depthChargeFromRootTime;
    private double stateExpansionTime;
    private long numDepthChargesPerNode;
    private long branchingFactor;
    private boolean firstMove;
}

