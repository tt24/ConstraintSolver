
import java.util.ArrayList;

public class Problem {
    
    private ArrayList<DecisionVariable> variables = new ArrayList<>();
    private ArrayList<Constraint> arcs = new ArrayList<>();
    private ArrayList<Matrix> matrices = new ArrayList<>();
    
    public ArrayList<DecisionVariable> getDecisionVariables() {
    	return this.variables;
    }
    public ArrayList<Matrix> getMatrices() {
    	return this.matrices;
    }
    public ArrayList<Constraint> getArcs() {
    	return this.arcs;
    }
    
}
