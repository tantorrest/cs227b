package org.ggp.base.player.gamer.mygamers;

import org.ggp.base.util.statemachine.FailsafeStateMachine;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.implementation.propnet.FactorPropNetStateMachine;
import org.ggp.base.util.statemachine.implementation.propnet.PropNetStateMachine;

public class FactorPlayer extends StablePlayer {
	@Override
	public StateMachine getInitialStateMachine(){
		FactorPropNetStateMachine sm =  new FactorPropNetStateMachine();
		sm.initialize(getMatch().getGame().getRules());
		boolean factorable = sm.markFactors();
		p("FACTORABLE? " + factorable);
		if (!factorable) {
			return new CachedStateMachine(new FailsafeStateMachine(new PropNetStateMachine()));
		} else {
			return new FailsafeStateMachine(sm);
		}
	}
	@Override
	public void p(String x){ System.out.println(x);}
}





