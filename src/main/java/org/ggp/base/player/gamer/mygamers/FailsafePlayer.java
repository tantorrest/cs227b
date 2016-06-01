package org.ggp.base.player.gamer.mygamers;

import java.util.ArrayList;

import org.ggp.base.util.statemachine.FailsafeStateMachine;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.propnet.PropNetStateMachine;

public class FailsafePlayer extends StablePlayer {
	@Override
	public StateMachine getInitialStateMachine() {
		System.out.println("failsafe acquired");
		PropNetStateMachine pnsm = new PropNetStateMachine();
		pnsm.initialize(getMatch().getGame().getRules());
		return new FailsafeStateMachine(pnsm);
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		p("Metagaming Phase Optimized Propnet: " + getMatch().getMatchId());
		init();
		expand(root);
		long start = System.currentTimeMillis();
		finishBy = timeout - 5000;
		performMCTS(root);
		timeToDepthCharge = (System.currentTimeMillis() - start) / numDepthCharges;
		p("time to depth charge: " + timeToDepthCharge);
	}

	//@Override
	private void init() {
		game = getStateMachine();
		//game.initialize(getMatch().getGame().getRules());
		role = getRole();
		root = new MultiNode(getCurrentState(), null, null, 1, 0, true);
		bestPathReversed = new ArrayList<Move>();
		isFirstMove = true;
		isSinglePlayer = false;
		bestPathFound = false;
		stepAfterFoundBestMove = 0;
		prevNumMoves = 0;
		isSinglePlayer = (game.getRoles().size() == 1);
	}

}
