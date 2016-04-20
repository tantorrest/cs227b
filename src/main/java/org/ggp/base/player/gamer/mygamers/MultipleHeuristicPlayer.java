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

public class MultipleHeuristicPlayer extends SampleGamer{

    @Override
    public Move stateMachineSelectMove(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	finishBy = timeout - 3000; //gives us 3 seconds to give move
    	return bestMove(getRole(), getCurrentState());
    }

    private Move bestMove(Role role, MachineState state)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	long start = System.currentTimeMillis();
    	StateMachine game = getStateMachine();
        List<Move> actions = game.findLegals(role, state);
        Move action = actions.get(0);
        int alpha = 0;
        int beta = 100;
        int score = 0;
        for (int i = 0; i < actions.size(); i++) {
        	int result = (int) minscore(role, actions.get(i), state, alpha, beta, 0);
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

    private double multiHeuristics(Role role, MachineState state){
    	double mobGuess = 0;
    	double goalGuess = 0;
    	double oppMobGuess = 0;
    	try {
			mobGuess = mobility(role, state);
		} catch (MoveDefinitionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

    	try {
			goalGuess = (double) (getStateMachine().findReward(role, state));
		} catch (GoalDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	try {
			oppMobGuess = opponentMobility(role, state);
		} catch (GoalDefinitionException | MoveDefinitionException | TransitionDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	return ((.5 * mobGuess) + (.333 * goalGuess) + (.167 * oppMobGuess));
    }

    private double mobility(Role role, MachineState state) throws MoveDefinitionException {
    	System.out.println("Checking Mobility!");
    	System.out.println(finishBy);
    	StateMachine game = getStateMachine();
    	List<Move> actions = game.findLegals(role, state);
    	List<Move> feasibles = game.findActions(role);
    	return (double) (actions.size() / feasibles.size() * 100);
	}

    // Opponent Mobility
    // 100 means limits opponent mobility the most (good for us)
    // 0   means limits opponent mobility the least (bad for us)
    // extremely conservative heuristic
    private double opponentMobility(Role role, MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
    	System.out.println("Checking Opponent Mobility!");
    	System.out.println(finishBy);
    	StateMachine game = getStateMachine();
    	double maxOpponentMobility = 0;

    	// assume opponent plays noop in current state
    	// hence, find mobility at next state
    	for (Role opponent : game.findRoles()) {
    		if (!opponent.equals(role)) {
	    		double opponentMobility = 0;
	    		for (List<Move> jointMove : game.getLegalJointMoves(state)) {
	        		MachineState newstate = game.getNextState(state, jointMove);
	        		opponentMobility = Math.max(opponentMobility, mobility(opponent, newstate));
	        	}
    			System.out.println(opponentMobility);
    			maxOpponentMobility = (opponentMobility > maxOpponentMobility) ? opponentMobility: maxOpponentMobility;
    		}
    	}
    	return - maxOpponentMobility; // kind of adding a weight to the function
	}

    private double maxscore(Role role, MachineState state, int alpha, int beta, int level)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	StateMachine game = getStateMachine();
    	if (game.findTerminalp(state)) return game.findReward(role, state);
    	if (level >= limit || System.currentTimeMillis() > finishBy) return multiHeuristics(role, state);
    	List<Move> actions = game.findLegals(role, state);
    	for (int i = 0; i < actions.size(); i++) {
    		int result = (int) minscore(role, actions.get(i), state, alpha, beta, level);
    		alpha = Math.max(alpha, result);
    		if (alpha >= beta) return (double)beta;
    	}
    	return (double) alpha;
    }

    private double minscore(Role role, Move action, MachineState state, int alpha, int beta, int level) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	StateMachine game = getStateMachine();
    	for (List<Move> jointMove : game.getLegalJointMoves(state, role, action)) {
    		MachineState newstate = game.getNextState(state, jointMove);
    		int result = (int) maxscore(role, newstate, alpha, beta, level + 1);
    		beta = Math.min(beta, result);
    		if (beta <= alpha) return (double) alpha;
    	}
    	return (double) beta;
    }

    private static final int limit = 7;
    private long finishBy;

}
