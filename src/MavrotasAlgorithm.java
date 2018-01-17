import ilog.concert.*;
import ilog.cplex.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.TreeMap;


public class MavrotasAlgorithm {

    public List<Map<String, Double>> solve(double [][] objectives, double [][] matrixA, double[] b, boolean[] binary) throws IloException{
        return this.solveWithoutTimeStop(objectives, matrixA, b, binary);
    }

    public List<Map<String, Double>> solveWithoutTimeStop(double [][] objectives, double [][] matrixA, double[] b, boolean [] binary) throws IloException{
        
        //initializzation
        int ID = 0;
        ProblemMavrotas root = new ProblemMavrotas(0, objectives, matrixA, b,binary);
        List<ProblemMavrotas> D = new ArrayList<>();
        D.add(root);
        List<Map<String, Double>> Y = new ArrayList<>();

        //alg
        while( D.size() > 0 ){
            //if(ID%100 == 0) System.out.println(ID);
            ProblemMavrotas p = D.get(D.size()-1); // todo this is only depthfirst
            boolean isSolvable = p.solve();
           // System.out.println(" ID "+p.getID()+"  Is it solvable? "+isSolvable);
            //MavrotasAlgorithm.printSingleCsv(p.getIdealvector());
            if( isSolvable){
                D.remove(p);
                //System.out.println(Fathomed(Y,p.getIdealvector())+"   mavv");
                if(!Fathomed(Y,p.getIdealvector())) {
                    if(p.levelReach()) {
                        //System.out.println("sono entrato");
                        boolean val = MavrotasAlgorithm.clear(Y,p.getIdealvector());
                        Y.add(p.getIdealvector());
                        //MavrotasAlgorithm.printFrontier(Y);
                    }else{
                        Map<String, IloAddable> pool = p.branchOn();
                        for (String s : pool.keySet()) {
                            ID++;
                            //System.out.println("ciao "+s);
                            ProblemMavrotas pk = new ProblemMavrotas(ID, p, s, pool.get(s));
                            D.add(pk);
                        }
                    }
                }

            }else{

                D.remove(p);
            }
            p.refresh();

        }

        return Y;
    }

    public  boolean Fathomed(List<Map<String, Double>> Y, Map<String, Double> ideal){

        for(Map<String, Double> y : Y){
            int size = y.size();
            int count = 0;
            for(String s : y.keySet()){
                double d = y.get(s).doubleValue();
                double d2 = ideal.get(s).doubleValue();

                if( d <= d2){
                    count ++;
                }
            }
            if(count >= size){
                return true;
            }
        }
        return false;
    }

    public static boolean clear(List<Map<String, Double>> Yor, Map<String, Double> ideal){
        List<Map<String, Double>> Y = new LinkedList<>();
        for(Map<String, Double> y : Yor){
            Y.add(y);
        }
        boolean ret = false;

        for(Map<String, Double> y : Y){
            int size = y.size();
            int count = 0;
            for(String s : y.keySet()){
                double d = y.get(s).doubleValue();
                double d2 = ideal.get(s).doubleValue();

                if( d >= d2){
                    count ++;
                }
            }
            if(count >= size){
                Yor.remove(y);
                ret = true;
            }
        }
        Yor = Y;
        return ret;
    }

    public static void printFrontier(List<Map<String, Double>> Y){
        int i = 1;
        System.out.println("---Optimal frontier Mavrotas---");
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
        System.out.println("---Optimal frontier Mavrotas---");
        for(Map<String, Double> y : Y ){
            for(String s : y.keySet()){
                System.out.print(y.get(s)+", ");
            }
            System.out.print("\n");
            i++;
        }
    }
    public static void printSingleCsv(Map<String, Double> y){
        int i = 1;
        System.out.println  ("ideal   ");

            for(String s : y.keySet()){
                System.out.print(y.get(s)+" ");
            }
            System.out.print("\n");
            i++;

    }

}
