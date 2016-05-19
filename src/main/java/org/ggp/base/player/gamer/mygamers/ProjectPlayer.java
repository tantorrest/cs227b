package org.ggp.base.player.gamer.mygamers;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.StateMachine;

public class ProjectPlayer extends OptimizedPropnetPlayer {

	@Override
	public void stateMachineStop(){
		MachineState ms = getCurrentState();
		StateMachine sm = getStateMachine();
		System.out.println("Final Propositions: " + ms.getContents());
	}
}
