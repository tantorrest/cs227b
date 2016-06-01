package org.ggp.base.player.gamer.mygamers;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.propnet.PropNetStateMachine;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public class StablePlayer extends SampleGamer {
	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		p("Metagaming Phase Optimized Propnet: " + getMatch().getMatchId());
		init();
		expand(root);
		long start = System.currentTimeMillis();
		finishBy = timeout - 5000;
		performMCTS(root);
		timeToDepthCharge = (System.currentTimeMillis() - start) / (numDepthCharges + 1);
		p("time to depth charge: " + timeToDepthCharge);
	}

	// does the initialization
	private void init() {
		game = getStateMachine();
		role = getRole();
		root = new MultiNode(getCurrentState(), null, null, 1, 0, true);
		bestPathReversed = new ArrayList<Move>();
		isFirstMove = true;
		isSinglePlayer = false;
		bestPathFound = false;
		stepAfterFoundBestMove = 0;
		prevNumMoves = 0;
		isSinglePlayer = (game.getRoles().size() == 1);
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new PropNetStateMachine());
	}

	public StateMachine getProverStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	public StateMachine getPropNetStateMachine() {
		return new CachedStateMachine(new PropNetStateMachine());
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		root = getRoot();
		if (root.children.size() == 0) expand(root);
		prevNumMoves = game.getLegalMoves(getCurrentState(), role).size();
		finishBy = timeout - 1500 - timeToDepthCharge;
		performMCTS(root);
		return getBestMove();
	}

	private MultiNode getRoot() {
		state = getCurrentState();
			if (!isFirstMove) {
				return new MultiNode(state, null, null, 1, 0, true);
			}
			isFirstMove = false;

		return root;
	}

	/************* major helper functions *********/
	private MultiNode select(MultiNode node) throws MoveDefinitionException, TransitionDefinitionException {
		if (node.isMax) {
			if ((node.visits == 0 || game.findTerminalp(node.state))) return node;
			double score = selectfnMax(node.children.get(0));
			MultiNode result = node.children.get(0);
			for (int i = 1; i < node.children.size(); i++) {
				double newscore = selectfnMax(node.children.get(i));
				if (newscore > score) {
					score = newscore;
					result = node.children.get(i);
				}
			}
			if (result.children.size() == 0) expand(result);
			return select(result);
		} else {
			for (int i = 0; i < node.children.size(); i++) {
				if (node.children.get(i).visits == 0) return node.children.get(i);
			}
			double score = selectfnMin(node.children.get(0));
			MultiNode result = node.children.get(0);
			for (int i = 1; i < node.children.size(); i++) {
				double newscore = selectfnMin(node.children.get(i));
				if (newscore > score) {
					score = newscore;
					result = node.children.get(i);
				}
			}
			return select(result);
		}
	}

	protected void expand(MultiNode node)
			throws MoveDefinitionException, TransitionDefinitionException {
		if (node.isMax) {
			List<Move> moves = game.getLegalMoves(node.state, role);
			for (Move move : moves) {
				MultiNode newnode = new MultiNode(node.state, move, null, 0, 0, !(node.isMax)); // alternate state
				node.addChild(newnode);
			}
		} else {
			List<List<Move>> jointMoves = game.getLegalJointMoves(node.state, role, node.move);
			for (List<Move> jointMove : jointMoves) {
				MachineState nextState = game.getNextState(node.state, jointMove);
				MultiNode newnode = new MultiNode(nextState, null, jointMove, 0, 0, !(node.isMax));
				node.addChild(newnode);
			}
		}
	}

	/************* minor helper functions *****************/
	protected void performMCTS(MultiNode root)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		numDepthCharges = 0;
		while (System.currentTimeMillis() < finishBy) {
			double score = 0;
			MachineState terminal = null;
			MultiNode selected = select(root);
			if (!game.findTerminalp(selected.state)) {
				expand(selected);
				terminal = game.performDepthCharge(selected.state, null);
			} else {
				terminal = selected.state;
			}
			numDepthCharges++;
			score = game.findReward(role, terminal);
			backPropagate(selected, score);
		}
		p("Num Depth Charges SP: " + numDepthCharges);
	}

	private void backPropagate(MultiNode node, double score) {
		node.updateUtilityAndVisits(score);
		if (node.parent != null) {
			backPropagate(node.parent, score);
		}
	}

	private Move getBestMove() throws MoveDefinitionException {

		double bestUtility = 0;
		for (MultiNode child : root.children) {
			if (child.getAveUtility() > bestUtility) {
				bestUtility = child.getAveUtility();
				bestMove = child.move;
			}
		}
		p("utility SP: " + bestUtility);
		return (bestUtility != 0) ? bestMove : game.getRandomMove(getCurrentState(), role);
	}

	private double selectfnMax(MultiNode node) {
		return (node.getAveUtility()) + explorationFactor * Math.sqrt(tunedFunction(node));
	}

	private double selectfnMin(MultiNode node) {
		return (-node.getAveUtility()) + explorationFactor * Math.sqrt(tunedFunction(node));
	}

	private double tunedFunction(MultiNode node) {
		return Math.log(node.parent.visits) / node.visits;
	}

	/*********************** variables *******************/
	/* dynamic game state data */
	private Move bestMove = null;
	protected StateMachine game = null;
	protected Role role = null;
	protected MultiNode root = null;
	protected int prevNumMoves = 0;

	protected long timeToDepthCharge = 0;
	protected int numDepthCharges = 0;
	private MachineState state = null;
	protected long finishBy = 0;

	/***************** single player games **************/
	protected boolean isSinglePlayer = false;
	protected boolean bestPathFound = false;
	protected ArrayList<Move> bestPathReversed = null;
	protected int stepAfterFoundBestMove = 0;

	/* game information data */
	protected boolean isFirstMove = true;

	/* game parameter data */
	private double explorationFactor = 125;

	public ArrayList<Move> reverse(List<Move> moves) {
		p("moves: " + moves.toString());
		for (int i = 0; i < moves.size() / 2; i++) {
			Move tmp = moves.get(i);
			moves.set(i, moves.get(moves.size() - i - 1));
			moves.set(moves.size() - i - 1, tmp);
		}
		return (ArrayList<Move>) moves;
	}

	protected void p(String message) { System.out.println(message); }
}