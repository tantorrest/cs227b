package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.components.Proposition;

public class FactorPropNetStateMachine extends SamplePropNetStateMachine {

	public List<List<Gdl>> independentFactor (){
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
			List<Integer> subGameIndices = new ArrayList<Integer>();
			for (int j = 0; j < terms.size(); j++){
				Component term = terms.get(j);
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
		return subGameContents;
	}

}
