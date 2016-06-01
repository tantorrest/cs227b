package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.ArrayList;
<<<<<<< HEAD
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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

    private Map<Component, Set<Component>> preterminalPaths = new HashMap<Component,Set<Component>>();
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
		p(getPropNet().toString());
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

		}
		for (Set<Component> inpts : allSubgameInputs) {
			p("INPUTS FOR SUBGAME: " +  inpts);
		}
		Component terminal = (Component) getPropNet().getTerminalProposition();
		p("MARKING FACTORS DONE AND SEPARATED: #FACTORS = " + factoredComponents.size());
		Set<Component> preterminalInputs = new HashSet<Component>();
		Set<Component> gatesToAdd = new HashSet<Component>();
		if (terminal.getInputs().size() == 1) {
			getPreterminalInputs(terminal.getSingleInput(), preterminalInputs, gatesToAdd, factoredComponents);
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
			    	 if (subfactor.contains(i) && !subgameInput.isEmpty()) {
			    		 Set<Component> pathToTerminal = getPath(i);
			    		 if (pathToTerminal != null) subfactor.addAll(pathToTerminal);
							subgames.add(subfactor);
							totalSubgameInputs.add(subgameInput);
							combinedInputs.addAll(subgameInput);
							combinedFactors.addAll(subfactor);
							subgameToInputs.put(subfactor, subgameInput);
					}
			     }
//			     p("SUBGAMES: " + subgames);
//			     p("totalSubgameInputs: " + totalSubgameInputs);

			}

		} else {
			p("AWWW MULTIPLE INPUTS TO TERMINAL!!!");
		}
