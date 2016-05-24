package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;

public class FactorPropNetStateMachine extends SamplePropNetStateMachine {



	/*public List<List<Gdl>> independentFactor (){
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
	}*/

//	int numComponentsMarked = 0; // just something I put to keep track of whether or not we have marked every node

	// this function just marks the factors
	public Set<Component> markFactors() {
		p("PRINTING WHOLE PROPNET");
		p(getPropNet().toString());
		int[] numComponentsMarked = { 0 };
		List<HashSet<Component>> factoredComponents = new ArrayList<HashSet<Component>>();
		p(" " + getPropNet().getComponents().size());
		while (numComponentsMarked[0] < getPropNet().getComponents().size()) { // keep factoring until we have marked all the components in the propnet
			HashSet<Component> factor = new HashSet<Component>();
			Proposition base = getUnmarkedBase(factoredComponents); // start with one base proposition that hasn't been marked yet
			// get a new sub-factor
			if (base == null) break;

			getDependencies(base, factor, numComponentsMarked);
			// add a new sub-factor
			factoredComponents.add(factor);
		}
		Component terminal = (Component) getPropNet().getTerminalProposition();
		p("MARKING FACTORS DONE AND SEPARATED: #FACTORS = " + factoredComponents.size());
		Set<Component> preterminalInputs = new HashSet<Component>();
		Set<Component> gatesToAdd = new HashSet<Component>();
		if (terminal.getInputs().size() == 1) {
			getPreterminalInputs(terminal.getSingleInput(), preterminalInputs, gatesToAdd);
		} else {
			p("AWWW MULTIPLE INPUTS TO TERMINAL!!!");
		}



		Set<Component> combinedFactors = new HashSet<Component>();

		if (terminal.getInputs().size() == 1) {
//			Component inp = terminal.getSingleInput();
			combinedFactors.addAll(gatesToAdd);
			for (Component i : preterminalInputs) {
				p("INPUTS TO PENULTIMATE #: " + i);
				for (HashSet<Component> subfactor : factoredComponents){
					if (subfactor.contains(i)) {
						combinedFactors.addAll(subfactor);
					}
				}
			}
		} else {
			p("AWWW MULTIPLE INPUTS TO TERMINAL!!!");
		}
		combinedFactors.add(terminal);
		p("COMBINED FACTORING DONE!!!");
//		StringBuilder sb = new StringBuilder();
//		sb.append("digraph factoredPropNet\n{\n");
//		for (Component cp : combinedFactors) {
//
//	            sb.append("\t" + cp.toString() + "\n");
//
//		}
//		 sb.append("}");
//		 p(sb.toString());
//		 p("COMBINED FACTORING DONE!!!");
//
//
//		int in = 0;
//		for (HashSet<Component> subfactor : factoredComponents){
//			in++;
//			StringBuilder s = new StringBuilder();
//			p("SUBFACTOR: " + in);
//			s.append("digraph propNet\n{\n");
//			for (Component cp : subfactor) {
//
//		            s.append("\t" + cp.toString() + "\n");
//
//
//			}
//			 s.append("}");
//			 p(s.toString());
//		}
		// we have separated the propnet into the various factors
		// most of what is left is being able to explore each of these components
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
				p("BASE PROP!!");
				p(prop.toString());
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
	private void getDependencies(Component base, Set<Component> factor, int[] numComponentsMarked) {
		if (factor.contains(base)) return;
		factor.add(base);
		increment(numComponentsMarked);
		for (Component input : base.getInputs()) { // recursively add all the dependencies
//			factor.add(input);
			getDependencies(input, factor, numComponentsMarked);
		}
	}

	public void backtrackPrint(Component cp, int increment, int inputindex){
		if (increment > 10) return;
		if (cp.getInputs().size() == 1 && cp.getSingleInput() instanceof Transition) { // base
			   p("Level: " + increment + " InputIndex: " + inputindex + " base");
				p(cp.toString());
		   }else {
			   p("Level: " + increment + " InputIndex: " + inputindex);
			   p(cp.toString());
		   }
		p("NUM CHILDREN: " + cp.getInputs().size());
		int i = 0;
		for (Component c : cp.getInputs()) {
			i++;
			backtrackPrint(c, increment+1, i);
		}
		return;
	}

	public Set<Component> componentFactoring(){
		Set<Component> cmpnts = new HashSet<Component>();
		Proposition terminal = getPropNet().getTerminalProposition();
		cmpnts.add((Component)terminal);
		p("STARTING RECURSIVE COMPONENT FACTORING");
		cmpnts = recursiveComponentFactoring((Component) terminal, cmpnts, 0);
		p("DONE RECURSIVE COMPONENT FACTORING");
		return cmpnts;
	}

	public Set<Component> recursiveComponentFactoring(Component cp, Set<Component> used, int increment){
		if (increment > 50) return used;
		if (cp.getInputs().size() == 0) {
			return used;
		}
		for (Component c: cp.getInputs()) {
			p(c.toString());
			if (!used.contains(c)) used.add(c);
		}
		for (Component c: cp.getInputs()) {
			Set<Component> branch_cmpnts = new HashSet<Component>();
			used.addAll(recursiveComponentFactoring(c, branch_cmpnts, increment+1));
		}
		return used;
	}

	public Set<Proposition> independentFactor(){
		p("{PROPNET}");
		p(getPropNet().toString());
		Proposition terminal = getPropNet().getTerminalProposition();
		Set<Proposition> usedProps = new HashSet<Proposition>();

		usedProps.add(terminal);
		System.out.println("starting recursive factoring");
		p("TERMINAL" + terminal.toString());
		p(terminal.getInputs().size());
		Map<Proposition, Proposition> legalInputMap = getPropNet().getLegalInputMap();
		System.out.println("LEGAL INPUT MAP:");
		System.out.println(legalInputMap);
		p("BACKTRACKPRINTING");
		backtrackPrint((Component)terminal, 0, 0);
		p("BRACKTRACKPRINTING DONE");
		for (Component cp : terminal.getInputs()) {
			p(cp.toString());
		}

		p(terminal.getInputs().toString());
		p(terminal.getOutputs().toString());
		usedProps = recursiveFactor(terminal, usedProps);

		List<Gdl> factored = new ArrayList<Gdl>();
		for (Proposition p : usedProps){
			factored.add(p.getName());
		}
		//return factored;
		return usedProps;
	}

	private void p(int x) {
		System.out.println(x);
	}

	public Set<Proposition> recursiveFactor(Proposition prop, Set<Proposition> seen){
		if (prop.equals(getPropNet().getInitProposition())){
			if (!seen.contains(prop)){
				seen.add(prop);
			}

			System.out.println("done");
			return seen;
		}
		Map<Proposition, Proposition> legalInputMap = getPropNet().getLegalInputMap();
		System.out.println("LEGAL INPUT MAP:");
		System.out.println(legalInputMap);
		for (Proposition input : legalInputMap.keySet()){
			if (prop.equals(legalInputMap.get(input))){
				if (!seen.contains(input)){
					seen.add(input);
					seen = recursiveFactor(input, seen);
				}
			}
		}
		return seen;
	}


	public void p(String x){ System.out.println(x);}
}
