package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;

public class FactorPropNetStateMachine extends PropNetStateMachine {

	private HashSet<Component> combinedFactors = new HashSet<Component>();
	private Set<HashSet<Component>> subgames = new HashSet<HashSet<Component>>();
	private Set<Component> inputProps = new HashSet<Component>();
	private Set<HashSet<Component>> totalSubgameInputs = new HashSet<HashSet<Component>>();
	private HashSet<Component> combinedInputs = new HashSet<Component>();
	private Map<HashSet<Component>, HashSet<Component>> subgameToInputs = new HashMap<HashSet<Component>, HashSet<Component>>();


//	int numComponentsMarked = 0; // just something I put to keep track of whether or not we have marked every node

	// this function just marks the factors
	public Set<Component> markFactors() {
		Map<GdlSentence, Proposition> inputs = getPropNet().getInputPropositions();
		 for (GdlSentence gs : inputs.keySet()) {
			 Proposition input = inputs.get(gs);
			 inputProps.add(input);
		 }
//		p("PRINTING WHOLE PROPNET");
//		p(getPropNet().toString());
		int[] numComponentsMarked = { 0 };
		List<HashSet<Component>> factoredComponents = new ArrayList<HashSet<Component>>();
		List<HashSet<Component>> allSubgameInputs = new ArrayList<HashSet<Component>>();

		p(" " + getPropNet().getComponents().size());
		while (numComponentsMarked[0] < getPropNet().getComponents().size()) { // keep factoring until we have marked all the components in the propnet
			HashSet<Component> factor = new HashSet<Component>();
			HashSet<Component> subgameInputs = new HashSet<Component>();
			Proposition base = getUnmarkedBase(factoredComponents); // start with one base proposition that hasn't been marked yet
			// get a new sub-factor
			if (base == null) break;

			getDependencies(base, factor, numComponentsMarked, subgameInputs);
			// add a new sub-factor
			factoredComponents.add(factor);
			allSubgameInputs.add(subgameInputs);
			subgameToInputs.put(factor, subgameInputs);
		}
		Component terminal = (Component) getPropNet().getTerminalProposition();
//		p("MARKING FACTORS DONE AND SEPARATED: #FACTORS = " + factoredComponents.size());
		Set<Component> preterminalInputs = new HashSet<Component>();
		Set<Component> gatesToAdd = new HashSet<Component>();
		if (terminal.getInputs().size() == 1) {
			getPreterminalInputs(terminal.getSingleInput(), preterminalInputs, gatesToAdd);
		} else {
			p("AWWW MULTIPLE INPUTS TO TERMINAL!!!");
		}





		if (terminal.getInputs().size() == 1) {
//			Component inp = terminal.getSingleInput();
			combinedFactors.addAll(gatesToAdd);
			for (Component i : preterminalInputs) {
				p("INPUTS TO PENULTIMATE #: " + i);
				Iterator<HashSet<Component>> factoredComponentsIt = factoredComponents.iterator();
				Iterator<HashSet<Component>> subgameInputsIt = allSubgameInputs.iterator();
			     while(factoredComponentsIt.hasNext() && subgameInputsIt.hasNext()){
			    	 HashSet<Component> subfactor = factoredComponentsIt.next();
			    	 HashSet<Component> subgameInput = subgameInputsIt.next();
//			    	 p("SUBGAME: " + subfactor);
//			    	 p("SUBGAME INPUT: " + subgameInput);
			    	 if (subfactor.contains(i)) {
							subgames.add(subfactor);
							totalSubgameInputs.add(subgameInput);
							combinedInputs.addAll(subgameInput);
							combinedFactors.addAll(subfactor);
					}
			     }
//			     p("SUBGAMES: " + subgames);
//			     p("totalSubgameInputs: " + totalSubgameInputs);

			}

		} else {
			p("AWWW MULTIPLE INPUTS TO TERMINAL!!!");
		}
		p("#subgames: " + subgames.size());
		p("#subgameInputs: " + totalSubgameInputs.size());
		combinedFactors.add(terminal);
		subgameToInputs.put(combinedFactors, combinedInputs);
		p("COMBINED FACTORING DONE!!!");
		StringBuilder sb = new StringBuilder();
		sb.append("digraph factoredPropNet\n{\n");
		for (Component cp : combinedFactors) {

	            sb.append("\t" + cp.toString() + "\n");

		}
		 sb.append("}");
		 p(sb.toString());
		 p("COMBINED FACTORING DONE!!!");
		return combinedFactors;
	}

