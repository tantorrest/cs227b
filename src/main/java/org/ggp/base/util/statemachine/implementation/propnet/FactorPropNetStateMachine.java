package org.ggp.base.util.statemachine.implementation.propnet;

import org.ggp.base.util.propnet.architecture.components.Proposition;

public class FactorPropNetStateMachine extends SamplePropNetStateMachine {

	public void independentFactor (){
		Proposition terminal = propNet.getTerminalProposition();
		System.out.println("terminal:" + terminal.toString());
	}

}
