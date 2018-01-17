import ilog.concert.IloException;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.stat.*;
import java.util.ArrayList;
import java.util.Arrays;
import com.csvreader.*;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

public class main {
    public static double eps = 10e-8;
    public static void main (String [] args){

        String outputfolder = args[0];
        int nrun = Integer.parseInt(args[1]);
        int nstat = Integer.parseInt(args[2]);
        int nupperruns = Integer.parseInt(args[3]);
        int nvarsvariation = Integer.parseInt(args[4]);
        int statsnumber = Integer.parseInt(args[5]);

        boolean allbinaries = true;

        /*
        int nrun = 100;
        int nstat = 4;
        int nupperruns = 10;
        int nvarsvariation = 4;
        int statsnumber = 2;
        */

        try {
            main.upperRuns(nrun, nstat, nupperruns, nvarsvariation, statsnumber, outputfolder, allbinaries);
        }catch (IOException e){
            System.out.println("ERROR something is wrong with the Input/Output");
            e.printStackTrace();
        }

    }
    public static void upperRuns(int nrun, int nstat, int nupperruns,int nvarsvariation,int statsnumber,String outputfolder, boolean allbinaries) throws IOException{
        double[][] stats = new double[statsnumber*nstat][nupperruns*nvarsvariation];
        String output = outputfolder + File.separator+"Stats.csv";
        CsvWriter outputWriter = new CsvWriter(new FileWriter(output, false), ',');
        outputWriter.write("NVars");
        outputWriter.write("NUpperRun");
        outputWriter.write("MeanFPA");
        outputWriter.write("MeanMAV");
        outputWriter.write("MeanCheck");
        outputWriter.write("StdDevFPA");
        outputWriter.write("StdDevMAV");
        outputWriter.write("StdDevCheck");
        outputWriter.write("MaxFPA");
        outputWriter.write("MaxMAV");
        outputWriter.write("MaxCheck");
        outputWriter.write("MinFPA");
        outputWriter.write("MinMAV");
        outputWriter.write("MinCheck");
        outputWriter.endRecord();
        for(int h = 1; h < nvarsvariation+1; h++) {
            for(int k = 0; k < nupperruns; k++) {

                double[][] res = new double[nstat][nrun];


                String outputkh = outputfolder + File.separator+"SingleRun_"+k+"_"+(h*10)+".csv";
                CsvWriter outputWriterkh = new CsvWriter(new FileWriter(outputkh, false), ',');
                outputWriterkh.write("FPA");
                outputWriterkh.write("MAV");
                outputWriterkh.write("Check");
                outputWriterkh.endRecord();

                for (int i = 0; i < nrun; i++) {
                    //System.out.println("Iter:" + i);
                    long[] ret = main.run(allbinaries, h*10, nstat);
                    for (int j = 0; j < nstat; j++) {
                        //System.out.println("-> "+j+nstat);
                        //System.out.println(ret[j]);
                        res[j][i] = (double) ret[j];
                        outputWriterkh.write(ret[j]+"");
                    }
                    outputWriterkh.endRecord();
                }

                outputWriterkh.close();

                outputWriter.write(h*10+"");
                outputWriter.write(k+"");
                for(int j = 0; j < statsnumber; j++){
                    for(int s = 0; s < nstat ; s++){
                        if (j == 0) {
                            stats[j*nstat+s][(h-1)*nupperruns+k] = StatUtils.mean(res[s]);
                            outputWriter.write(""+StatUtils.mean(res[s]));
                        }
                        if (j == 1) {
                            stats[j*nstat+s][(h-1)*nupperruns+k] = Math.sqrt(StatUtils.variance(res[s]));
                            outputWriter.write(""+Math.sqrt(StatUtils.variance(res[s])));
                        }
                        if (j == 2) {
                            stats[j*nstat+s][(h-1)*nupperruns+k] = StatUtils.max(res[s]);
                            outputWriter.write(""+StatUtils.max(res[s]));
                        }
                        if (j == 3) {
                            stats[j*nstat+s][(h-1)*nupperruns+k] = StatUtils.min(res[s]);
                            outputWriter.write(""+StatUtils.min(res[s]));
                        }
                    }
                }
                outputWriter.endRecord();
            }
        }
        outputWriter.close();
    }