	private void getPreterminalInputs(Component singleInput, Set<Component> preterminalInputs, Set<Component> gatesToAdd) {
		if(!(singleInput instanceof Not || singleInput instanceof Or || singleInput instanceof And)){
			preterminalInputs.add(singleInput);
			return;
		} else {
			gatesToAdd.add(singleInput);
			for (Component in : singleInput.getInputs()){
				getPreterminalInputs(in, preterminalInputs, gatesToAdd);
			}

		}

	}

	private Proposition getUnmarkedBase(List<HashSet<Component>> factoredComponents) {
		Map<GdlSentence, Proposition> bases = getPropNet().getBasePropositions();
		for (GdlSentence gs : bases.keySet()){
			Proposition prop = bases.get(gs);
			boolean marked = false;
			for (HashSet<Component> subfactor : factoredComponents){
				if (subfactor.contains(prop)) {
					marked = true;
				}
			}
			if (!marked){
//				p("BASE PROP!!");
//				p(prop.toString());
				return prop;
			}
		}
		return null;
	}

	public static void increment(int[] array)
	{
	   array[0] = array[0] + 1;
	}

	// this is not perfect
	// it assumes that no two potential factors can share the same component
	private void getDependencies(Component base, Set<Component> factor, int[] numComponentsMarked, Set<Component> subgameInputs) {
		if (factor.contains(base)) return;
		factor.add(base);
		increment(numComponentsMarked);
		if (isInputProp(base)) {
			subgameInputs.add(base);
		}
		for (Component input : base.getInputs()) { // recursively add all the dependencies
//			factor.add(input);
			getDependencies(input, factor, numComponentsMarked, subgameInputs);
		}
	}



	private boolean isInputProp(Component base) {
//		 p("COMPONENT: " + base);
//		 p("INPUT ? " + inputProps.contains(base));
		return inputProps.contains(base);
	}

	/**
     * Computes the legal moves for role in state.
     */
    @Override
    public List<Move> getLegalMoves(MachineState state, Role role)
            throws MoveDefinitionException {

        markbases(state, getPropNet());
        Set<Proposition> legals = getPropNet().getLegalPropositions().get(role);
        List<Move> actions = new ArrayList<Move>();
        for (Proposition p : legals) {
        	boolean inSubgame = inSubgame(p, combinedFactors);
//        	p("LEGAL PROP: " + (Component)p + "INPUT PROP: " + (Component)getPropNet().getLegalInputMap().get(p) +  " IN SUBGAME ? " + inSubgame);
        	if (p.getValueIsCorrect() ) {
        		if (p.getValue() && inSubgame) actions.add(getMoveFromProposition(p));
        	} else {
        		if (propmarkp(p) && inSubgame) actions.add(getMoveFromProposition(p));
        	}
        }
        return actions;
    }



	private boolean inSubgame(Proposition p, Set<Component> subgame) {
		HashSet<Component> subgameInputs = subgameToInputs.get(subgame);
//		p("SUBGAMEINPUTS : " + subgameInputs);
		Map<Proposition, Proposition> legalInputMap = getPropNet().getLegalInputMap();
		Proposition input = legalInputMap.get(p);
//		p(subgameContainsInput(subgameInputs, input) + "INPUT COMPONENT: " + input);

		return subgameContainsInput(subgameInputs, input);
}

	private boolean subgameContainsInput(HashSet<Component> subgameInputs, Proposition input) {
		for (Component cp : subgameInputs) {
			String compName = ((Proposition) cp).getName().toString();
//			p("INPUT NAME IN subgameInputs: " + compName + " input name in input: " + input.getName().toString());
			if (compName.equals(input.getName().toString())) return true;
		}
		return false;
	}

	public void p(String x){ System.out.println(x);}
}
