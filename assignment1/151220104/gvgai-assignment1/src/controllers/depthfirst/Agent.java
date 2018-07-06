package controllers.depthfirst;

import java.awt.Graphics2D;
import java.util.ArrayList;
//jmport java.lang.Thread.sleep;
import java.lang.InterruptedException;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

public class Agent extends AbstractPlayer {

    protected ArrayList<StateObservation> states = new ArrayList<>(); //all of the states in this game
    protected ArrayList<Types.ACTIONS> actions = new ArrayList<>(); //the selected actions
    boolean finish; // the state of game state - 0: not finish 1: finish
    int step;

    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        finish = false;
        step = 0;
    }

    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        if(step == 0)
            dfs(stateObs);
        step++;
        //System.out.println(count);
        //System.out.println(actions.size());
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return actions.get(step - 1);
    }

    public void dfs(StateObservation so){
        //ArrayList<Observation>[] movingPositions = so.getMovablePositions();
        //System.out.print("\n");
        //System.out.print(movingPositions[0].size());
        ArrayList<Types.ACTIONS> currentActions = so.getAvailableActions(); //all of the available actions in current state
        states.add(so); //add current state
        for(int i =0; i < currentActions.size(); i++) {
            boolean flag = true;
            if (finish)
                break;
            StateObservation stCopy = so.copy();
            stCopy.advance(currentActions.get(i));
            for (int j = 0; j < states.size(); j++) {
                if (stCopy.equalPosition(states.get(j))) { //avoid retreats
                    flag = false;
                    break;
                }
            }
            if (stCopy.getGameWinner() == Types.WINNER.PLAYER_LOSES)
                flag = false;
            else if (stCopy.getGameWinner() == Types.WINNER.PLAYER_WINS)
                finish = true;

            if (flag) {
                actions.add(currentActions.get(i));
                dfs(stCopy);
            }
        }
        //the simulative action is not right, back to last state
        if(!finish) {
            if(!actions.isEmpty() && !states.isEmpty()) {
                actions.remove(actions.size() - 1);
                states.remove(states.size() - 1);
            }
        }
    }
}
