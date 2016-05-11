package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;


@SuppressWarnings("unused")
public class SamplePropNetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;
    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
        try {
            propNet = OptimizingPropNetFactory.create(description);
            roles = propNet.getRoles();
            ordering = getOrdering();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Computes if the state is terminal. Should return the value
     * of the terminal proposition for the state.
     */
    @Override
    public boolean isTerminal(MachineState state) {
        // TODO: Compute whether the MachineState is terminal.
    	p("is terminal");
    	markbases(state, propNet);
    	return propmarkp(propNet.getTerminalProposition());
    }

    /**
     * Computes the goal for a role in the current state.
     * Should return the value of the goal proposition that
     * is true for that role. If there is not exactly one goal
     * proposition true for that role, then you should throw a
     * GoalDefinitionException because the goal is ill-defined.
     */
    @Override
    public int getGoal(MachineState state, Role role)
            throws GoalDefinitionException {
    	p("goal");
        // TODO: Compute the goal for role in state.
    	markbases(state, propNet);
    	List<Role> roles = propNet.getRoles();
    	// TODO: oluwasanya adjusted pseudocode here
    	Set<Proposition> rewards = propNet.getGoalPropositions().get(role);
      	for (Proposition p : rewards) {
      		if (propmarkp(p)) { return getGoalValue(p); } // TODO: how to get int value
      	}
      	throw new GoalDefinitionException(state, role);
    }

    /**
     * Returns the initial state. The initial state can be computed
     * by only setting the truth value of the INIT proposition to true,
     * and then computing the resulting state.
     */
    @Override
    public MachineState getInitialState() {
    	p("get initial state");
        // TODO: Compute the initial state.
    	propNet.setInitProposition(true);
    	return getStateFromBase();
    }

    /**
     * Computes all possible actions for role.
     */
    @Override
    public List<Move> findActions(Role role) throws MoveDefinitionException {
        // TODO: Compute legal moves.
    	p("find actions");
        return null;
    }

    /**
     * Computes the legal moves for role in state.
     */
    @Override
    public List<Move> getLegalMoves(MachineState state, Role role)
            throws MoveDefinitionException {
        markbases(state, propNet);
        List<Proposition> legals = new ArrayList<Proposition>();;
        for (int i = 0; i < roles.size(); i++){
        	if (role == roles.get(i)){
        		Map<Role, Set<Proposition> >legalMap = propNet.getLegalPropositions();
        		legals = new ArrayList<Proposition>(legalMap.get(role));
        		break;
        	}
        }
        List<Move> actions = new ArrayList<Move>();
        for (int i = 0; i < legals.size(); i++){
        	if (propmarkp(legals.get(i))){
        		actions.add(getMoveFromProposition(legals.get(i)));
        	}
        }
        return actions;
    }

    /**
     * Computes the next state given state and the list of moves.
     */
    @Override
    public MachineState getNextState(MachineState state, List<Move> moves)
            throws TransitionDefinitionException {
        markactions(moves, propNet);
        Map<GdlSentence, Proposition> bases = propNet.getBasePropositions();
        //List<Move> nexts = new ArrayList<Move>();
        for (int i = 0; i < bases.size(); i++){
        	List<GdlSentence> keys = new ArrayList<GdlSentence>(bases.keySet());
        	propmarkp(bases.get(keys.get(i)));

        }
        MachineState nextState = getStateFromBase();
        return nextState;
    }

	/**
     * This should compute the topological ordering of propositions.
     * Each component is either a proposition, logical gate, or transition.
     * Logical gates and transitions only have propositions as inputs.
     *
     * The base propositions and input propositions should always be exempt
     * from this ordering.
     *
     * The base propositions values are set from the MachineState that
     * operations are performed on and the input propositions are set from
     * the Moves that operations are performed on as well (if any).
     *
     * @return The order in which the truth values of propositions need to be set.
     */
    public List<Proposition> getOrdering()
    {
        // List to contain the topological ordering.
        List<Proposition> order = new LinkedList<Proposition>();

        // All of the components in the PropNet
        List<Component> components = new ArrayList<Component>(propNet.getComponents());

        // All of the propositions in the PropNet.
        List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());

        // TODO: Compute the topological ordering.

        return order;
    }

    /* Already implemented for you */
    @Override
    public List<Role> getRoles() {
        return roles;
    }

    /* Helper methods */
    /**
     * The Input propositions are indexed by (does ?player ?action).
     *
     * This translates a list of Moves (backed by a sentence that is simply ?action)
     * into GdlSentences that can be used to get Propositions from inputPropositions.
     * and accordingly set their values etc.  This is a naive implementation when coupled with
     * setting input values, feel free to change this for a more efficient implementation.
     *
     * @param moves
     * @return
     */
    private List<GdlSentence> toDoes(List<Move> moves)
    {
        List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
        Map<Role, Integer> roleIndices = getRoleIndices();

        for (int i = 0; i < roles.size(); i++)
        {
            int index = roleIndices.get(roles.get(i));
            doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
        }
        return doeses;
    }

    /**
     * Takes in a Legal Proposition and returns the appropriate corresponding Move
     * @param p
     * @return a PropNetMove
     */
    public static Move getMoveFromProposition(Proposition p)
    {
        return new Move(p.getName().get(1));
    }

    /**
     * Helper method for parsing the value of a goal proposition
     * @param goalProposition
     * @return the integer value of the goal proposition
     */
    private int getGoalValue(Proposition goalProposition)
    {
        GdlRelation relation = (GdlRelation) goalProposition.getName();
        GdlConstant constant = (GdlConstant) relation.get(1);
        return Integer.parseInt(constant.toString());
    }

    /**
     * A Naive implementation that computes a PropNetMachineState
     * from the true BasePropositions.  This is correct but slower than more advanced implementations
     * You need not use this method!
     * @return PropNetMachineState
     */
    public MachineState getStateFromBase()
    {
        Set<GdlSentence> contents = new HashSet<GdlSentence>();
        for (Proposition p : propNet.getBasePropositions().values())
        {
            p.setValue(p.getSingleInput().getValue());
            if (p.getValue())
            {
                contents.add(p.getName());
            }
        }
        return new MachineState(contents);
    }

    /** additional methods from 10.3 and 10.4 */
    // TODO:
    private void markbases (MachineState state, PropNet propNet) {
    	Map<GdlSentence, Proposition> props = propNet.getBasePropositions();
    	p("markBases: Base Props   : " + props.size());
    	Set<GdlSentence> stateContents = state.getContents();
    	p("markBases: Machine State: " + stateContents.size());
    	for (GdlSentence gs : stateContents) {
    		props.get(gs).setValue(true);
    	}
    }

   private void markactions (List<Move> moves, PropNet propNet) {
	 Map<GdlSentence, Proposition> props = propNet.getInputPropositions();
	 p("markActions: Base Props: " + props.size());
	 p("markActions: Moves     : " + moves.size());
	 // is this accurate?
	 for (Move move : moves) {
		 props.get(move.getContents().toSentence()).setValue(true);
	 }
   }

   private void clearpropnet (PropNet propNet) {
	   p("clearpropnet");
	   Map<GdlSentence, Proposition> props = propNet.getBasePropositions();
	   p("clearPropnet: Base Props: " + props.size());
	   for (GdlSentence gs : props.keySet()) {
		   props.get(gs).setValue(false);
	   }
   }

   public boolean propmarkp (Proposition p) {
	   p("propmarkp " + p.toString());
	   // base
		if ((Component) p instanceof Transition) return p.getValue();
		// input
		else if (((GdlRelation) p.getName()).getName().getValue().equals("does")) return p.getValue();

		else if ((Component) p instanceof Not) return propmarknegation(p);
		else if ((Component) p instanceof And) return propmarkconjunction(p);
		else if ((Component) p instanceof Or) return propmarkdisjunction(p);
		else if ((Component) p instanceof Transition) return propmarkp((Proposition) p.getSingleInput()); // TODO
		else { p("returning false"); return false; }
   }

