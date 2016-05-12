package org.ggp.base.util.statemachine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class DualStateMachine extends StateMachine {
	private StateMachine proverStateMachine;
	private StateMachine propnetStateMachine;

    public DualStateMachine(StateMachine prover, StateMachine propnet) {
        proverStateMachine = prover;
        propnetStateMachine = propnet;
    }

    @Override
    public synchronized void initialize(List<Gdl> description) {
        proverStateMachine.initialize(description);
        propnetStateMachine.initialize(description);
    }

    @Override
    public int getGoal(MachineState state, Role role) throws GoalDefinitionException {
//    	p("Goal for propnet: " + propnetStateMachine.getGoal(state, role));
//    	p("Goal for prover : " + proverStateMachine.getGoal(state, role));
    	return proverStateMachine.getGoal(state, role);
    }

    private void p(String word) { System.out.println(word); }

    @Override
    public MachineState getInitialState() {
    	return proverStateMachine.getInitialState();
    }

    @Override
    public List<Move> findActions(Role role) throws MoveDefinitionException {
//    	p("Find Actions Size Propnet: " + propnetStateMachine.findActions(role).size());
//    	p("Find Actions Size Prover : " + proverStateMachine.findActions(role).size());
        return proverStateMachine.findActions(role);
    }

    @Override
    public List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException {
//    	p("Get Legal Moves Size Propnet: " + propnetStateMachine.getLegalMoves(state, role).size());
//    	p("Get Legal Moves Size Prover : " + proverStateMachine.getLegalMoves(state, role).size());
        return proverStateMachine.getLegalMoves(state, role);
    }

    @Override
    public MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException {
//    	p("Get Next State Size Propnet: " + propnetStateMachine.getNextState(state, moves).getContents().size());
//    	p("Get Next State Size Prover : " + proverStateMachine.getNextState(state, moves).getContents().size());
    	MachineState ms1 = propnetStateMachine.getNextState(state, moves);
    	MachineState ms2 = proverStateMachine.getNextState(state, moves);
    	Set<GdlSentence> propnet = ms1.getContents();
    	Set<GdlSentence> prover = ms2.getContents();
    	Set<GdlSentence> difference = diff(propnet, prover);
    	for (GdlSentence gs : difference) {
    		System.out.print(gs + " ");
    	}
    	System.out.println(" ");
    	return ms2;
    }

	@Override
	public boolean isTerminal(MachineState state) {
//		p("Is Terminal Pronent: " + propnetStateMachine.isTerminal(state));
//    	p("Is Terminal Prover : " + proverStateMachine.isTerminal(state));
        return proverStateMachine.isTerminal(state);
    }

	@Override
	public List<Role> getRoles() {
//		p("Get Roles Size Pronent: " + propnetStateMachine.getRoles());
//    	p("Get Roles Size Prover : " + proverStateMachine.getRoles());
        return proverStateMachine.getRoles();
	}

	public static <T> Set<T> diff(final Set<? extends T> s1, final Set<? extends T> s2) {
	    Set<T> symmetricDiff = new HashSet<T>(s1);
	    symmetricDiff.addAll(s2);
	    Set<T> tmp = new HashSet<T>(s1);
	    tmp.retainAll(s2);
	    symmetricDiff.removeAll(tmp);
	    return symmetricDiff;
	}
}
