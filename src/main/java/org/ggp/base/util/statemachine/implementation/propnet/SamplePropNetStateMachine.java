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
import org.ggp.base.util.propnet.architecture.components.Constant;
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
    protected PropNet propNet;
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
            setPropNet(OptimizingPropNetFactory.create(description));
            roles = getPropNet().getRoles();
            ordering = getOrdering();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void reportStats() {
    	p("Size: " + getPropNet().getSize());
    	p("Num Ands: " + getPropNet().getNumAnds());
    	p("Num Ors: " + getPropNet().getNumOrs());
    	p("Num Nots: " + getPropNet().getNumNots());
    	p("Num Links: " + getPropNet().getNumLinks());
    }

    /**
     * Computes if the state is terminal. Should return the value
     * of the terminal proposition for the state.
     */
    @Override
    public boolean isTerminal(MachineState state) {
    	markbases(state, getPropNet());
    	return propmarkp(getPropNet().getTerminalProposition());
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
    	markbases(state, getPropNet());
    	Set<Proposition> rewards = getPropNet().getGoalPropositions().get(role);
//    	p("rewards: " + rewards.toString());
    	for (Proposition p : rewards) {
      		if (propmarkp(p)) {
//      			p(p.toString());
      			return getGoalValue(p);
      		}
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
    	getPropNet().setInitProposition(true);
    	return getStateFromBase();
    }

    /**
     * Computes all possible actions for role.
     */
    @Override
    public List<Move> findActions(Role role) throws MoveDefinitionException {
    	Map<GdlSentence, Proposition> actionsMap = getPropNet().getInputPropositions();
    	List<Move> actions = new ArrayList<Move>();
    	for (Proposition p : actionsMap.values()) {
    		actions.add(getMoveFromProposition(p));
    	}
        return actions;
    }

    /**
     * Computes the legal moves for role in state.
     */
    @Override
    public List<Move> getLegalMoves(MachineState state, Role role)
            throws MoveDefinitionException {
        markbases(state, getPropNet());
        Set<Proposition> legals = getPropNet().getLegalPropositions().get(role);
        List<Move> actions = new ArrayList<Move>();
        for (Proposition p : legals) {
        	if (propmarkp(p)) {
        		actions.add(getMoveFromProposition(p));
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
        markactions(moves, getPropNet());
        markbases(state, getPropNet());
        Map<GdlSentence, Proposition> bases = getPropNet().getBasePropositions();
        Set<GdlSentence> nextState = new HashSet<GdlSentence>();
        for (GdlSentence gs : bases.keySet()) {
        	Component cp = bases.get(gs).getSingleInput();
        	if(propmarkp(cp.getSingleInput())) {
        		nextState.add(gs);
        	}
        }
        return new MachineState(nextState);
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
        List<Component> components = new ArrayList<Component>(getPropNet().getComponents());

        // All of the propositions in the PropNet.
        List<Proposition> propositions = new ArrayList<Proposition>(getPropNet().getPropositions());

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
        for (Proposition p : getPropNet().getBasePropositions().values())
        {
            p.setValue(p.getSingleInput().getValue());
            if (p.getValue())
            {
                contents.add(p.getName());
            }
        }
        return new MachineState(contents);
    }

	private void p(String word) { System.out.println(word); }

	/************** marking functions ********************/
    private void markbases (MachineState state, PropNet propNet) {
    	clearpropnet(propNet); // sets everything to false
    	Map<GdlSentence, Proposition> props = propNet.getBasePropositions();
    	Set<GdlSentence> stateContents = state.getContents();
    	for (GdlSentence gs : stateContents) {
    		props.get(gs).setValue(true);
    	}
    }

   private void markactions (List<Move> moves, PropNet propNet) {
	 Map<GdlSentence, Proposition> props = propNet.getInputPropositions();
	 // TODO: is this the right way
	 List<GdlSentence> toDo = toDoes(moves);
	 for (GdlSentence gs : props.keySet()) {
		 props.get(gs).setValue(false);
	 }
	 for (GdlSentence move : toDo) {
		 props.get(move).setValue(true);
	 }
   }

   private void clearpropnet (PropNet propNet) {
	   propNet.setInitProposition(false);
	   Map<GdlSentence, Proposition> props = propNet.getBasePropositions();
	   for (GdlSentence gs : props.keySet()) {
		   props.get(gs).setValue(false);
	   }
   }

   /***************** propagating view **********************/
   public boolean propmarkp (Component cp) {
	   cp.setValueIsCorrect(true);// optimized
	   if (cp.getInputs().size() == 1 && cp.getSingleInput() instanceof Transition) { // base
//		   p("base");
		   return cp.getValue();
	   } else if (cp instanceof Not) { // negation
		   return propmarknegation(cp);
	   } else if (cp instanceof Or) { // disjunction
		   return propmarkdisjunction(cp);
	   } else if (cp instanceof And) { // conjunction
		   return propmarkconjunction(cp);
	   } else if (cp instanceof Constant) { // constant
//		   p("constant");
		   return cp.getValue();
	   } else if (((Proposition) cp).getName().getName().getValue().equals("does")) { // input
//		   p("input");
		   return cp.getValue();
	   } else if((((Proposition) cp).getName().getName().getValue().toUpperCase().equals("INIT"))) { // init
//		   p("init");
		   return cp.getValue();
	   }
//	   else if(((Proposition) cp).getName().getName().getValue().equals("legal")) { // legal
//		   p("legal");
//		   return propmarkp(cp.getSingleInput());
//	   }
//	   else if(((Proposition) cp).getName().getName().getValue().equals("terminal")) {
//		   p("terminal");
//		   return propmarkp(cp.getSingleInput());
//	   }
//	   else if (((Proposition) cp).getName().getName().getValue().equals("goal")) {
//		   p("goal");
//		  return propmarkp(cp.getSingleInput());
//	   }
	   else if (cp.getInputs().size() == 1) { // view
//		   p("view");
		   return propmarkp(cp.getSingleInput());
	   } else {
//		   p("false");
		   return false;
	   }
	}

	public boolean propmarknegation (Component cp) {
		return !propmarkp(cp.getSingleInput());
	}

	private boolean propmarkconjunction (Component cp) {
		Set<Component> sources = cp.getInputs();
		for (Component source : sources) {
			if (!propmarkp(source)) return false;
		}
		return true;
   }

	private boolean propmarkdisjunction (Component cp) {
		Set<Component> sources = cp.getInputs();
		for (Component source : sources) {
			if (propmarkp(source)) return true;
		}
		return false;
	}

<<<<<<< HEAD
	public PropNet getPropNet() {
		return propNet;
	}

	public void setPropNet(PropNet propNet) {
		this.propNet = propNet;
=======
	@Override
	public MachineState performPropNetDepthCharge(MachineState state,
			int[] theDepth) throws TransitionDefinitionException,
			MoveDefinitionException {
		return performDepthCharge(state, theDepth);
	}

	@Override
	public List<Move> getBestMoves() {
		return null;
>>>>>>> optimized-propnet-oluwasanya
	}
}