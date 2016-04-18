package org.ggp.base.player.gamer.mygamers;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class LegalPlayer extends SampleGamer {

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine sm = getStateMachine();
		long start = System.currentTimeMillis();
		MachineState ms = getCurrentState();
		Role r = getRole();
		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(sm.findLegals(r, ms), sm.findLegalx(r, ms), stop - start));
		return sm.findLegalx(r, ms);
	}

}
