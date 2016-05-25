package org.ggp.base.player.gamer.mygamers;

import java.util.Set;

import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.implementation.propnet.FactorPropNetStateMachine;

public class FactorPlayer extends OptimizedPropnetPlayer {
	@Override
	public StateMachine getInitialStateMachine(){
		FactorPropNetStateMachine sm =  new FactorPropNetStateMachine();
    	sm.initialize(getMatch().getGame().getRules());
    	//List<Gdl> newRules = sm.independentFactor();
//    	Set<Component> usedComponents = sm.componentFactoring();
    	//System.out.println(newRules.toString());
    	//SamplePropNetStateMachine fm = new SamplePropNetStateMachine();
    	//fm.initialize(newRules);-
//
//    	StringBuilder sb = new StringBuilder();

//        sb.append("digraph propNet\n{\n");
//        for ( Component component : usedComponents )
//        {
//            sb.append("\t" + component.toString() + "\n");
//        }
//        sb.append("}");
//        p("FACTORED PROPNET!!!: ");
//        p(factoredPN.toString());
//        sm.setPropNet(factoredPN);
    	Set<Component> usedComponents = sm.markFactors();

//    	PropNet factoredPN = new PropNet(sm.getRoles(), usedComponents);
//    	 p("FACTORED PROPNET!!!: ");
//       p(factoredPN.toString());
//       sm.setPropNet(factoredPN);


    	return sm;
	}


public void p(String x){ System.out.println(x);}
}






