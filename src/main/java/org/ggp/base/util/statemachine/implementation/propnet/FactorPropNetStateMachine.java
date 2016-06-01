package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.ArrayList;
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
	public boolean markFactors() {
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
			if (base == null){
				p("BASE NULL");
				break;
			} else {
				p("BASE NOT NULL");
			}

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
			p("PRETERMINAL INPUTS: " + preterminalInputs.size());
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
			    	 if (subfactor.contains(i) && !subgameInput.isEmpty()) {
			    		 Set<Component> pathToTerminal = getPath(i);
			    		 if (pathToTerminal != null) {
			    			subfactor.addAll(pathToTerminal);
							subgames.add(subfactor);
							totalSubgameInputs.add(subgameInput);
							combinedInputs.addAll(subgameInput);
							combinedFactors.addAll(subfactor);
							subgameToInputs.put(subfactor, subgameInput);
							break;
			    		 }
					}
			     }
			}

		} else {
			p("AWWW MULTIPLE INPUTS TO TERMINAL!!!");
		}

		p("#subgames: " + subgames.size());
		p("#subgameInputs: " + totalSubgameInputs.size());
		combinedFactors.add(terminal);
		subgameToInputs.put(combinedFactors, combinedInputs);
		Iterator<HashSet<Component>> subgameIt = subgames.iterator();
		if ((subgames.size() != totalSubgameInputs.size()) || subgames.isEmpty() || totalSubgameInputs.isEmpty()){
			return false;
		}
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
		return true;
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
			getDependencies(input, factor, numComponentsMarked, subgameInputs);
		}
	}



	private boolean isInputProp(Component base) {
		return inputProps.contains(base);
	}

	/**
     * Computes the legal moves for role in state.
     */
    @Override
    public List<Move> getLegalMoves(MachineState state, Role role)
            throws MoveDefinitionException {

        markbases(state.getContents(), getPropNet());
        Set<Proposition> legals = getPropNet().getLegalPropositions().get(role);
        List<Move> actions = new ArrayList<Move>();
        for (Proposition p : legals) {
        	boolean inSubgame = inputInSubgame(p, combinedFactors);
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
		inp.visited = true;
		while(!queue.isEmpty()) {
			List<Component> pth = (List<Component>)queue.remove();
			Component node = pth.get(pth.size() - 1);
			if (node ==  (Component) getPropNet().getTerminalProposition()){
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

				}
				queue.add(new_pth);
			}
		}
		return null;
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
    private void clearVisitedInput(Component inp) {
    	Queue queue = new LinkedList();
		queue.add(inp);
		inp.visited = false;
		while(!queue.isEmpty()) {
			Component node = (Component)queue.remove();
			Component child=null;
			while((child=getVisitedInputNode(node))!=null) {
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
	private Component getUnvisitedInputNode(Component node) {
		// TODO Auto-generated method stub
    	for (Component child : node.getInputs()) {
    		if (!child.visited) return child;
    	}
		return null;
	}

	private Component getVisitedInputNode(Component node) {
		for (Component child : node.getInputs()) {
    		if (child.visited) return child;
    	}
		return null;
	}


	private void getPreterminalInputs(Component singleInput, Set<Component> preterminalInputs, Set<Component> gatesToAdd, List<HashSet<Component>> factoredComponents) {
		Queue queue = new LinkedList();
		queue.add(singleInput);
		singleInput.visited = true;
		while(!queue.isEmpty()) {
			Component node = (Component)queue.remove();
			Component child=null;
			while((child=getUnvisitedInputNode(node))!=null) {
				child.visited=true;
				if(!(child instanceof Not || child instanceof Or || child instanceof And)){

					if (inFactoredComponents(child, factoredComponents)) {
						preterminalInputs.add(child);
					} else {
						gatesToAdd.add(child);
						queue.add(child);
					}


				} else {
					gatesToAdd.add(child);
					queue.add(child);
				}
			}
		}
		// Clear visited property of nodes
		clearVisitedInput(singleInput);
		return;

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
		Map<Proposition, Proposition> legalInputMap = getPropNet().getLegalInputMap();
		Proposition input = legalInputMap.get(p);


		return subgameContainsInput(subgameInputs, input);
}

	private boolean subgameContainsInput(HashSet<Component> subgameInputs, Proposition input) {
		for (Component cp : subgameInputs) {
			String compName = ((Proposition) cp).getName().toString();
			if (compName.equals(input.getName().toString())) return true;
		}
		return false;
	}

	public void p(String x){ System.out.println(x);}
}