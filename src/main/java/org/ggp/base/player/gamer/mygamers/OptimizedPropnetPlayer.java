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

	//    @Override
	//    public void stateMachineMetaGame(long timeout)
	//        throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
	//      p("Debug Metagaming Phase Propnet");
	//      prover = getProverStateMachine();
	//        prover.initialize(getMatch().getGame().getRules());
	//
	//        propnet = getPropNetStateMachine();
	//        propnet.initialize(getMatch().getGame().getRules());
	//       private ArrayList<Move> bestPathReversed = new ArrayList<Move>();
	//
	//      game = new DualStateMachine(prover, propnet);
	//      role = getRole();
	//		stepAfterFoundBestMove = 0;
	//      root = new MultiNode(getCurrentState(), null, null, 1, 0, true);
	//    expand(root);
	//    performMCTS(root, timeout - 1000);
	//    }

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		p("Metagaming Phase Optimized Propnet");
		init();
		expand(root);
		performMCTS(root, timeout - 1000);
	}

	// does the initialization
	private void init() {
		game = getStateMachine();
		role = getRole();
		root = new MultiNode(getCurrentState(), null, null, 1, 0, true);
		bestPathReversed = new ArrayList<Move>();
		isSinglePlayer = false;
		bestPathFound = false;
		stepAfterFoundBestMove = 0;
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
			p("previous perfect move: " + bestMove);
			stepAfterFoundBestMove++;
			p("bestPath: " + bestPathReversed);
			p("step    : " + stepAfterFoundBestMove);
			bestMove = bestPathReversed.get(bestPathReversed.size() - stepAfterFoundBestMove);
			// stepAfterFoundBestMove++;
			p("playing perfect move : " + bestMove);
			return bestMove;
		}
		if (!isFirstMove) {
			root = new MultiNode(getCurrentState(), null, null, 1, 0, true);
		}
		expand(root);
		isFirstMove = false;
		performMCTS(root, timeout - 2000);
		return getBestMove();
	}

	/************* major helper functions
	 * @throws TransitionDefinitionException
	 * @throws MoveDefinitionException *****************/
	// added node.isMax to this function
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
			// bug fix??
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

	private void backPropagate(MultiNode node, double score) {
		if (isSinglePlayer && bestPathFound && node.isMax && node.parent != null) { // the move it gets at a max node
			p("adding move: " + node.jointMoves.get(0));
			bestPathReversed.add(node.jointMoves.get(0));
		}
		node.utility += score;
		node.visits++;
		if (useUCBTuned) node.utilities.add(node.utility);
		if (node.parent != null) {
			backPropagate(node.parent, score);
		}
	}

	/************* minor helper functions *****************/
	private void performMCTS(MultiNode root, long timeout)mnmDCA
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		int numDepthCharges = 0;
		while (System.currentTimeMillis() < timeout && !bestPathFound) {
			double score = 0;
			MachineState terminal = null;
			MultiNode selected = select(root);
			if (!selected.isMax) p("from min node: " + selected.move);
			if (!game.findTerminalp(selected.state)) {
				expand(selected);
				terminal = game.performPropNetDepthCharge(selected.state, null);
			} else {
				terminal = selected.state;
			}
			numDepthCharges++;
			// informs us that we have found a sure line of attack
			score = game.findReward(role, terminal) / 100.0;
			if (score == 1 && isSinglePlayer) {
				p("found forced win");
				bestPathReversed = reverse(game.getBestMoves());
				bestPathFound = true;
			}
			backPropagate(selected, score);
		}
		p("Num Depth Charges OP: " + numDepthCharges);
	}

	private Move getBestMove() throws MoveDefinitionException {
		if (bestPathFound) {
			// we save time on reversing the loop and rather just work backwards instead
			p("previous perfect move: " + bestMove);
			stepAfterFoundBestMove++;
			p("bestPath: " + bestPathReversed);
			p("step    : " + stepAfterFoundBestMove);
			bestMove = bestPathReversed.get(bestPathReversed.size() - stepAfterFoundBestMove);
			// stepAfterFoundBestMove++;
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
		if (useUCBTuned) {
			double result = Math.log(node.parent.visits) / node.visits;
			double factor = Math.min(0.25, adjustedVariance(node));
			return Math.sqrt(result * factor);
		} else {
			return 2 * Math.log(node.parent.visits) / node.visits;
		}
	}

	private double adjustedVariance(MultiNode node) {
		double result = 0;
		for (double utility : node.utilities) {
			result += Math.pow(utility, 2);
		}
		return (0.5 * result) - (Math.pow(node.getAveUtility(), 2)) + (Math.sqrt(2 * Math.log(node.parent.visits) / node.visits));
	}

	/*********************** variables *******************/
	/* dynamic game state data */
	private Move bestMove = null;
	private StateMachine game = null;
	private Role role = null;
	private MultiNode root = null;

	/***************** single player games **************/
	private boolean isSinglePlayer = false;
	private boolean bestPathFound = false;
	private ArrayList<Move> bestPathReversed = null;
	private int stepAfterFoundBestMove = 0;

	/* game information data */
	private boolean isFirstMove = true;
	private boolean useUCBTuned = false;

	/* game parameter data */
	private double explorationFactor = Math.sqrt(2.3);

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
}