    public static long[] run(boolean allbinaries, int nsize, int nstat){
        long [] ret = new long [nstat];
        ArrayList<Object> param = main.RANDOMINSIDEABOX(allbinaries, nsize);

        double [][] objectives =  (double[][]) param.get(0);
        double [][] matrixA = (double[][]) param.get(1);
        double [] b = (double[]) param.get(2);
        boolean [] binary = (boolean[]) param.get(3);
        try{
            CleverFrontierAlg alg = new CleverFrontierAlg(norm.L1_norm);
            long init = System.currentTimeMillis();
            List<Map<String,Double>> Y = alg.solve(objectives, matrixA, b, binary);
            long end = System.currentTimeMillis();
            CleverFrontierAlg.printFrontierCsv(Y);

            long start = 0, finish = 0;
            if(allbinaries) {
                MavrotasAlgorithm malg = new MavrotasAlgorithm();
                start = System.currentTimeMillis();
                List<Map<String, Double>> MY = malg.solve(objectives, matrixA, b, binary);
                finish = System.currentTimeMillis();
                MavrotasAlgorithm.printFrontierCsv(MY);
                ret[2] = (AlgUtils.compare(Y,MY)? 1 : 0);
            }

            ret[0] = end - init;
            ret[1] = finish - start;
            //System.out.println("Total time Partitioner(s): "+(end-init));
            //System.out.println("Total time Mavrotas   (s): "+(finish-start));
        }catch(IloException e){
            e.printStackTrace();
        }
        return ret;
    }

    public static ArrayList<Object> RANDOMINSIDEABOX(boolean allbinaries, int nsize){
        int m = 3;
        int n = nsize;//nsize;// (int) Math.round(Math.random()*200);
        int p = 10*n;//(int) Math.round(Math.random()*200);
        //System.out.println(m+" "+n+" "+p);


        double [] [] matrix = new double [p+2*n] [n];
        double [] b = new double [p+2*n];
        double [][] o = new double [m][n];
        boolean [] binary = new boolean [n];

        for(int i = 0; i<p; i++){
            for(int j = 0; j<n; j++)
                matrix[i][j] = Math.round(Math.random()*21-1);
        }
        for(int i = 0; i<p; i++){
            b[i] =Math.round(10 - Math.random()*5);
        }

        double val = Math.round(1*L1NormBounder(matrix, b));

        for(int i = p; i<(p+2*n); i++){
            for(int j = 0; j<n; j++)
                if(i==p+j) {
                    matrix[i][j] = -1;
                }else if( i == p+n+j){
                    matrix[i][j] = 1;
                }
        }
        for(int i = p; i<(p+2*n); i++){
            if(i<p+n) {
                b[i] = -val;
            }else{
                b[i] = -val;
            }
        }
        //////////////////////////////////////





        for(int i = 0; i<m; i++){
            for(int j = 0; j<n; j++)
                o[i][j] = Math.round(Math.random()*100);//-50;
        }

        if(allbinaries){
           for(int i = 0 ; i < n ; i++){
               binary[i] = true;
           }
        }else {
            int count = 0;
            loop:
            while (true) {
                for (int i = 0; i < n; i++) {
                    if (Math.random() > 0.5d && !binary[i]) {
                        binary[i] = true;
                        count++;
                    }
                    if (count >= n / 2) {
                        break loop;
                    }

                }
            }
        }

        ArrayList<Object> ret = new ArrayList<>();
        ret.add(o);
        ret.add(matrix);
        ret.add(b);
        ret.add(binary);
        return ret;
    }


    public static double L1NormBounder( double[][] a, double [] b){
        double ret = 10e-200;
        for(int i = 0; i<a.length;i++){
            if(Math.abs(b[i]) > eps) {
                double maxb = b[i];
                for (int j = 0; j < a[0].length; j++) {
                    if (Math.abs(a[i][j]) > eps) {
                        double maxa = a[i][j];
                        double val = Math.abs(maxb/maxa);
                        if( val > ret){
                            ret = val;
                        }
                    }
                }
            }
        }
        return ret;
    }
    public static void printMatrix(String name, double[][] a){
        System.out.println("Matrix Printer-- Matrix Name: "+name);
        for(int i = 0; i<a.length;i++){
            for(int j = 0; j<a[0].length; j++){
                System.out.print(a[i][j]+" ");
            }
            System.out.println();
        }
    }
    public static void printMatrix(String name, double[] a){
        System.out.println("Matrix Printer-- Matrix Name: "+name);
        for(int i = 0; i<a.length;i++){
            System.out.println(a[i]+", ");
        }
    }

    public static ArrayList<Object> BOX(){
        int m = 2;
        int n = 3;// (int) Math.round(Math.random()*200);
        int p = 2*n;//(int) Math.round(Math.random()*200);
        System.out.println(m+" "+n+" "+p);


        double [] [] matrix = new double [p] [n];
        double [] b = new double [p];
        double [][] o = new double [m][n];

        for(int i = 0; i<p; i++){
            for(int j = 0; j<n; j++)
                if(i==j) {
                    matrix[i][j] = -1;
                }else if( i == n+j){
                    matrix[i][j] = 1;
                }
        }
        for(int i = 0; i<p; i++){
            if(i<n) {
                b[i] = -1;
            }else{
                b[i] = 2;
            }
        }
        for(int i = 0; i<m; i++){
            for(int j = 0; j<n; j++) {
                o[0][j] = 1;
                o[1][0] = -1;
                o[1][1] = 0;
                o[1][2] = 0;
            }
        }

        ArrayList<Object> ret = new ArrayList<>();
        ret.add(o);
        ret.add(matrix);
        ret.add(b);
        return ret;
    }
}
