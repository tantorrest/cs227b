package org.ggp.base.player.gamer.mygamers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.implementation.propnet.FactorPropNetStateMachine;

public class FactorPlayer extends PropnetPlayer {
	@Override
	public StateMachine getInitialStateMachine(){
		FactorPropNetStateMachine sm =  new FactorPropNetStateMachine();
    	sm.initialize(getMatch().getGame().getRules());
    	System.out.println(sm.getPropNet().getPropositions());
    	//System.out.println("Old rules: " + getMatch().getGame().getRules());
    	//List<Gdl> newRules = sm.independentFactor();
    	//System.out.println("new rules: " + newRules.toString());
    	Set<Component> usedProps = sm.independentFactor();
    	//System.out.println(newRules.toString());
    	//SamplePropNetStateMachine fm = new SamplePropNetStateMachine();
    	//fm.initialize(newRules);
    	System.out.println("usedProps " + usedProps.toString());
    	List<Proposition> propList = new ArrayList<Proposition>(sm.getPropNet().getPropositions());
    	System.out.println("Starting pruning");
    	System.out.println(usedProps.toString());
    	for (int i = 0; i < propList.size(); i++){
    		Proposition p = propList.get(i);
    		if (!sm.getPropNet().getPropositions().contains(p)){
    			continue;
    		}
    		boolean needToDelete = true;
			/*for (Component input : p.getInputs()){
				System.out.println("input: " + input);
				if (usedProps.contains(input)){
					System.out.println("No need to delete");
					needToDelete = false;
				}
			}
			for (Component output : p.getOutputs()){
				System.out.println("output: " + output);
				if (usedProps.contains(output)){
					System.out.println("No need to delete");
					needToDelete = false;
				}
			}*/
    		if (usedProps.contains(p)){
    			needToDelete = false;
    		}
    		if (needToDelete == true && !p.equals(sm.getPropNet().getInitProposition()) && !p.equals(sm.getPropNet().getTerminalProposition())){
    			sm.getPropNet().removeComponent(p);
    		}
    	}
    	for (Component used : usedProps){
    		if (!sm.getPropNet().getPropositions().contains(used)){
    			System.out.println("Mismatch type 1: " + used);
    		}
    	}
    	for (Proposition prop : sm.getPropNet().getPropositions()){
    		if (!usedProps.contains(prop)){
    			System.out.println("Mismatch type 2: " + prop);
    		}
    	}
    	System.out.println("Props after : " + sm.getPropNet().getPropositions());
    	return sm;
	}
}
