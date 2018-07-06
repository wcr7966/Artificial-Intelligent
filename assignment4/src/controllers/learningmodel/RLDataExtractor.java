/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controllers.learningmodel;

import tools.*;
import core.game.Observation;
import core.game.StateObservation;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Observable;

import ontology.Types;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author yuy
 */
public class RLDataExtractor {
    public FileWriter filewriter;
    public static Instances s_datasetHeader = datasetHeader();
    
    public RLDataExtractor(String filename) throws Exception{
        
        filewriter = new FileWriter(filename+".arff");
        filewriter.write(s_datasetHeader.toString());
        /*
                // ARFF File header
        filewriter.write("@RELATION AliensData\n");
        // Each row denotes the feature attribute
        // In this demo, the features have four dimensions.
        filewriter.write("@ATTRIBUTE gameScore  NUMERIC\n");
        filewriter.write("@ATTRIBUTE avatarSpeed  NUMERIC\n");
        filewriter.write("@ATTRIBUTE avatarHealthPoints NUMERIC\n");
        filewriter.write("@ATTRIBUTE avatarType NUMERIC\n");
        // objects
        for(int y=0; y<14; y++)
            for(int x=0; x<32; x++)
                filewriter.write("@ATTRIBUTE object_at_position_x=" + x + "_y=" + y + " NUMERIC\n");
        // The last row of the ARFF header stands for the classes
        filewriter.write("@ATTRIBUTE Class {0,1,2}\n");
        // The data will recorded in the following.
        filewriter.write("@Data\n");*/
        
    }
    
    public static Instance makeInstance(double[] features, int action, double reward){
        features[s_datasetHeader.numAttributes() - 2] = action;
        features[s_datasetHeader.numAttributes() - 1] = reward;
        Instance ins = new Instance(1, features);
        ins.setDataset(s_datasetHeader);
        return ins;
    }

    public static double nearest(StateObservation obs, ArrayList<Observation> l){
        Vector2d avatarPos = obs.getAvatarPosition();
        ArrayList<Observation> grid[][]=obs.getObservationGrid();
        int []x = {0, 0, -1, 1}; // up, down, left, right
        int []y = {-1, 1, 0, 0};

        Vector2d[] dir = new Vector2d[4];
        for(int i=0; i<4; i++){
            dir[i] = new Vector2d(avatarPos);
            dir[i].set(avatarPos.x + x[i], avatarPos.y + y[i]);
        }

        double []min4 = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        boolean [] canGo = {true, true, true, true};
        for(int i = 0; i < 4; i++){
            int xi = (int)dir[i].x/20;
            int yi = (int)dir[i].y/20;
            if(grid[xi][yi].size() == 1 && grid[xi][yi].get(0).itype == 0)
                canGo[i] = false;
        }

        for(Observation o:l){
            for(int i=0; i<4; i++){
                if(canGo[i]){
                    if(min4[i] > o.position.dist(dir[i]))
                        min4[i] = o.position.dist(dir[i]);
                }
            }
        }

        double min = Double.MAX_VALUE;
        double minIndex = -1;
        for(int i=0; i<4; i++) {
            if (min > min4[i]) {
                minIndex = i;
                min = min4[i];
            }
        }
        return minIndex;
    }

    public static boolean isNearGhosts(StateObservation obs,ArrayList<Observation> allGhost){
        Vector2d avatarPos = obs.getAvatarPosition();
        double minDist = Double.MAX_VALUE;
        for(Observation o:allGhost) {
            double d = o.position.dist(avatarPos);
            if(minDist > d)
                minDist = d;
        }
        // in 10 step
        double step = 10;
        if(minDist < 20 * step)
            return true;
        else
            return false;
    }

    public static double direction(StateObservation obs, Observation l){
        Vector2d avatarPos = obs.getAvatarPosition();
        double delta_x = l.position.x - avatarPos.x;
        double delta_y = l.position.y - avatarPos.y;
        double dist = l.position.dist(avatarPos);
        double a_theta = Math.acos(delta_x/dist);
        if(delta_y<0)
            return a_theta + Math.PI;
        else return a_theta;
    }

