package org.ggp.base.player.gamer.mygamers;

import java.util.Set;

import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.implementation.propnet.FactorPropNetStateMachine;

public class FactorPlayer extends PropnetPlayer {
	@Override
	public StateMachine getInitialStateMachine(){
		FactorPropNetStateMachine sm =  new FactorPropNetStateMachine();
    	sm.initialize(getMatch().getGame().getRules());
    	//List<Gdl> newRules = sm.independentFactor();
    	Set<Proposition> usedProps = sm.independentFactor();
    	//System.out.println(newRules.toString());
    	//SamplePropNetStateMachine fm = new SamplePropNetStateMachine();
    	//fm.initialize(newRules);-
    	for (Proposition prop = sm.propositions){

    	}
    	return sm;
	}
}




