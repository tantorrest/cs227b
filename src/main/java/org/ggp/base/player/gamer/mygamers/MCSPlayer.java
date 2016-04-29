package org.ggp.base.player.gamer.mygamers;

import java.util.List;
import java.util.Random;

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
        finishBy = timeout - 2000;

        /* calculate game parameter based on game */
        StateMachine game = getStateMachine();
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
    	finishBy = timeout - 500;
    	long start = System.currentTimeMillis();
    	expansionDepth = 2;
    	branchingFactor = getStateMachine().getLegalMoves(getCurrentState(), getRole()).size();
    	expansionDepth = (long) Math.floor((Math.log(finishBy - start) / Math.log(branchingFactor * getNextStateTime))) - 2; // -1
        depthChargeFromCutoffTime = (((double) (averageDepth - expansionDepth) / averageDepth) * depthChargeFromRootTime);
        averageExploredStates = (long) (Math.pow(branchingFactor, expansionDepth - 1));
        exploreEarlyStatesTime = averageExploredStates * getNextStateTime;
        completeChargesTime = (depthChargeFromRootTime + exploreEarlyStatesTime);
        numDepthChargesPerNode = (long) Math.min(((double) finishBy - start) / completeChargesTime, 2);
        printMoveData();

    	return bestMove(getRole(), getCurrentState());
    }

    private Move bestMove(Role role, MachineState state)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	StateMachine game = getStateMachine();
        List<Move> actions = game.findLegals(role, state);
        if (actions.size() == 1) return actions.get(0);
        Move action = actions.get(0);
        double alpha = 0;
        double beta = 100;
        double score = 0;
        Random rgen = new Random();
        int i = rgen.nextInt(actions.size());
        for (; i < 2 * actions.size(); i++) {
        	double result = minscore(role, actions.get(i % actions.size()), state, alpha, beta, 0);
        	if (result == 100) return actions.get(i % actions.size());
        	if (result > score) {
        		score = result;
        		action = actions.get(i % actions.size());
        		if (System.currentTimeMillis() > finishBy) {
        			System.out.println("Main Cutoff after " + i % actions.size() + " nodes out of " + actions.size()+ " nodes");
        			return action;
        		}
        	}
        }
        return action;
    }

    private double maxscore(Role role, MachineState state, double alpha, double beta, int level)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	StateMachine game = getStateMachine();
    	if (game.findTerminalp(state)) {  return game.findReward(role, state); }
    	if (level >= expansionDepth) {
    		return monteCarlo(role, state);
    	}
    	List<Move> actions = game.findLegals(role, state);
    	Random rgen = new Random();
        int i = rgen.nextInt(actions.size());
    	for (; i < 2 * actions.size(); i++) {
    		double result = minscore(role, actions.get(i % actions.size()), state, alpha, beta, level);
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
    private double monteCarlo(Role role, MachineState state)
    		throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
    	double total = 0.0;
    	int count = 0;
    	for (int i = 0; i < numDepthChargesPerNode; i++) {
    		// in case we are close to time out
    		if (System.currentTimeMillis() > finishBy) {
    			if (total == 0) {
//    				System.out.println("Heu at charge " + i + " versus " + numDepthChargesPerNode);
//    				System.out.println(multiHeuristics(role, state));
//    				return multiHeuristics(role, state);
    				return getStateMachine().findReward(role, state);
    			}
//    			System.out.println("Cutoff at charge " + i + " versus " + numDepthChargesPerNode);
				return total / count;
			}
    		MachineState stateForCharge = state.clone();
    		int[] theDepth = new int[1];
    		MachineState finalState = getStateMachine().performDepthCharge(stateForCharge, theDepth);
    		total += getStateMachine().getGoal(finalState, role);
    		count++;
    	}
    	System.out.println("runToCompletion: " + total / numDepthChargesPerNode);
    	return total / count;
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

    private void printMoveData() {
    	System.out.println("Expansion depth--------------------" + expansionDepth);
    	System.out.println("Num Depth Charges per Node---------" + numDepthChargesPerNode);
    	System.out.println("Time of Depth Charge from Cutoff---" + depthChargeFromCutoffTime + "ms");
    	System.out.println("Average Explored States------------" + averageExploredStates);
    	System.out.println("Average Explore Early States Time--" + exploreEarlyStatesTime + "ms");
    	System.out.println("Complete Charges Time--------------" + completeChargesTime + "ms");
    }

    /* private variables */
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

