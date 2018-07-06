package controllers.limitdepthfirst;

import java.util.ArrayList;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;


public class Agent extends AbstractPlayer {

    public final static int LIMIT_DEPTH = 5;
    protected ArrayList<Types.ACTIONS> actions = new ArrayList<>();
    protected ArrayList<StateObservation> states = new ArrayList<>();
    protected ArrayList<Types.ACTIONS> bestActions = new ArrayList<>();
    int depth;
    int step;
    boolean finish;
    double cost;

    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        depth = 0;
        finish = false;
        step = 0;
        cost = 10000;
        //System.out.print("here\n");
    }

    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        ArrayList<Observation>[] movingPositions = stateObs.getMovablePositions();
        //System.out.print(movingPositions[0].size());
        cost = 10000;
        if(finish == false){ //don't search the result, just search to limit depth
            states.clear();
            actions.clear();
            bestActions .clear();
            ldfs(stateObs, 0);
            //System.out.println(actions.size());
			//System.out.println(bestActions .size());
			//System.out.println(bestActions .get(0));

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return bestActions.get(0);
        }
        else
            step++;
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return actions.get(step - 1);
    }

    public double heuristic(StateObservation stateObs){
        double judge;
        ArrayList<Observation>[] fixedPositions = stateObs.getImmovablePositions();
        ArrayList<Observation>[] movingPositions = stateObs.getMovablePositions();
        Vector2d goalpos = fixedPositions[1].get(0).position; //目标的坐标
        Vector2d keypos = movingPositions[0].get(0).position; //钥匙的坐标
        Vector2d currentpos = stateObs.getAvatarPosition();
        //System.out.print(movingPositions[0].size());
        //System.out.print("\n");
        if(movingPositions[0].size() == 1) { //without key, the judge is the distance from goal and key
            judge = currentpos.dist(keypos) + currentpos.dist(goalpos);
        }
        else //with key, the judge is the distance from goal
            judge = currentpos.dist(goalpos);
        return judge;
    }

    public void ldfs(StateObservation so, int depth){
        ArrayList<Types.ACTIONS> currentActions = so.getAvailableActions();
        states.add(so);
        for(int i = 0; i < currentActions.size(); i++){
            boolean flag = true;
            StateObservation stCopy = so.copy();
            stCopy.advance(currentActions.get(i));
            for(int j = 0; j < states.size(); j++){
                if(stCopy.equalPosition(states.get(j))){ //avoid retreats
                    flag = false;
                    break;
                }
            }
            if(stCopy.getGameWinner() == Types.WINNER.PLAYER_WINS){
                //System.out.print("finish");
                //System.out.print(finish);
                finish = true;
                actions.add(currentActions.get(i));
                bestActions .clear();
                for(int j = 0; j<actions.size(); j++)
                    bestActions.add(actions.get(j));
                break;
            }
            if(flag == true && depth == LIMIT_DEPTH){ //achieve the limit depth, find the best in current states
                //System.out.print("limit depth\n");
                if(heuristic(stCopy) < cost){
                    cost = heuristic(stCopy);
                    bestActions .clear();
                    for(int j = 0; j < actions.size(); j++)
                        bestActions.add(actions.get(j));
                }
                flag = false;
            }

            //System.out.print(flag);
            if(flag){
                actions.add(currentActions.get(i));
                ldfs(stCopy, depth + 1);
            }
        }
        if(!finish) {
            if(!actions.isEmpty() && !states.isEmpty()) {
                actions.remove(actions.size() - 1);
                states.remove(states.size() - 1);
            }
        }
    }
}
