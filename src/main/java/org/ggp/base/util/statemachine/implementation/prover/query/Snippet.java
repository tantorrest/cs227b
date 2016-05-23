package org.ggp.base.util.statemachine.implementation.prover.query;

public class Snippet {
	MachineState s1 = propnetStateMachine.getInitialState();
	MachineState s2 = proverStateMachine.getInitialState();

	if (!s1.equals(s2)) {
		p(diff(s1.getContents(), s2.getContents()).toString());
		p("");
	}
	return s1;
}