//	public boolean propmarkp (Proposition p) {
//		p("propmarkp " + p.toString());
//		if (p.getName().toString()=="base") return p.getValue();
//		else if (p.getName().toString()=="input") return p.getValue();
//		else if (p.getName().toString()=="view") return propmarkp((Proposition) p.getSingleInput());
//		else  if (p.getName().toString()=="negation") return propmarknegation(p);
//		else if (p.getName().toString()=="conjunction") return propmarkconjunction(p);
//		else if (p.getName().toString()=="disjunction") return propmarkdisjunction(p);
//		else { p("returning false"); return false; }
//	}

	public boolean propmarknegation (Proposition p) {
		p("propmarknegation " + p.toString());
		return !propmarkp((Proposition)p.getSingleInput());
	}

	private boolean propmarkconjunction (Proposition p) {
		p("propmarkconjunction " + p.toString());
		Set<Component> sources = p.getInputs();
		for (Component source : sources) {
			if (!propmarkp((Proposition) source)) return false;
		}
		return true;
   }

	private boolean propmarkdisjunction (Proposition p) {
		p("propmarkdisjunction " + p.toString());
		Set<Component> sources = p.getInputs();
		for (Component source : sources) {
			if (propmarkp((Proposition) source)) return true;
		}
		return true;
	}

	private void p(String word) { System.out.println(word); }
}