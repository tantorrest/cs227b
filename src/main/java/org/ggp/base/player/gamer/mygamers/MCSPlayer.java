package org.ggp.base.player.gamer.mygamers;

import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
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
    public Move stateMachineSelectMove(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	finishBy = timeout - 1500;
    	return bestMove(getRole(), getCurrentState());
    }

    private Move bestMove(Role role, MachineState state)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	long start = System.currentTimeMillis();
    	StateMachine game = getStateMachine();
        List<Move> actions = game.findLegals(role, state);
        Move action = actions.get(0);
        double score = 0;
        for (int i = 0; i < actions.size(); i++) {
        	double result = minscore(role, actions.get(i), state, 0);
        	if (result == 100) return actions.get(i);
        	if (result > score) {
        		score = result;
        		action = actions.get(i);
        		if (System.currentTimeMillis() > finishBy) return action;
        	}
        }
        long stop = System.currentTimeMillis();
        notifyObservers(new GamerSelectedMoveEvent(actions, action, stop - start));
        return action;
    }

    private double maxscore(Role role, MachineState state, int level)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	StateMachine game = getStateMachine();
    	if (game.findTerminalp(state)) {  return game.findReward(role, state); }
    	if (level >= limit || System.currentTimeMillis() > finishBy) { return monteCarlo(role, state, 10); }
    	List<Move> actions = game.findLegals(role, state);
    	double score = 0;
    	for (int i = 0; i < actions.size(); i++) {
    		double result = minscore(role, actions.get(i), state, level);
    		if (result == 100) return 100;
    		if (result > score) { score = result; }
    	}
    	return score;
    }

    private double monteCarlo(Role role, MachineState state, int count) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
    	double total = 0.0;
    	for (int i = 0; i < count; i++) {
    		total += depthCharge(role, state);
    	}
    	System.out.println("monteCarlo: " + total / count);
    	return total/count;
    }

    private double depthCharge(Role role, MachineState state)
    		throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
    	StateMachine game = getStateMachine();
    	if (game.findTerminalp(state)) { System.out.println("terminal: " + game.findReward(role, state)); return game.findReward(role, state); }
    	List<Move> moves = game.getRandomJointMove(state);
    	return depthCharge(role, game.getNextState(state, moves));
    }

	private double minscore(Role role, Move action, MachineState state, int level)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	StateMachine game = getStateMachine();
    	double score = 100;
    	for (List<Move> jointMove : game.getLegalJointMoves(state, role, action)) {
    		MachineState newstate = game.getNextState(state, jointMove);
    		double result = maxscore(role, newstate, level + 1);
	        if (result == 0) { return 0; }
	        if (result < score) { score = result; }
    	}
    	return score;
    }

    private static final int limit = 7;
    private long finishBy;
}

