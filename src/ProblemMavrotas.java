import ilog.concert.*;
import ilog.cplex.*;
import java.util.Map;
import java.util.TreeMap;

public class ProblemMavrotas {

    private long ID;
    private long level;
    private static int n;
    private static Map<String, IloAddable> constraints;
    private static Map<String, IloNumExpr> objectives;
    private Map<String, IloAddable> localConstraints;
    private static Map<String, IloNumVar> vars;
    private static IloCplex cplex;
    private static IloModel model;
    private Map<String, Double> idealvector;
    public static double eps = 10e-8;


    public ProblemMavrotas(long ID,double [][] objectives, double [][] matrixA, double[] b,boolean[] binary) throws IloException {
        this.basicFilling(objectives, matrixA, b, binary);
        this.ID = ID;
    }

    public ProblemMavrotas(long ID, ProblemMavrotas p,Map<String, IloAddable> newconstraints) throws IloException {
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

    public ProblemMavrotas(long ID, ProblemMavrotas p,String conname, IloAddable con) throws IloException {
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
        this.createObjectives(objectives);
        this.model = this.cplex.getModel();
        this.localConstraints = new TreeMap<>();
        this.level = 0;
        this.n = objectives[0].length;
    }
    public long getID(){
        return ID;
    }

    public boolean levelReach(){
        //System.out.println(this.level);
        //System.out.println(this.n);
        return this.level == this.n ;
    }
    private void createVariable(int n, boolean[] binary) throws IloException{
        for(int i = 0; i < n; i++){
            String s = "x"+(i+1);
            if(binary[i]){
                this.vars.put(s, this.cplex.numVar(0d,1d));
            }else {
                this.vars.put(s, this.cplex.numVar(Integer.MIN_VALUE, Integer.MAX_VALUE));
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

            IloLinearNumExpr expr = this.cplex.linearNumExpr();

            int j = 0;
            for(String s : this.vars.keySet()){
                expr.addTerm(objectives[i][j], this.vars.get(s));
                j++;
            }
            String s = "objective"+i;
            this.objectives.put(s, expr);
        }
    }

    private boolean idealVectorFiller() throws IloException{
        for(IloAddable con : this.localConstraints.values()){
            this.model.add(con);
        }
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

    public boolean solve() throws IloException{
        return this.idealVectorFiller();
    }

    public Map<String, Double> getIdealvector() {
        return idealvector;
    }

    private void solverSettings() throws IloException{
        this.cplex.setOut(null);
        this.cplex.setParam(IloCplex.IntParam.AdvInd, 0);
    }


    //public void approximate() throws IloException{
    //todo: implementa Feasibility Pump
    // ovviamente nel caso lineare conviene
    // far trovare una qualsiasi soluzione intera al solutore
    //this.intsol =  new TreeMap<String, Double>();
    //}

    public void refresh() throws IloException{
        for(IloAddable con : this.localConstraints.values()){
            this.model.remove(con);
        }
    }

    public Map<String, IloAddable> branchOn() throws IloException{  //todo modificare per fare fixing
        Map<String, IloAddable> ret = new TreeMap<>();
        String s = "x"+(this.level+1);
        for(int i = 0; i<2; i++) {
            IloLinearNumExpr expr = this.cplex.linearNumExpr();
            expr.addTerm(1d, this.vars.get(s));
            IloAddable con = this.cplex.addEq(expr, i);
            ret.put("FixingTo"+i+"Var"+s , con);
            this.model.remove(con);
        }
        return ret;
    }
}

