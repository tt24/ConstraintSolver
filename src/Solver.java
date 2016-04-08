
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

public class Solver {

    private ArrayList<DecisionVariable> variables = new ArrayList<>();
    private ArrayList<Constraint> unaryConstraints = new ArrayList<>();
    private ArrayList<Constraint> constraints = new ArrayList<>();
    private HashMap<String, Integer> assignments = new HashMap<>();
    private LinkedList<DecisionVariable> changedVars = new LinkedList<>();

    public void forwardChecking(int depth) {
        DecisionVariable var = variables.get(depth);
        Iterator<Integer> iterator = var.getDomain().iterator();
        while (iterator.hasNext()) {
            int value = iterator.next();
            assignments.put(var.getName(), value);
            boolean consistent = true;
            for (int future = depth + 1; future < variables.size(); future++) {
                for (Constraint constraint : constraints) {
                    if (constraint.containsVar(variables.get(future)) && constraint.containsVar(var)) {
                        consistent = revise(constraint, var);
                    }
                }
            }
            if (consistent) {
                if (depth == variables.size() - 1) {
                    showSolution();
                } else {
                    forwardChecking(depth + 1);
                }
            }
            assignments.remove(var.getName());
            for (DecisionVariable changed : changedVars) {
                changed.reverseCheckpoint();
            }
        }
    }

    public boolean revise(Constraint constraint, DecisionVariable var) {
        int value = assignments.get(var.getName());
        boolean first = constraint.getExp1().getVar().equals(var);
        TreeSet<Integer> domain;
        if(first) {
            domain = constraint.getExp2().getVar().getDomain();
        }
        else {
            domain = constraint.getExp1().getVar().getDomain();
        }
        Iterator iterator = domain.iterator();
        while(iterator.hasNext()) {
        }
        
        return false;
    }

    public void showSolution() {

    }

    /**
     * checks node consistency and reduces domain if necessary
     *
     * @param var
     * @return
     */
    public boolean nodeConsistency(DecisionVariable var) {
        for (Constraint constraint : unaryConstraints) {
            for (int d : var.getDomain()) {
                if (!constraint.checkComparison(d, constraint.getValue())) {
                    changedVars.push(var);
                    var.checkpoint();
                    if (!var.removeFromDomain(d)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

}
