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
		if (!proverStateMachine.equals(propnetStateMachine)) {
			p("failed initialization");
		}
	}

	@Override
	public int getGoal(MachineState state, Role role) throws GoalDefinitionException {
		int g1 = propnetStateMachine.getGoal(state, role);
		//    	p("stt goal: " + state.getContents());
		int g2 = proverStateMachine.getGoal(state, role);
		if (g1 != g2) {
			p("different goals");
			p("propnet goal: " + g1);
			p("prover goal : " + g2);
			p("");
		}
		return g1;
	}

	private void p(String word) { System.out.println(word); }

	@Override
	public MachineState getInitialState() {
		MachineState s1 = propnetStateMachine.getInitialState();
		MachineState s2 = proverStateMachine.getInitialState();

		if (!s1.equals(s2)) {
			p("different initial states");
			p(diff(s1.getContents(), s2.getContents()).toString());
			p("");
		}
		return s1;
	}

	@Override
	public List<Move> findActions(Role role) throws MoveDefinitionException {
		int s1 = propnetStateMachine.findActions(role).size();
		int s2 = proverStateMachine.findActions(role).size();

		if (s1 != s2) {
			p("different find actions");
			p("propnet: " + s1);
			p("prover  : " + s2);
			p("");
		}
		return propnetStateMachine.findActions(role);
	}

	@Override
	public List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException {
		List<Move> m1 = propnetStateMachine.getLegalMoves(state, role);
		List<Move> m2 = proverStateMachine.getLegalMoves(state, role);;

		if (m1.size() != m2.size()) {
			p("diff legal moves");
			p("propnet : " + m1.toString());
			p("prover  : " + m2.toString());
			p("");
		}
		return m1;
	}

	@Override
	public MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException {

		MachineState ms1 = propnetStateMachine.getNextState(state.clone(), moves);
		MachineState ms2 = proverStateMachine.getNextState(state, moves);
		if (!ms1.equals(ms2)) {
			p("different get next states");
			Set<GdlSentence> propnet = ms1.getContents();
			Set<GdlSentence> prover = ms2.getContents();
			Set<GdlSentence> difference = diff(propnet, prover);
			for (GdlSentence gs : difference) {
				System.out.print(gs + " ");
			}
			System.out.println(" ");
		}
		return ms1;
	}

	@Override
	public boolean isTerminal(MachineState state) {
		boolean t1 = propnetStateMachine.isTerminal(state);
		boolean t2 = proverStateMachine.isTerminal(state);
		if (t1 != t2) {
			p("different isterminal");
			p("propnet :" + t1);
			p("prover  :" + t2);
		}
		return t1;
	}

	@Override
	public List<Role> getRoles() {
		List<Role> r1 = propnetStateMachine.getRoles();
		List<Role> r2 = proverStateMachine.getRoles();
		if (!r1.equals(r2)) {
			p("diff roles");
		}
		return r1;
	}

	public static <T> Set<T> diff(final Set<? extends T> s1, final Set<? extends T> s2) {
		Set<T> symmetricDiff = new HashSet<T>(s1);
		symmetricDiff.addAll(s2);
		Set<T> tmp = new HashSet<T>(s1);
		tmp.retainAll(s2);
		symmetricDiff.removeAll(tmp);
		return symmetricDiff;
	}

	@Override
	public MachineState performPropNetDepthCharge(MachineState state,
			int[] theDepth) throws TransitionDefinitionException,
			MoveDefinitionException {
		MachineState m1 = propnetStateMachine.performPropNetDepthCharge(state, null);
		MachineState m2 = proverStateMachine.performPropNetDepthCharge(state, null);
		return m1;
	}

	@Override
	public List<Move> getBestMoves() {
		// TODO Auto-generated method stub
		return propnetStateMachine.getBestMoves();
	}
}