//		for(Set<Component> subgame : subgames){
//			Set<Component> needToAdd = new HashSet<Component>();
//			connectSubgameToTerminal(subgame, terminal, needToAdd);
//		}
//		for(Set<Component> subgame : subgames){
//			p("SUBGAMEEEE");
//			 StringBuilder sb = new StringBuilder();
//
//		        sb.append("digraph propNet\n{\n");
//		        for ( Component component : subgame )
//		        {
//		            sb.append("\t" + component.toString() + "\n");
//		        }
//		        sb.append("}");
//		        p(sb.toString());
//		}
		p("#subgames: " + subgames.size());
		p("#subgameInputs: " + totalSubgameInputs.size());
		combinedFactors.add(terminal);
		subgameToInputs.put(combinedFactors, combinedInputs);
		Iterator<HashSet<Component>> subgameIt = subgames.iterator();
		if (subgameIt.hasNext()) combinedFactors = subgameIt.next();
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





	private void connectSubgameToTerminal(Set<Component> subgame, Component cp, Set<Component> needToAdd) {
		for (Component child : cp.getInputs()) {
			if (subgame.contains(child)) {
				needToAdd.add(child);
				subgame.addAll(needToAdd);
				return;
			}

		}

		if (subgame.contains(cp)) {
			subgame.addAll(needToAdd);
			return;
		} else {
			for (Component child : cp.getInputs()) {
				Set<Component> keepAdding = new HashSet<Component>(needToAdd);
				keepAdding.add(cp);
				connectSubgameToTerminal(subgame, child, keepAdding);
			}
			return;
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
				p("BASE PROP!! " + prop.toString());
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
        	boolean inSubgame = inputInSubgame(p, combinedFactors);
//        	p("LEGAL PROP: " + (Component)p + "INPUT PROP: " + (Component)getPropNet().getLegalInputMap().get(p) +  " IN SUBGAME ? " + inSubgame);
        	if (p.getValueIsCorrect() ) {
        		if (p.getValue() && inSubgame) actions.add(getMoveFromProposition(p));
        	} else {
        		if (propmarkp(p) && inSubgame) actions.add(getMoveFromProposition(p));
        	}
        }
        return actions;
    }
    private boolean inFactoredComponents(Component singleInput, List<HashSet<Component>> factoredComponents) {
    	boolean in = false;

    	for (Set<Component> factor : factoredComponents) {
    		if (factor.contains(singleInput)) {
    			in = true;
//    			getPath(singleInput);
    			return in;
    		}
    	}

    	return in;
    }

    private Set<Component> getPath(Component inp) {

    	Queue queue = new LinkedList();
    	List<Component> way = new ArrayList<Component>();
    	way.add(inp);
		queue.add(way);
//		printNode(this.rootNode);
		inp.visited = true;
		while(!queue.isEmpty()) {
			List<Component> pth = (List<Component>)queue.remove();
			Component node = pth.get(pth.size() - 1);
			if (node ==  (Component) getPropNet().getTerminalProposition()){
				p("TERMINAL REACHED: " + pth.toString());
				Set<Component> path = new HashSet<Component>(pth);
				clearVisited(inp);
				return path;
			}
			Component child=null;
			while((child=getUnvisitedChildNode(node))!=null) {
				child.visited=true;
				List<Component> new_pth = new ArrayList<Component>(pth);
				new_pth.add(child);
				if (child ==  (Component) getPropNet().getTerminalProposition()) {

					p("REACHED TERMINAL!!");
				}
				queue.add(new_pth);
			}
		}
		return null;
//    	Component terminal = (Component) getPropNet().getTerminalProposition();
//    	if(terminal.equals(inp)) {
//    		return path;
//    	} else {
//    		for (Component out : inp.getOutputs()) {
//    			Set<Component> pth = new HashSet<Component>(path);
//    			pth.add(inp);
//    			return getPath(out, pth);
//    		}
//    		return null;
//    	}
    }

    private void clearVisited(Component inp) {
    	Queue queue = new LinkedList();
		queue.add(inp);
		inp.visited = false;
		while(!queue.isEmpty()) {
			Component node = (Component)queue.remove();
			Component child=null;
			while((child=getVisitedChildNode(node))!=null) {
				child.visited=false;
				queue.add(child);
			}
		}

	}





	private Component getVisitedChildNode(Component node) {
		for (Component child : node.getOutputs()) {
    		if (child.visited) return child;
    	}
		return null;
	}





	private Component getUnvisitedChildNode(Component node) {
		// TODO Auto-generated method stub
    	for (Component child : node.getOutputs()) {
    		if (!child.visited) return child;
    	}
		return null;
	}





	private void getPreterminalInputs(Component singleInput, Set<Component> preterminalInputs, Set<Component> gatesToAdd, List<HashSet<Component>> factoredComponents) {
		if(!(singleInput instanceof Not || singleInput instanceof Or || singleInput instanceof And)){

			if (inFactoredComponents(singleInput, factoredComponents)) {
				preterminalInputs.add(singleInput);
				return;
			} else {
//				Set<Component> needToAdd = new HashSet<Component>();
//	    		needToAdd.add(singleInput);
//	    		matchToSubgame(singleInput, needToAdd, factoredComponents);
				gatesToAdd.add(singleInput);
				for (Component in : singleInput.getInputs()){
					getPreterminalInputs(in, preterminalInputs, gatesToAdd, factoredComponents);
				}
			}
//    		preterminalInputs.add(singleInput);
			return;
//			return;

		} else {
			gatesToAdd.add(singleInput);
			for (Component in : singleInput.getInputs()){
				getPreterminalInputs(in, preterminalInputs, gatesToAdd, factoredComponents);
			}

		}

	}

	private void matchToSubgame(Component singleInput, Set<Component> needToAdd, List<HashSet<Component>> factoredComponents) {
		for (Component input : singleInput.getInputs()) {
			for (HashSet<Component> factor : factoredComponents) {
	    		if (factor.contains(singleInput)) {
	    			HashSet<Component> updated = factor;
	    			updated.addAll(needToAdd);
	    			factoredComponents.remove(factor);
	    			factoredComponents.add(updated);
	    			return;
	    		}
	    	}
			needToAdd.add(input);
			matchToSubgame(input, needToAdd, factoredComponents);
		}


	}

	private boolean inputInSubgame(Proposition p, Set<Component> subgame) {
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
=======
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Proposition;

public class FactorPropNetStateMachine extends SamplePropNetStateMachine {

	public List<List<Gdl>> independentFactor (){
		System.out.println("v2.0");
		List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());
		System.out.println("terminal:" + propositions.toString());
		List<Set<Component>> subGames = new ArrayList<Set<Component>>();
		List<List<Gdl>> subGameContents = new ArrayList<List<Gdl>>();
		int counter = 0;
		for(int i = 0; i < propositions.size(); i++){
			Proposition proposition = propositions.get(i);
			System.out.println("Prop: " + proposition);
			List<Component> terms = new ArrayList<Component>(proposition.getInputs());
			terms.addAll(proposition.getOutputs());
			System.out.println("Term: " + terms);
			List<Integer> subGameIndices = new ArrayList<Integer>();
			for (int j = 0; j < terms.size(); j++){
				Component term = terms.get(j);
				//System.out.println("Term: " + term);
				if (!(term instanceof Constant)){
					continue;
				}
				for (int k = 0; k < subGames.size(); k++){
					if (subGames.get(k).contains(term)){
						//System.out.println("Term in Question: " + term.toString());
						//System.out.println("Matching Set: " + subGames.get(k));
						subGameIndices.add(new Integer(k));
					}
				}
			}
			System.out.println("Matching subgames: " + subGameIndices.size());
			if (subGameIndices.size() == 0){
				System.out.println("New: " + proposition.toString());
				System.out.println("Old: " + subGames.toString());
				Set<Component> newSubGame = new HashSet<Component>(terms);
				for (int m = 0; m < terms.size(); m++){
					newSubGame.add(terms.get(m));
				}
				subGames.add(newSubGame);
				List<Gdl> newContents = new ArrayList<Gdl>();
				newContents.add(proposition.getName());
				subGameContents.add(newContents);
				counter++;
				if (counter >= 11){
					break;
				}
			}
			else{
				for (Integer index : subGameIndices){
					for (Component termToAdd : terms){
						subGames.get(index).add(termToAdd);
					}
					subGameContents.get(index).add(proposition.getName());
				}
			}
		}
		System.out.println("SubGames: " + subGameContents.toString());
		System.out.println(subGames.size());
		List<Set<Component>> finalSubGames = new ArrayList<Set<Component>>();
		List<List<Gdl>> finalSubGameContents = new ArrayList<List<Gdl>>();
		for (int i  = 0; i < subGames.size(); i++){
			Set<Component> cSet = subGames.get(i);
			for (int j  = 0; j < subGames.size(); j++){
				List<Component> cSet2 = new ArrayList<Component>(subGames.get(j));
				for (int k = 0; k < cSet2.size(); k++){
					Component c = cSet2.get(k);
					if (cSet.contains(c)){
						cSet.addAll(cSet2);
						subGames.remove(j);
						i = 0;
						j = 0;
						k = 0;
					}
				}

			}
		}
		for (List<Gdl> subgame : subGameContents){
			System.out.println(subgame.toString());
		}
		return subGameContents;
	}
}
>>>>>>> optimized-propnet-oluwasanya
