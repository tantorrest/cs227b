package org.ggp.base.player.gamer.mygamers;

import java.util.List;

import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MCTSPlayer extends SampleGamer {
	private int count;
	private List<Role> gameRoles;
	private int numRoles;
	private int myRoleNum;
    @Override
    public void stateMachineMetaGame(long timeout)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	count = 0;
    	game = getStateMachine();
    	gameRoles = game.getRoles();
    	role = getRole();
    	numRoles = gameRoles.size();
    	myRoleNum = gameRoles.indexOf(role);
    	p("Doing metagaming");
    }

	@Override
    public Move stateMachineSelectMove(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		root = new MCTSNode(getCurrentState(), null, 1, 0);
		explorationFactor = Math.sqrt(2);
		bestMove = game.getRandomMove(getCurrentState(), role);
//		if(currRole == numRoles) currRole = 0;
//		else currRole ++;
    	while (System.currentTimeMillis() < timeout - 1000) {
    		double score = 0;
    		MachineState terminal = null;
    		MCTSNode selected = null;
    		if (!game.findTerminalp(root.state)) {
    			expand(root);
    			selected = select(root);
        		terminal = game.performDepthCharge(selected.state, null);
    		} else {
    			terminal = root.state;
    			selected = root;
    		}
    		score = game.findReward(role, terminal) / 100.0;
//    		p("" + score);
    		backPropagate(selected, score);

//    		for (MCTSNode s : root.children) {
//    			p("move " + s.move + " has value " + s.getAveUtility());
//    		}
    	}

    	// extra information
		for (MCTSNode s : root.children) {
//			p("move " + s.move + " has value " + s.getAveUtility());
		}

    	// return the best value
		double bestUtility = 0;
		p("" + root.children.size());
		for (MCTSNode child : root.children) {
    		if (child.getAveUtility() > bestUtility) {
    			System.out.println("Updating to " + child.getAveUtility());
    			bestUtility = child.getAveUtility();
    			bestMove = child.move;
    		}
    	}

    	p("Chosen Move: " + bestMove.toString());
		return bestMove;
    }


	// works as far as I know
    private double selectfn(MCTSNode node) {
    	return (node.getAveUtility()) + explorationFactor * Math.sqrt(2 * Math.log(node.parent.visits) / node.visits);
    }

    // pretty sure this function does what it's meant to do
    private void backPropagate(MCTSNode node, double score) {
    	node.updateUtilityAndVisits(score);
    	if (node.parent != null) {
    		backPropagate(node.parent, score);
    	}
    }

    private void p(String message) {
    	System.out.println(message);
    }

    /* private variables */
    private Move bestMove;
    private StateMachine game;
    private Role role;
    private MCTSNode root;
    private double explorationFactor;


    /* minimax */
    private double maxscore(Role role, MachineState state, double alpha, double beta)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	StateMachine game = getStateMachine();
    	if (game.findTerminalp(state)) return game.findReward(role, state);
    	List<Move> actions = game.findLegals(role, state);
    	for (int i = 0; i < actions.size(); i++) {
    		double result = minscore(role, actions.get(i), state, alpha, beta);
    		alpha = Math.max(alpha, result);
    		if (alpha >= beta) return beta;
    	}
    	return alpha;
    }

    private double minscore(Role role, Move action, MachineState state, double alpha, double beta)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	StateMachine game = getStateMachine();
    	for (List<Move> jointMove : game.getLegalJointMoves(state, role, action)) {
    		MachineState newstate = game.getNextState(state, jointMove);
    		double result = maxscore(role, newstate, alpha, beta);
    		beta = Math.min(beta, result);
    		if (beta <= alpha) return alpha;
    	}
    	return beta;
    }


    private void expand(MCTSNode node)
    		throws MoveDefinitionException, TransitionDefinitionException {

//    	p("in expand");
    	game.getRoles();
    	//List<Move> actions = game.getLegalMoves(node.state, role);
    	List<List<Move>> actions = game.getLegalJointMoves(node.state);
    	for (int i = 0; i < actions.size(); i++) {
    		List<Move> jointMove = actions.get(i);
    				//new ArrayList<Move>();
    		//jointMove.add(actions.get(i));
    		//System.out.println("node state " + node.state +", jointMove = " + jointMove);
    		MachineState newstate = game.getNextState(node.state, jointMove); // changed from getCurrentState to state
    		MCTSNode newnode = new MCTSNode(newstate, jointMove.get(0), 0, 0);
    		node.addChild(newnode);
    		count++;
    		//System.out.println("Count = " + count);
    	}
//    	p("out expand");
    }

    // find an expandable node
    private MCTSNode select(MCTSNode node) {
//    	p("in select");
    	if (node.visits == 0) return node;
    	for (int i = 0; i < node.children.size(); i++) {
    		if (node.children.get(i).visits == 0) return node.children.get(i);
    	}
    	double score = 0;
    	MCTSNode result = node;
    	for (int i = 0; i < node.children.size(); i++) {
    		double newscore = selectfn(node.children.get(i));
    		if (newscore > score) {
    			score = newscore;
    			result = node.children.get(i);
    		}
    	}
    	return select(result);
    }

}