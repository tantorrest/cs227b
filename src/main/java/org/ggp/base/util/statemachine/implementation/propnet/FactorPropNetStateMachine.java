package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Constant;
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

	public Set<Component> independentFactor(){
		Proposition terminal = getPropNet().getTerminalProposition();
		Set<Component> usedProps = new HashSet<Component>();
		usedProps.add(terminal);
		usedProps = backtrack(terminal, usedProps);
		List<Gdl> factored = new ArrayList<Gdl>();
		for (Component p : usedProps){
			//factored.add(p.getName());
		}
		//System.out.println(factored);
		//return factored;
		return usedProps;
	}

	public Set<Proposition> recursiveFactor(Proposition prop, Set<Proposition> seen){
		if (prop.equals(getPropNet().getInitProposition())){
			System.out.println("done");
			return seen;
		}
		/*Map<Proposition, Proposition> legalInputMap = getPropNet().getLegalInputMap();
		System.out.println("Lim: ");
		for (Proposition key :legalInputMap.keySet()){
			System.out.println("key " + key);
			System.out.println("value : " + legalInputMap.get(key).toString());
		}
		for (Proposition input : legalInputMap.keySet()){
			if (prop.equals(legalInputMap.get(input))){
				if (!seen.contains(input)){
					seen.add(input);
					seen = recursiveFactor(input, seen);
				}
			}
		}*/
		return seen;
	}

	public Set<Component> backtrack (Component cp, Set<Component> seen) {
	   cp.setValueIsCorrect(true);// optimized
	   if (cp.equals(getPropNet().getInitProposition())){
		   return seen;
	   }
	   if (cp.getInputs().size() == 1 && cp.getSingleInput() instanceof Transition) { // base
//			   p("base");
		   if (!seen.contains(cp)){
				seen.add(cp);
			}
			//seen = backtrack(cp, seen);
			return seen;
	   } else if (cp instanceof Not) { // negation
		   return negationBacktrack(cp, seen);
	   } else if (cp instanceof Or) { // disjunction
		   return disjunctionBacktrack(cp, seen);
	   } else if (cp instanceof And) { // conjunction
		   return conjunctionBacktrack(cp, seen);
	   } else if (cp instanceof Constant) { // constant
//			   p("constant");
		   if (!seen.contains(cp)){
				seen.add(cp);
			}
			//seen = backtrack(cp, seen);
			return seen;
	   } else if (((Proposition) cp).getName().getName().getValue().equals("does")) { // input
//			   p("input");
		   if (!seen.contains(cp)){
				seen.add(cp);
			}
			//seen = backtrack(cp, seen);
			return seen;
	   } else if((((Proposition) cp).getName().getName().getValue().toUpperCase().equals("INIT"))) { // init
//			   p("init");
		   if (!seen.contains(cp)){
				seen.add(cp);
			}
			//seen = backtrack(cp, seen);
			return seen;
	   }
	   else if (cp.getInputs().size() == 1) { // view
//			   p("view");
		   if (!seen.contains(cp)){
			   seen.add(cp);
		   }
		   seen = backtrack(cp.getSingleInput(), seen);
		   return seen;
	   } else {
//			   p("false");
		   return seen;
	   }
	}

	public Set<Component> negationBacktrack (Component cp, Set<Component> seen) {
		if (!seen.contains(cp)){
			seen.add(cp);
		}
		seen = backtrack(cp, seen);
		return backtrack(cp.getSingleInput(), seen);
	}

	private Set<Component> conjunctionBacktrack (Component cp, Set<Component> seen) {
		Set<Component> sources = cp.getInputs();
		for (Component source : sources) {
			if (!seen.contains(source)){
				seen.add(source);
			}
			seen = backtrack(source, seen);
		}
		return seen;
   }

	private Set<Component> disjunctionBacktrack (Component cp, Set<Component> seen) {
		Set<Component> sources = cp.getInputs();
		for (Component source : sources) {
			if (!seen.contains(source)){
				seen.add(source);
			}
			seen = backtrack(source, seen);
		}
		return seen;
	}

}
