import ilog.concert.*;
import ilog.cplex.*;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.LinkedList;

public class Problem {

    private long ID;
    private long level;
    private static Map<String, IloAddable> constraints;
    private static Map<String, IloNumExpr> objectives;
    private IloAddable localObjective;
    private Map<String, IloAddable> localConstraints;
    private static Map<String, IloNumVar> vars;
    private static int norm;
    private static IloCplex cplex;
    private static IloModel model;
    private Map<String, Double> relsol;
    private Map<String, Double> pareto;
    private Map<String, Double> idealvector;
    private static Map<String, Double> val;
    public static double eps = 10e-8;


    public Problem(long ID,double [][] objectives, double [][] matrixA, double[] b,boolean[] binary, int norm) throws IloException {
        this.basicFilling(objectives, matrixA, b, binary);
        this.norm = norm;
        this.ID = ID;
    }

    public Problem(long ID,double [][] objectives, double [][] matrixA, double[] b, boolean[] binary) throws IloException {
        this.basicFilling(objectives, matrixA, b, binary);
        this.norm = 1;
        this.ID = ID;
    }

    public Problem(long ID, Problem p,Map<String, IloAddable> newconstraints) throws IloException {
        this.ID = ID;
        this.level = p.level + 1;
        Map<String , IloAddable> empty = new TreeMap<>();
        for(String s : p.localConstraints.keySet()){
            empty.put(s, p.localConstraints.get(s));
        }
        for(String conname: newconstraints.keySet()) {
            IloAddable con = newconstraints.get(conname);
            empty.put(conname, con);
        }
        this.localConstraints = empty;

    }

    public Problem(long ID, Problem p,String conname, IloAddable con) throws IloException {
        this.ID = ID;
        this.level = p.level + 1;
        Map<String , IloAddable> empty = new TreeMap<>();
        for(String s : p.localConstraints.keySet()){
            empty.put(s, p.localConstraints.get(s));
        }

        empty.put(conname, con);

        this.localConstraints = empty;

    }

    private void basicFilling(double [][] objectives, double [][] matrixA, double[] b, boolean[] binary) throws IloException{
        this.cplex = new IloCplex();
        this.vars  = new TreeMap<>();
        this.createVariable(objectives[0].length, binary);
        this.constraints = new TreeMap<>();
        this.createConstraints(matrixA, b);
        this.objectives = new TreeMap<>();
        this.val = new TreeMap<>();
        this.createObjectives(objectives);
        this.model = this.cplex.getModel();
        this.localConstraints = new TreeMap<>();
        this.level = 0;





    }

    private void createVariable(int n, boolean[] binary) throws IloException{
        for(int i = 0; i < n; i++){
            String s = "x"+(i+1);
            if(binary[i]){
                this.vars.put(s, this.cplex.boolVar());
            }else {
                this.vars.put(s, this.cplex.intVar(Integer.MIN_VALUE, Integer.MAX_VALUE));
            }
        }
    }

    private void createConstraints(double [][] matrixA, double[] b) throws IloException{
        for(int i = 0; i < matrixA.length; i++){
            IloLinearNumExpr expr = this.cplex.linearNumExpr();

            int j = 0;
            for(String s : this.vars.keySet()){
                expr.addTerm(matrixA[i][j], this.vars.get(s));
                j++;
            }
            IloAddable con = this.cplex.addGe(expr, b[i]);
            String s = "con"+i;
            this.constraints.put(s, con);
        }
    }

    private void createObjectives(double [][] objectives) throws IloException{
        for(int i = 0; i < objectives.length; i++){
            double minpos = 10e200;
           // double maxneg = -1e200;
            IloLinearNumExpr expr = this.cplex.linearNumExpr();
            //System.out.println();
            int j = 0;
            for(String s : this.vars.keySet()){
                expr.addTerm(objectives[i][j], this.vars.get(s));
                //System.out.print(objectives[i][j]+" ");
                if(Math.abs(objectives[i][j]) > Problem.eps && minpos > Math.abs(objectives[i][j])){
                    minpos = Math.abs(objectives[i][j]);
                }
                //if(objectives[i][j] < 0 && maxneg < objectives[i][j]){
                 //   maxneg = objectives[i][j];
                //}
                j++;
            }
            //todo capire perché non va bene col valore fornito
            minpos = 1;

            String s = "objective"+i;
            this.objectives.put(s, expr);
            this.val.put(s, minpos);
            //System.out.println(minpos+"<--<-<-");

            //double absmaxneg = Math.abs(maxneg);
//            if(absmaxneg < minpos){
//                double[] ins = new double[2];
//                ins[0] = -1d;
//                ins[1] =
//                this.val.put(s, );
//            }
        }
    }

