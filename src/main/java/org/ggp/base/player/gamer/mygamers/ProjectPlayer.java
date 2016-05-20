package org.ggp.base.player.gamer.mygamers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.propnet.SamplePropNetStateMachine;

public class ProjectPlayer extends OptimizedPropnetPlayer {

	private List<List<Integer>> dataset;

	@Override
    public void stateMachineMetaGame(long timeout)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	p("Metagaming Phase Propnet");
    	game = getStateMachine();
    	role = getRole();
    	root = new MultiNode(getCurrentState(), null, null, 1, 0, true);
		expand(root);
		dataset = new ArrayList<List<Integer>>();
		performMCTSLearn(root, timeout - 2000);
		System.out.println("Dataset: ");
		System.out.println(dataset.toString());
    }

	@Override
	public void stateMachineStop(){
		MachineState ms = getCurrentState();
		SamplePropNetStateMachine sm = (SamplePropNetStateMachine) getStateMachine();
		Set<GdlSentence> props = ms.getContents();
		System.out.println("Final Propositions: " + props);
		System.out.println("All Propositions: ");
		List<Proposition> allProps = new ArrayList<Proposition>(sm.propNet.propositions);
		Collections.sort(allProps);
		List<Integer> featureVector = new ArrayList<Integer>();
		for (Proposition prop : allProps){
			GdlSentence name = prop.getName();
			System.out.println(name);
			if (props.contains(name)){
				featureVector.add(new Integer(1));
			}
			else{
				featureVector.add(new Integer(0));
			}
		}
		System.out.println(featureVector);
	}

	@Override
	public StateMachine getInitialStateMachine(){
		return new SamplePropNetStateMachine();
	}

	protected void performMCTSLearn(MultiNode root, long timeout)
    		throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
    	int numDepthCharges = 0;
    	while (System.currentTimeMillis() < timeout) {
    		double score = 0;
    		MachineState terminal = null;
    		MultiNode selected = select(root);
    		if (!game.findTerminalp(selected.state)) {
    			expand(selected);
        		terminal = game.performDepthCharge(selected.state, null);
    		} else {
    			terminal = selected.state;
    			MachineState ms = getCurrentState();
    			SamplePropNetStateMachine sm = (SamplePropNetStateMachine) getStateMachine();
    			Set<GdlSentence> props = ms.getContents();
    			System.out.println("Final Propositions: " + props);
    			System.out.println("All Propositions: ");
    			List<Proposition> allProps = new ArrayList<Proposition>(sm.propNet.propositions);
    			Collections.sort(allProps);
    			List<Integer> featureVector = new ArrayList<Integer>();
    			for (Proposition prop : allProps){
    				GdlSentence name = prop.getName();
    				System.out.println(name);
    				if (props.contains(name)){
    					featureVector.add(new Integer(1));
    				}
    				else{
    					featureVector.add(new Integer(0));
    				}
    			}
    			System.out.println(featureVector);
    			dataset.add(featureVector);
    		}
    		numDepthCharges++;
    		score = game.findReward(role, terminal) / 100.0;
    		backPropagate(selected, score);
    	}
    	p("Num Depth Charges PP: " + numDepthCharges);
    }

}
