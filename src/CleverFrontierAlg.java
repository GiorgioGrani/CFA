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
        return this.solveWithoutTimeStop(objectives, matrixA, b, binary);
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