    private boolean setObjective() throws IloException{
        boolean solvable = this.idealVectorFiller();
        if(!solvable){
            return false;
        }
        if(this.norm == 1){
            IloNumExpr expr = this.cplex.linearNumExpr();
            int h = 1;
            for(IloNumExpr obj : this.objectives.values()){

                IloLinearNumExpr objective = (IloLinearNumExpr) obj;
                expr = this.cplex.sum(expr, this.cplex.prod(h,objective));
                //h++;
            }
            this.localObjective = this.cplex.addMinimize(expr);
        }else if(this.norm == 2){
            IloNumExpr expr = this.cplex.numExpr();
            for(String s : this.objectives.keySet()){
                IloNumExpr bobj = (IloNumExpr) this.objectives.get(s);
                double val = this.idealvector.get(s);
                expr = this.cplex.sum(expr,
                        this.cplex.sum(this.cplex.prod( bobj,bobj),this.cplex.prod(bobj, -2d*val)));
            }
            this.localObjective = this.cplex.addMinimize(expr);
        }else if(this.norm == 3){
            IloNumExpr expr = this.cplex.linearNumExpr();

            for(IloNumExpr obj : this.objectives.values()){
                double h = Math.round(Math.random()*100)+1;
                IloLinearNumExpr objective = (IloLinearNumExpr) obj;
                expr = this.cplex.sum(expr, this.cplex.prod(h,objective));

            }
            this.localObjective = this.cplex.addMinimize(expr);
        }
        return true;
    }

    private boolean idealVectorFiller() throws IloException{
        this.idealvector = new TreeMap<>();
        for(String s : this.objectives.keySet()){
            this.solverSettings();
            IloNumExpr bobj = (IloNumExpr) this.objectives.get(s);
            //System.out.println(bobj+"   "+this.cplex.getObjective());
            IloAddable obj = this.cplex.addMinimize(bobj);
            boolean solvable = this.cplex.solve();
            if(!solvable){
                this.model.remove(obj);
                return false;
            }
            double val = this.cplex.getObjValue();
            this.idealvector.put(s, val);
            this.model.remove(obj);

        }
        return true;
    }

    private void solverSettings() throws IloException{
        this.cplex.setOut(null);
        this.cplex.setParam(IloCplex.IntParam.AdvInd, 0);
    }

    public long getID(){
        return this.ID;
    }


    public boolean solve() throws IloException{
        for(String s : this.localConstraints.keySet()){
            //System.out.println("   "+ID+"  "+this.level+"  "+s);
            this.model.add(this.localConstraints.get(s));
        }


        boolean solvablesingleton = this.setObjective();
        if(!solvablesingleton){
            return false;
        }

        this.solverSettings();
        boolean solvable = this.cplex.solve();
        if(!solvable){
            return false;
        }
        this.relsol = new TreeMap<>();
        this.pareto = new TreeMap<>();

        for(String y : this.vars.keySet()){
            this.relsol.put(y, this.cplex.getValue(this.vars.get(y)));
        }
        for(String y : this.objectives.keySet()){
            this.pareto.put(y, this.cplex.getValue(this.objectives.get(y)));
        }
        //this.pareto.put("bau", (double)this.level);

        return true;
    }

    //public void approximate() throws IloException{
    //todo: implementa Feasibility Pump
    // ovviamente nel caso lineare conviene
    // far trovare una qualsiasi soluzione intera al solutore
    //this.intsol =  new TreeMap<String, Double>();
    //}

    public void refresh() throws IloException{
        this.model.remove(this.localObjective);
        for(IloAddable con : this.localConstraints.values()){
            this.model.remove(con);
        }
    }

    public Map<String, IloAddable> branchOn() throws IloException{
        Map<String, IloAddable> ret = new TreeMap<>();
        for(String s : this.pareto.keySet()){
            double relval = this.pareto.get(s);
                double val = this.val.get(s);
                //System.out.println("val:"+val);
                IloAddable con = this.cplex.addLe(this.objectives.get(s), relval - val);

                ret.put(s + this.ID, con);
                this.model.remove(con);
        }
        return ret;
    }

    public List<Map<String, IloAddable>> branchOnStrongly() throws IloException{
        List<Map<String, IloAddable>> list = new LinkedList<>();
        int m = this.objectives.keySet().size();
        int [] counter = new int [m];
        boolean [] flip = new boolean [m];
        for(int i = 0; i < (AlgUtils.pow(2,m)) ; i++){
            Map<String, IloAddable> map = new TreeMap<>();
            int j = 0;
            for(String s : this.pareto.keySet()){

                if(counter[j]%(AlgUtils.pow(2,j)) == 0){
                    flip[j] = !flip[j];
                }

                double relval = this.pareto.get(s);
                double val = this.val.get(s);
                if(flip[j]){

                        //System.out.println("val:"+val);
                        IloAddable con = this.cplex.addLe(this.objectives.get(s), relval - val);
                        //System.out.println("ID+"+ID+" true  "+con);

                        map.put(s + this.ID+"le", con);
                        this.model.remove(con);
                }else{
                    IloAddable con = this.cplex.addGe(this.objectives.get(s), relval );
                    //System.out.println("ID+"+ID+" false "+con);

                    map.put(s + this.ID+"ge", con);
                    this.model.remove(con);
                }


                counter[j] = counter[j] + 1;
                j ++;
            }


            if( !AlgUtils.checkFailure( flip)) {
                //System.out.println(this.ID+"- "+flip[0] + " "+ flip[1]+" "+flip[2]);
                list.add(map);
            }
        }


        return list;
    }

    public Map<String, Double> getRelsol() {
        return relsol;
    }

    public Map<String, Double> getPareto() {
        return pareto;
    }


//todo: devi togliere prima o poi la funzione obiettivo
}
