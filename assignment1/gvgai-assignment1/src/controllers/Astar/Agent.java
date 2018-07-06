package controllers.Astar;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.*;

public class Agent extends AbstractPlayer {

    ArrayList<Types.ACTIONS> finalActions = new ArrayList<>();
    int step;
    Node root;

    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        step = 0;
        root = new Node(finalActions);
    }
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        if(step == 0)
            root.AStar(stateObs);
        step++;
        //System.out.print(step);
        //System.out.print(finalActions.size());
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return finalActions.get(finalActions.size() - step);
    }
}

class Node{
    StateObservation self;
    Node father;
    ArrayList<Types.ACTIONS> actions;
    Types.ACTIONS chosedAction;
    double alreadyCost;
    double goalCost;
    double allCost;
    Vector2d rootpos;

    public Node(ArrayList<Types.ACTIONS> actions){
        this.alreadyCost = 0;
        this.goalCost = 0;
        this.allCost = 0;
        this.actions = actions;
    }

    public Node(StateObservation stateObs, Vector2d rootPostion){
        this.self = stateObs.copy();
        this.rootpos = rootPostion;
        heuristic(stateObs);
    }


    Comparator<Node> compare = new Comparator<Node> () {
        public int compare(Node o1,Node o2) {
            return (int)(o1.allCost - o2.allCost);
        }
    };

    boolean finish = false;
    Queue<Node> openQueue = new PriorityQueue<>(compare);
    Queue<Node> closeQueue = new PriorityQueue<>(compare);


    public void AStar(StateObservation so){
       ArrayList<Observation>[] movingPositions = so.getMovablePositions();
       ArrayList<Observation>[] fixedPositions = so.getImmovablePositions();
       rootpos = so.getAvatarPosition();
       Node root = new Node(so, rootpos);
       openQueue.add(root);
       while(!finish){
           Node n = openQueue.poll();
           //System.out.print(n.self.getAvatarPosition());
           //System.out.print("\n");
           closeQueue.add(n);
           ArrayList<Types.ACTIONS> currentActions = so.getAvailableActions();
           for(int i = 0; i < currentActions.size(); i++){
               StateObservation stCopy = n.self.copy();
               stCopy.advance(currentActions.get(i));
               Node nextStep = new Node(stCopy, rootpos);
               nextStep.chosedAction = currentActions.get(i);


               boolean reachable = true;
               boolean open = false;
               boolean close = false;
               for(Node s: openQueue){
                   if(stCopy.equalPosition(s.self))
                       open = true;     //the node is in the openQueue
               }
               for(Node s: closeQueue){
                   if(stCopy.equalPosition(s.self))
                       close = true;    //the node is in the closeQueue
               }
               if(stCopy.getGameWinner() == Types.WINNER.PLAYER_LOSES)
                   reachable = false;
               if(stCopy.equalPosition(n.self))
                   reachable = false;   //the node is unreachable
               if(reachable && !close && !open){     //not in the openQueue
                   nextStep.father = n;
                   openQueue.add(nextStep);
               }
               else if(reachable && !close && open){    //in the openQueue
                   for(Node s: openQueue){
                       if(stCopy.equalPosition(s.self)){
                           if(s.allCost > nextStep.allCost){
                               s.father = n;
                               n.alreadyCost = nextStep.alreadyCost;
                               n.goalCost = nextStep.goalCost;
                               n.allCost = nextStep.allCost;
                           }
                       }
                   }
               }

               for(Node s: openQueue){
                   if(s.self.getGameWinner() == Types.WINNER.PLAYER_WINS) {
                       while (s != null) {
                           actions.add(s.chosedAction);
                           s = s.father;
                       }
                       finish = true;
                   }
               }
           }
       }
    }

    public double heuristic(StateObservation stateObs){
        if(stateObs.getGameWinner() == Types.WINNER.PLAYER_WINS)
            return 0;
        else if(stateObs.isGameOver())
            return -1;
        ArrayList<Observation>[] fixedPositions = stateObs.getImmovablePositions();
        ArrayList<Observation>[] movingPositions = stateObs.getMovablePositions();

        Vector2d currentpos = stateObs.getAvatarPosition(); //avatar position
        Vector2d goalpos = fixedPositions[fixedPositions.length - 1].get(0).position;   //door position

        //holes positions
        ArrayList<Observation> holes = null;
        if (fixedPositions.length > 2) holes = fixedPositions[fixedPositions.length - 2];

        //boxes positions
        ArrayList<Observation> boxes = null;
        if (movingPositions != null) boxes = movingPositions[movingPositions.length - 1];

        alreadyCost = currentpos.dist(rootpos);

        double keyCost =0, boxCost = 0;
        if(holes != null && boxes != null && holes.size() > 0 && boxes.size() > 0) {
            boxCost = currentpos.dist(boxes.get(boxes.size() - 1).position);
            if (stateObs.getAvatarType() != 4) { //do not get key
                Vector2d keypos = movingPositions[0].get(0).position;
                keyCost = 5 * currentpos.dist(keypos);
            }
        }
        goalCost = keyCost + boxCost;
        allCost = alreadyCost + goalCost;
        return allCost;
    }
}