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

public class OptimizedPropnetPlayer extends SampleGamer {
	private int prevDepthCharges;

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		p("Metagaming Phase Optimized Propnet: " + getMatch().getMatchId());
		init();
		expand(root);
		long start = System.currentTimeMillis();
		performMCTS(root, timeout - 5000);
		timeToDepthCharge = (System.currentTimeMillis() - start) / numDepthCharges;
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
		numDepthCharges = 0;
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
		if (isSinglePlayer && bestPathFound) {
			// we save time on reversing the loop and rather just work backwards instead
			stepAfterFoundBestMove++;
			bestMove = bestPathReversed.get(bestPathReversed.size() - stepAfterFoundBestMove);
			return bestMove;
		}

		// last move was a noop so we can use opponent's moves
		root = getRoot();
		if (root.children.size() == 0) expand(root);
		prevNumMoves = game.getLegalMoves(getCurrentState(), role).size();
		performMCTS(root, timeout - 1000 - timeToDepthCharge);
		return getBestMove();
	}

	private MultiNode getRoot() {

		state = getCurrentState();
		if (prevNumMoves == 1 && !isSinglePlayer) {
			MultiNode child = root.children.get(0); // we played a noop
			for (MultiNode next : child.children) {
				if (next.state.equals(state)) return next;
			}
			System.err.println("Could not find state in getRoot. Returning new root");
			return new MultiNode(state, null, null, 1, 0, true);
		} else {
			if (!isFirstMove) {
				return new MultiNode(state, null, null, 1, 0, true);
			}
			isFirstMove = false;
		}
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

	private void expand(MultiNode node)
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
	private void performMCTS(MultiNode root, long timeout)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		numDepthCharges = 0;
		while (System.currentTimeMillis() < timeout && !bestPathFound) {
			double score = 0;
			MachineState terminal = null;
			MultiNode selected = select(root);
			if (!game.findTerminalp(selected.state)) {
				expand(selected);
				terminal = game.performPropNetDepthCharge(selected.state, null);
			} else {
				terminal = selected.state;
			}
			numDepthCharges++;
			// informs us that we have found a sure line of attack
			score = game.findReward(role, terminal);
			if (score == 100 && isSinglePlayer) {
				p("found forced win");
				bestPathReversed = reverse(game.getBestMoves());
				bestPathFound = true;
			}
			backPropagate(selected, score);
		}
		p("Num Depth Charges OP: " + numDepthCharges);
	}

	private void backPropagate(MultiNode node, double score) {
		// the move it gets at a max node
		if (bestPathFound && node.isMax && node.parent != null) {
			p("adding move: " + node.jointMoves.get(0));
			bestPathReversed.add(node.jointMoves.get(0));
		}
		node.updateUtilityAndVisits(score);
		if (node.parent != null) {
			backPropagate(node.parent, score);
		}
	}

	private Move getBestMove() throws MoveDefinitionException {
		if (bestPathFound) {
			// we save time on reversing the loop and rather just work backwards instead
			p("previous perfect move: " + bestMove);
			stepAfterFoundBestMove++;
			p("bestPath: " + bestPathReversed);
			p("step    : " + stepAfterFoundBestMove);
			bestMove = bestPathReversed.get(bestPathReversed.size() - stepAfterFoundBestMove);
			p("playing perfect move : " + bestMove);
		}
		double bestUtility = 0;
		for (MultiNode child : root.children) {
			if (child.getAveUtility() > bestUtility) {
				bestUtility = child.getAveUtility();
				bestMove = child.move;
			}
		}
		p("utility OP: " + bestUtility);
		return (bestUtility != 0) ? bestMove : game.getRandomMove(getCurrentState(), role);
	}

	private double selectfnMax(MultiNode node) {
		return (node.getAveUtility()) + explorationFactor * Math.sqrt(tunedFunction(node));
	}

	private double selectfnMin(MultiNode node) {
		return (-node.getAveUtility()) + explorationFactor * Math.sqrt(tunedFunction(node));
	}

	private double tunedFunction(MultiNode node) {
		return 2 * Math.log(node.parent.visits) / node.visits;
	}

	/*********************** variables *******************/
	/* dynamic game state data */
	private Move bestMove = null;
	private StateMachine game = null;
	private Role role = null;
	private MultiNode root = null;
	private int prevNumMoves = 0;

	private long timeToDepthCharge = 0;
	private int numDepthCharges = 0;
	private MachineState state = null;

	/***************** single player games **************/
	private boolean isSinglePlayer = false;
	private boolean bestPathFound = false;
	private ArrayList<Move> bestPathReversed = null;
	private int stepAfterFoundBestMove = 0;

	/* game information data */
	private boolean isFirstMove = true;

	/* game parameter data */
	private double explorationFactor = 110;

	public ArrayList<Move> reverse(List<Move> moves) {
		p("moves: " + moves.toString());
		for (int i = 0; i < moves.size() / 2; i++) {
			Move tmp = moves.get(i);
			moves.set(i, moves.get(moves.size() - i - 1));
			moves.set(moves.size() - i - 1, tmp);
		}
		return (ArrayList<Move>) moves;
	}

	private void p(String message) { System.out.println(message); }

	//	private StateMachine prover;
	//	private StateMachine propnet;
	//	@Override
	//	public void stateMachineMetaGame(long timeout)
	//			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
	//		p("Debug Metagaming Phase Propnet");
	//		prover = getProverStateMachine();
	//		prover.initialize(getMatch().getGame().getRules());
	//
	//		propnet = getPropNetStateMachine();
	//		propnet.initialize(getMatch().getGame().getRules());
	//
	//		bestPathReversed = new ArrayList<Move>();
	//		game = new DualStateMachine(prover, propnet);
	//		role = getRole();
	//		stepAfterFoundBestMove = 0;
	//		root = new MultiNode(getCurrentState(), null, null, 1, 0, true);
	//		expand(root);
	//		performMCTS(root, timeout - 1000);
	//	}

	//	UCBTuned functions
	//	private double tunedFunction(MultiNode node) {
	//		if (useUCBTuned) {
	//			double result = Math.log(node.parent.visits) / node.visits;
	//			double factor = Math.min(0.25, adjustedVariance(node));
	//			return result * factor;
	//		} else {
	//			return 2 * Math.log(node.parent.visits) / node.visits;
	//		}
	//	}
	//
	//	private double adjustedVariance(MultiNode node) {
	//		double result = 0;
	//		for (double utility : node.utilities) {
	//			result += Math.pow(utility, 2);
	//		}
	//		return (0.5 * result) - (Math.pow(node.getAveUtility(), 2)) + (Math.sqrt(2 * Math.log(node.parent.visits) / node.visits));
	//	}

	//		private double adjustedVariance(MultiNode node) {
	//			double result = 0;
	//			for (double utility : node.utilities) {
	//				result += Math.pow(utility, 2);
	//			}
	//			return (0.5 * result) - (Math.pow(node.getAveUtility(), 2)) + (Math.sqrt(2 * Math.log(node.parent.visits) / node.visits));
	//		}
	//	private boolean useUCBTuned = false;
}