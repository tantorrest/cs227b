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

public class WeightedHeuristicsBoundedDepthSearchPlayer extends SampleGamer {

    @Override
    public Move stateMachineSelectMove(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	finishBy = timeout - 3000;
    	return bestMove(getRole(), getCurrentState());
    }

    private Move bestMove(Role role, MachineState state)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	long start = System.currentTimeMillis();

    	StateMachine game = getStateMachine();
        List<Move> actions = game.findLegals(role, state);
        limit = 12; // make variable
        Move action = actions.get(0);
        double alpha = 0;
        double beta = 100;
        double score = 0;
        for (int i = 0; i < actions.size(); i++) {
        	// break early
        	if (System.currentTimeMillis() > finishBy) return action;
        	double result = minscore(role, actions.get(i), state, alpha, beta, 0);
        	if (result == 100) return actions.get(i);
        	if (result > score) {
        		score = result;
        		action = actions.get(i);
        	}
        }
        long stop = System.currentTimeMillis();
        notifyObservers(new GamerSelectedMoveEvent(actions, action, stop - start));
        return action;
    }

    private double maxscore(Role role, MachineState state, double alpha, double beta, int level)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	StateMachine game = getStateMachine();
    	if (game.findTerminalp(state)) return game.findReward(role, state);
    	// added heuristic value here
    	if (level >= limit || System.currentTimeMillis() > finishBy) return weightedHeuristicValue(role, state);
    	List<Move> actions = game.findLegals(role, state);
    	for (int i = 0; i < actions.size(); i++) {
    		double result = minscore(role, actions.get(i), state, alpha, beta, level);
    		alpha = Math.max(alpha, result);
    		if (alpha >= beta) return beta;
    	}
    	return alpha;
    }

    // Opponent Mobility
    // 100 means limits opponent mobility the most (good for us)
    // 0   means limits opponent mobility the least (bad for us)
    // extremely conservative heuristic
    // wishes to reduce the average moves of players
    private double opponentMobility(Role role, MachineState state)
    		throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
    	StateMachine game = getStateMachine();
    	double maxOpponentMobility = 100;
    	for (Move action : game.getLegalMoves(state, role)) {
    		double legalOpponentsMoves = game.getLegalJointMoves(state, role, action).size();
    		double allPossibleMoves = getAllOpponentPossibleMoves(state, role) + getStateMachine().findRoles().size() - 1;
    		maxOpponentMobility = Math.min((legalOpponentsMoves / allPossibleMoves) * 100, maxOpponentMobility);
    		if (System.currentTimeMillis() > finishBy) return 100 - maxOpponentMobility;
    	}
    	return 100 - maxOpponentMobility;
	}

    private double getAllOpponentPossibleMoves(MachineState state, Role role)
    		throws MoveDefinitionException {
    	double numMoves = 1;
    	for (Role opponent : getStateMachine().findRoles()) {
    		if (!role.equals(opponent)) {
    			numMoves *= (getStateMachine().findActions(opponent).size() + 1);
    		}
    	}
    	return numMoves;
    }

	private double minscore(Role role, Move action, MachineState state, double alpha, double beta, int level)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	StateMachine game = getStateMachine();
    	// break if time is up
    	for (List<Move> jointMove : game.getLegalJointMoves(state, role, action)) {
    		MachineState newstate = game.getNextState(state, jointMove);
    		double result = maxscore(role, newstate, alpha, beta, level + 1);
    		beta = Math.min(beta, result);
    		if (beta <= alpha) return alpha;
    	}
    	return beta;
    }

	private double weightedHeuristicValue(Role role, MachineState state)
			throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		System.out.println((1/3) * mobility(role, state) + (1/3) * goal(role, state) + (1/3) * opponentMobility(role, state));
		return (1/3) * mobility(role, state) + (1/3) * goal(role, state) + (1/3) * opponentMobility(role, state);
	}

	private double goal(Role role, MachineState state)
			throws GoalDefinitionException {
		return getStateMachine().findReward(role, state);
	}

    private double mobility(Role role, MachineState state) throws MoveDefinitionException {
    	StateMachine game = getStateMachine();
    	List<Move> actions = game.findLegals(role, state);
    	List<Move> feasibles = game.findActions(role);
    	return ((double) actions.size() / feasibles.size() * 100);
	}

	// make estimation of result
    private int limit;
    private long finishBy;
}
