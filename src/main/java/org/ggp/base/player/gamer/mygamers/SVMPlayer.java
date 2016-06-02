package org.ggp.base.player.gamer.mygamers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.propnet.PropNetStateMachine;
import org.ggp.base.util.statemachine.implementation.propnet.SamplePropNetStateMachine;

public class SVMPlayer extends OptimizedPropnetPlayer {

	private List<List<Integer>> dataset;
	private int limit;
    private long finishBy;
    private long winProb;
    private List<Integer> dataSetClasses;
    private List<Double> avgAlpha;

    @Override
    public StateMachine getInitialStateMachine(){
    	return new PropNetStateMachine();
    }

    @Override
    public void stateMachineMetaGame(long timeout)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	p("Metagaming Phase Propnet");
    	game = getStateMachine();
    	role = getRole();
    	root = new MultiNode(getCurrentState(), null, null, 1, 0, true);
		expand(root);
		dataset = new ArrayList<List<Integer>>();
		dataSetClasses = new ArrayList<Integer>();
		performMCTSLearn(root, timeout - 2000);
		avgAlpha = svmTrain();
		System.out.println("Avg alpha: " + avgAlpha.toString());
		System.out.println("Dataset: ");
		System.out.println(dataset.toString());
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
    			PropNetStateMachine sm = (PropNetStateMachine) getStateMachine();
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
    			boolean won = true;
    			int ourScore = sm.findReward(role, ms);
    			if (sm.getRoles().size() > 1){
    				for (Role r : game.getRoles()){
    					if (sm.findReward(r, getCurrentState()) > ourScore){
    						won = false;
    					}
    				}
    			}
    			else{
    				if (ourScore < 70){
    					won = false;
    				}
    			}
    			if (won){
    				dataSetClasses.add(1);
    			}
    			else{
    				dataSetClasses.add(0);
    			}
    		}
    		numDepthCharges++;
    		score = game.findReward(role, terminal) / 100.0;
    		backPropagate(selected, score);
    	}
    	p("Num Depth Charges PP: " + numDepthCharges);
    }

    private List<Double> svmTrain(){
		int tau = 8;
		double heuristic = 0;
		int mTrain = dataset.size();
		List<Double> alpha = new ArrayList<Double>(Collections.nCopies(mTrain, 0.0));
		List<Double> avgAlpha = new ArrayList<Double>(Collections.nCopies(mTrain, 0.0));

		// Construct kernel matrix
		List<List<Double>> kernel = new ArrayList<List<Double>>();
		for (int i = 0; i < mTrain; i++) {
			List<Double> row_vec = new ArrayList<Double>();
			for (int j = 0; j < mTrain; j++) {
				double squaredNorm = 0;
				for (int k = 0; k < mTrain; k++) {
					double diff = dataset.get(i).get(k) - dataset.get(j).get(k);
					squaredNorm += Math.pow(diff, 2);
				}
				row_vec.add(Math.exp(squaredNorm / (-2 * Math.pow(tau, 2))));
			}
			kernel.add(row_vec);
		}


		double numOuterLoops = 40;
		double lambda = 1 / (mTrain * 64);
		double count = 0;
		Random rand = new Random();

		for (int i = 0; i < (numOuterLoops*mTrain); i++) {
			count++;
			int index = (int) Math.ceil(rand.nextDouble() * mTrain);

			// Compute margin
			double margin = 0;
			for (int j = 0; j < mTrain; j++) {
				margin += kernel.get(index).get(j) * alpha.get(index);
			}
			margin = (2*dataSetClasses.get(index)-1) * margin; // Convert any 0's to -1's

			// Compute update using hinge loss
			List<Double> g = new ArrayList<Double>(Collections.nCopies(mTrain, 0.0));
			if (margin < 1) {
				for (int j = 0; j < mTrain; j++) {
					g.set(j, -(2*dataSetClasses.get(index)-1) * kernel.get(j).get(index));
				}
			}
			for (int j = 0; j < mTrain; j++) {
				double currG = g.get(j);
				currG += mTrain * lambda * kernel.get(j).get(index) * alpha.get(index);
				g.set(j, currG);
			}

			// Update alpha
			for (int j = 0; j < mTrain; j++) {
				double currAlpha = alpha.get(j);
				double currAvgAlpha = avgAlpha.get(j);
				currAlpha -= g.get(j) / Math.sqrt(count);
				alpha.set(j, currAlpha);
				avgAlpha.set(j, currAvgAlpha + currAlpha);
			}
		}
		for (int i = 0; i < mTrain; i++) {
			double currAvgAlpha = avgAlpha.get(i);
			avgAlpha.set(i, currAvgAlpha / (numOuterLoops*mTrain));
		}

		return avgAlpha;
    }

    @Override
    public Move stateMachineSelectMove(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	finishBy = timeout - 3000;
    	return bestMove(getRole(), getCurrentState());
    }

    private Move bestMove(Role role, MachineState state)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	long start = System.currentTimeMillis();

    	StateMachine game = getStateMachine();
        List<Move> actions = game.findLegals(role, state);
        limit = 12; // make variable
        Move action = actions.get(0);
        double alpha = 0;
        double beta = 100;
        double score = 0;
        for (int i = 0; i < actions.size(); i++) {
        	// break early
        	if (System.currentTimeMillis() > finishBy) return action;
        	double result = minscore(role, actions.get(i), state, alpha, beta, 0);
        	if (result == 100) return actions.get(i);
        	if (result > score) {
        		score = result;
        		action = actions.get(i);
        	}
        }
        long stop = System.currentTimeMillis();
        notifyObservers(new GamerSelectedMoveEvent(actions, action, stop - start));
        return action;
    }

    private double minscore(Role role, Move action, MachineState state, double alpha, double beta, int level)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	StateMachine game = getStateMachine();
    	// break if time is up
    	for (List<Move> jointMove : game.getLegalJointMoves(state, role, action)) {
    		MachineState newstate = game.getNextState(state, jointMove);
    		double result = maxscore(role, newstate, alpha, beta, level + 1);
    		beta = Math.min(beta, result);
    		if (beta <= alpha) return alpha;
    	}
    	return beta;
    }

    private double maxscore(Role role, MachineState state, double alpha, double beta, int level)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	StateMachine game = getStateMachine();
    	if (game.findTerminalp(state)) return game.findReward(role, state);
    	// added heuristic value here
    	if (level >= limit || System.currentTimeMillis() > finishBy) return learnedHeuristicValue(role, state);
    	List<Move> actions = game.findLegals(role, state);
    	for (int i = 0; i < actions.size(); i++) {
    		double result = minscore(role, actions.get(i), state, alpha, beta, level);
    		alpha = Math.max(alpha, result);
    		if (alpha >= beta) return beta;
    	}
    	return alpha;
    }

    private double learnedHeuristicValue(Role role, MachineState ms){
    	System.out.println("Start heuristic");
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


		double heuristic = 0;
		for (int i = 0; i < featureVector.size(); i++){
			Integer feature = featureVector.get(i);
			feature = feature*2-1;
			heuristic  += feature*avgAlpha.get(i);
			System.out.println(heuristic);
		}
		return heuristic;
    }
}
