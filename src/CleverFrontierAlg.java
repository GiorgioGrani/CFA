import ilog.concert.IloException;
import ilog.concert.IloAddable;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import java.util.concurrent.*;


public class CleverFrontierAlg {
    private long timemax = Long.MAX_VALUE;
    private branchingMode bmode = branchingMode.depth_first;
    private norm nmode = norm.L1_norm;

    public CleverFrontierAlg(){

    }
    public CleverFrontierAlg(branchingMode bmode){
        this.bmode = bmode;
    }
    public CleverFrontierAlg(norm nmode){
        this.nmode = nmode;
    }
    public CleverFrontierAlg(branchingMode bmode, norm nmode){
        this.bmode = bmode;
        this.nmode = nmode;
    }
    public CleverFrontierAlg(branchingMode bmode, norm nmode, long timemax){
        this.bmode = bmode;
        this.nmode = nmode;
        this.timemax = timemax;
    }

    public void setTimemax(long timemax){
        this.timemax = timemax;
    }

    public List<Map<String, Double>> solve(double [][] objectives, double [][] matrixA, double[] b, boolean[] binary) throws IloException{
        return this.solveWithoutTimeStopComparingVersion(objectives, matrixA, b, binary);
    }

    public List<Map<String, Double>> solveWithoutTimeStop(double [][] objectives, double [][] matrixA, double[] b, boolean [] binary) throws IloException{
        int nmodeint = 0;
        if(this.nmode == norm.L1_norm){
            nmodeint = 1;
        }else if(this.nmode == norm.L2_norm){
            nmodeint = 2;
        }else if(this.nmode == norm.Random_Weights){
            nmodeint = 3;
        }
        //initializzation
        int ID = 0;
        Problem root = new Problem(0, objectives, matrixA, b,binary, nmodeint);
        List<Problem> D = new ArrayList<>();
        D.add(root);
        List<Map<String, Double>> Y = new ArrayList<>();

        //alg
        while( D.size() > 0 && ID<=10000+2){
            //if(ID%100 == 0) System.out.println(ID);
            Problem p = D.get(D.size()-1); // todo this is only depthfirst
            boolean isSolvable = p.solve();
            if( isSolvable){
                List<Map<String, IloAddable>> pool = p.branchOnStrongly();
                Map<String, Double> point = p.getPareto();
                point.put("ID",(double) p.getID());
                point.put("ActualID",(double) ID/100d);
                Y.add(point);
                //System.out.println("---------------------------------ID "+ID);
                D.remove(p);
                for(Map<String, IloAddable> s: pool){
                    ID++;
                    Problem pk = new Problem(ID,p,s);
                    D.add(pk);
                }

            }else{

                D.remove(p);
            }
            p.refresh();

        }

        return Y;
    }

    public List<Map<String, Double>> solveWithoutTimeStopComparingVersion(double [][] objectives, double [][] matrixA, double[] b, boolean [] binary) throws IloException{
        int nmodeint = 0;
        if(this.nmode == norm.L1_norm){
            nmodeint = 1;
        }else if(this.nmode == norm.L2_norm){
            nmodeint = 2;
        }else if(this.nmode == norm.Random_Weights){
            nmodeint = 3;
        }
        //initializzation
        int ID = 0;
        Problem root = new Problem(0, objectives, matrixA, b,binary, nmodeint);
        List<List<Problem>> MasterD = new ArrayList<>();
        List<Problem> D = new ArrayList<>();
        List<Problem> NextD = new ArrayList<>();
        D.add(root);
        MasterD.add(D);
        List<Map<String, Double>> Y = new ArrayList<>();

        Problem p = D.get(0); // todo this is only depthfirst
        boolean isSolvable = p.solve();
        if( isSolvable){
            List<Map<String, IloAddable>> pool = p.branchOnStrongly();
            Map<String, Double> point = p.getPareto();
            point.put("ID",(double) p.getID());
            point.put("ActualID",(double) ID/100d);
            Y.add(point);
            //System.out.println("---------------------------------ID "+ID);
            D.remove(p);
            for(Map<String, IloAddable> s: pool){
                ID++;
                Problem pk = new Problem(ID,p,s);
                NextD.add(pk);
            }

        }
        
        MasterD.remove(D);
        if(NextD != null) {
            MasterD.add(NextD);
        }else{
            return Y;
        }

        //alg
        while( MasterD.size() > 0 && ID<=10000+2){
            //if(ID%100 == 0) System.out.println(ID);
            List<Problem> ActualD = MasterD.get(MasterD.size() - 1);
            Map<Problem,Map<String, Double>> LevelY = new TreeMap<>();

            for(Problem prob : ActualD) {
                if(prob.solve()) {
                    LevelY.put(prob, prob.getPareto());
                    prob.refresh();
                }
            }

            MasterD.remove(ActualD);

            List<Problem> NewD = compareLevel(LevelY);
            if(NewD != null) {
                MasterD.add(NewD);
            }else{
                continue;
            }

            for( Problem prob : NewD) {
                    List<Problem> NewLevel = new ArrayList<>();

                    List<Map<String, IloAddable>> pool = prob.branchOnStrongly();
                    Map<String, Double> point = prob.getPareto();
                    //point.put("ID", (double) prob.getID());
                    //point.put("ActualID", (double) ID / 100d);
                    Y.add(point);
                    //System.out.println("---------------------------------ID "+ID);
                    for (Map<String, IloAddable> s : pool) {
                        ID++;
                        Problem pk = new Problem(ID, prob, s);
                        NewLevel.add(pk);
                    }
                //prob.refresh();
                if( NewLevel != null) {
                    MasterD.add(NewLevel);
                }
            }

            MasterD.remove(NewD);

        }

        return Y;
    }

    public static List<Problem> compareLevel(Map<Problem,Map<String, Double>> LevelY){
        List<Problem> D = new ArrayList<>();

        for(Problem p : LevelY.keySet()){
            boolean score = true;

            loop:
            for(Problem q : LevelY.keySet()){
                if( p != q ){
                    if(AlgUtils.ParetoDominance(LevelY.get(q),LevelY.get(p))){
                        score = false;
                        break loop;
                    }
                }
            }

            if (score){
                D.add(p);
            }
        }

        return D;
    }

    public static void printFrontier(List<Map<String, Double>> Y){
        int i = 1;
        System.out.println("---Optimal frontier FPA---");
        for(Map<String, Double> y : Y ){
            System.out.print("Point: "+i+"    (");
            for(String s : y.keySet()){
                System.out.print(y.get(s)+" ");
            }
            System.out.print(")\n");
            i++;
        }
    }

    public static void printFrontierCsv(List<Map<String, Double>> Y){
        int i = 1;
        System.out.println("---Optimal frontier FPA---");
        for(Map<String, Double> y : Y ){
            for(String s : y.keySet()){
                System.out.print(y.get(s)+", ");
            }
            System.out.print("\n");
            i++;
        }
    }

}

