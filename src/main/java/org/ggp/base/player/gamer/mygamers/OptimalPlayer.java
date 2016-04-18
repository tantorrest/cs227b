package org.ggp.base.player.gamer.mygamers;

import java.util.ArrayList;
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
/*
 * Tailored to Single Player games
 *
 */

public class OptimalPlayer extends SampleGamer {

	@Override
    public Move stateMachineSelectMove(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	return bestMove(getRole(), getCurrentState());
    }


    private Move bestMove(Role role, MachineState state)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	long start = System.currentTimeMillis();
    	StateMachine game = getStateMachine();
        List<Move> actions = game.findLegals(role, state);
        Move action = actions.get(0);
        int score = 0;
        for (int i = 0; i < actions.size(); i++) {
        	List<Move> ourAction = new ArrayList<Move>();
        	ourAction.add(actions.get(i));
        	int result = maxscore(role, game.findNext(ourAction, state));
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

    private int maxscore(Role role, MachineState state) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException{
    	StateMachine game = getStateMachine();
    	if(game.findTerminalp(state)) return game.findReward(role, state);
    	List<Move> actions = game.findLegals(role, state);
        int score = 0;
        for (int i = 0; i < actions.size(); i++) {
        	List<Move> ourAction = new ArrayList<Move>();
        	ourAction.add(actions.get(i));
        	int result = maxscore(role, game.findNext(ourAction, state));
        	if (result > score) {
        		score = result;
        	}
        }
        return score;
    }


}