    public static double[] featureExtract(StateObservation obs){

        //double[] feature = new double[883];  // 868 + 4 + 1(action) + 1(Q)
        double[] feature = new double[s_datasetHeader.numAttributes()];

        /*
        // 448 locations
        int[][] map = new int[28][31];
        // Extract features
        LinkedList<Observation> allobj = new LinkedList<>();
        if( obs.getImmovablePositions()!=null )
            for(ArrayList<Observation> l : obs.getImmovablePositions()) allobj.addAll(l);
        if( obs.getMovablePositions()!=null )
            for(ArrayList<Observation> l : obs.getMovablePositions()) allobj.addAll(l);
        if( obs.getNPCPositions()!=null )
            for(ArrayList<Observation> l : obs.getNPCPositions()) allobj.addAll(l);
        
        for(Observation o : allobj){
            Vector2d p = o.position;
            int x = (int)(p.x/20); //squre size is 20 for pacman
            int y= (int)(p.y/20);
            map[x][y] = o.itype;
        }
        for(int y=0; y<31; y++)
            for(int x=0; x<28; x++)
                feature[y*28+x] = map[x][y];
        */

        // 4 states
        feature[0] = obs.getGameTick();
        feature[1] = obs.getAvatarSpeed();
        feature[2] = obs.getAvatarHealthPoints();
        feature[3] = obs.getAvatarType();


        if( obs.getImmovablePositions()!=null ) {
            for (ArrayList<Observation> l : obs.getImmovablePositions()) {
                if(l.size() == 0)
                    continue;
                // 小的能量
                else if (l.get(0).itype == 4) {
                    feature[4] = nearest(obs,l);
                }
                // 蘑菇
                else if (l.get(0).itype == 3) {
                    feature[5] = nearest(obs,l);
                }
            }
        }

        // 大能量
        if( obs.getResourcesPositions()!= null ) {
            for (ArrayList<Observation> l : obs.getResourcesPositions()){
                feature[6] = nearest(obs,l);
            }
        }

        //Ghosts
        ArrayList<Observation> allGhost = new ArrayList<>();
        if( obs.getPortalsPositions()!=null ) {
            for (ArrayList<Observation> l : obs.getPortalsPositions()){
                allGhost.addAll(l);
            }
        }
        feature[7] = nearest(obs,allGhost);

        feature[8] = isNearGhosts(obs,allGhost)?1:0;

        int i = 9;
        for(Observation o: allGhost) {
            feature[i] = direction(obs, o);
            i++;
        }

        return feature;
    }
    
    public static Instances datasetHeader(){
        
        if (s_datasetHeader!=null)
            return s_datasetHeader;
        
        FastVector attInfo = new FastVector();
        // 448 locations
        /*for(int y=0; y<28; y++){
            for(int x=0; x<31; x++){
                Attribute att = new Attribute("object_at_position_x=" + x + "_y=" + y);
                attInfo.addElement(att);
            }
        }
        */
        Attribute att = new Attribute("GameTick" ); attInfo.addElement(att);
        att = new Attribute("AvatarSpeed" ); attInfo.addElement(att);
        att = new Attribute("AvatarHealthPoints" ); attInfo.addElement(att);
        att = new Attribute("AvatarType" ); attInfo.addElement(att);

        FastVector nearestLittle = new FastVector();
        nearestLittle.addElement("0");
        nearestLittle.addElement("1");
        nearestLittle.addElement("2");
        nearestLittle.addElement("3");
        att = new Attribute("nearestLittle", nearestLittle);
        attInfo.addElement(att);

        FastVector nearestMushroom = new FastVector();
        nearestMushroom.addElement("0");
        nearestMushroom.addElement("1");
        nearestMushroom.addElement("2");
        nearestMushroom.addElement("3");
        att = new Attribute("nearestMushroom", nearestMushroom);
        attInfo.addElement(att);

        FastVector nearestPower = new FastVector();
        nearestPower.addElement("0");
        nearestPower.addElement("1");
        nearestPower.addElement("2");
        nearestPower.addElement("3");
        att = new Attribute("nearestPower", nearestPower);
        attInfo.addElement(att);

        FastVector nearestGhost = new FastVector();
        nearestGhost.addElement("0");
        nearestGhost.addElement("1");
        nearestGhost.addElement("2");
        nearestGhost.addElement("3");
        att = new Attribute("nearestGhost", nearestGhost);
        attInfo.addElement(att);

        FastVector isNear = new FastVector();
        isNear.addElement("0");
        isNear.addElement("1");
        att = new Attribute("isNear", isNear);
        attInfo.addElement(att);

        for(int i = 0; i< 4; i++) {
            att = new Attribute("ghost" + i + "direct");
            attInfo.addElement(att);
        }

        //action
        FastVector actions = new FastVector();
        actions.addElement("0");
        actions.addElement("1");
        actions.addElement("2");
        actions.addElement("3");
        att = new Attribute("actions", actions);
        attInfo.addElement(att);

        // Q value
        att = new Attribute("Qvalue");
        attInfo.addElement(att);
        
        Instances instances = new Instances("PacmanQdata", attInfo, 0);
        instances.setClassIndex( instances.numAttributes() - 1);
        
        return instances;
    }
    
}
