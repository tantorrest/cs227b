package org.ggp.base.player.gamer.mygamers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class LegalPlayer extends SampleGamer {

	private List<Set<GdlSentence>> seenStates = new ArrayList<Set<GdlSentence>>();

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine sm = getStateMachine();
		long start = System.currentTimeMillis();
		MachineState ms = getCurrentState();
		seenStates.add(ms.getContents());
		System.out.println("Seen: " + seenStates.toString());
		Role r = getRole();
		long stop = System.currentTimeMillis();
		System.out.println(ms.getContents());
		List<Move> possMoves = sm.findLegals(r, ms);
		for (int i=0; i < possMoves.size(); i++){
			List<Move> moves = new ArrayList<Move>();
			moves.add(possMoves.get(i));
			MachineState nextState = sm.getNextState(ms, moves);
			List<GdlSentence> nextContents = new ArrayList<GdlSentence>(nextState.getContents());
			Set<GdlSentence> nextContentsSet = nextState.getContents();
			boolean seen = true;
			for (int j=0; j < seenStates.size(); j++){
				seen = true;
				int differentCount = 0;
				Set<GdlSentence> seenContents = seenStates.get(j);
				List<GdlSentence> seenContentsList = new ArrayList<GdlSentence>(seenStates.get(j));
				for (int k=0; k < nextContents.size(); k++){
					if (!seenContents.contains(nextContents.get(k))){
						differentCount++;
						if (differentCount > 1){
							seen = false;
							System.out.println("unseen");
							break;
						}
					}
				}
				if (seen){
					differentCount = 0;
					for (int g=0; g < seenContentsList.size(); g++){
						if (!nextContentsSet.contains(seenContentsList.get(g))){
							differentCount++;
							if (differentCount > 1){
								seen = false;
								System.out.println("unseen");
								break;
							}
						}
					}
				}

				System.out.println(differentCount + " " + seen);
				if (seen){
					break;
				}
			}
			if (!seen){
				System.out.println("found unseen");
				System.out.println(nextContents.toString());
				notifyObservers(new GamerSelectedMoveEvent(sm.findLegals(r, ms), possMoves.get(i), stop - start));
				return possMoves.get(i);
			}
		}
		System.out.println("no good moves");
		notifyObservers(new GamerSelectedMoveEvent(sm.findLegals(r, ms), sm.findLegalx(r, ms), stop - start));
		return sm.findLegalx(r, ms);
	}

}
