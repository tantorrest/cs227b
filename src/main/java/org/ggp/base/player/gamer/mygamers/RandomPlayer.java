package org.ggp.base.player.gamer.mygamers;

import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class RandomPlayer extends SampleGamer {

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine sm = getStateMachine();
		long start = System.currentTimeMillis();
		MachineState ms = getCurrentState();
		Role r = getRole();
		List<Move> possMoves = sm.getLegalMoves(ms, r);
		Random rg = new Random();
		int index = rg.nextInt(possMoves.size());
		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(possMoves, possMoves.get(index), stop - start));
		return possMoves.get(index);
	}

}
