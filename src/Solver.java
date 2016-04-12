
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Solver {

    public boolean forwardChecking(ArrayList<DecisionVariable> variables, ArrayList<Constraint> constraints, int[] experimentData) {
        //domains are saved to be able to undo pruning if the branch reaches dead end
        HashMap<DecisionVariable, TreeSet<Integer>> savedDomains = new HashMap<>();
        ArrayList<DecisionVariable> orderedVariables = new ArrayList<>();
        //finding unassigned variables to choose one for the next assignment
        for (DecisionVariable variable : variables) {
            if (variable.getAssignedValue() == null) {
                orderedVariables.add(variable);
            }
        }
        //if orderedVariables is empty all variables have been assigned
        while (!orderedVariables.isEmpty()) {
            Collections.sort(orderedVariables);
            // after sorting the variable at index 0 is the one with the smallest domain
            DecisionVariable var = orderedVariables.get(0);
            if (var.getDomain().isEmpty()) {
                return false;
            }
            var.setAssignedValue(var.getDomain().first());
            //count the number of assignments made
            experimentData[0]++;
            if (checkConsistency(variables, constraints, -1, var, savedDomains, experimentData, true)) {
                if (forwardChecking(variables, constraints, experimentData)) {
                    return true;
                }
            }
            //if inconsistent remove the value from the domain and undo pruning
            int value = var.getAssignedValue();
            var.clearAssignment();
            reverseDomains(savedDomains);
            var.getDomain().remove(value);
        }
        return true;
    }

    public boolean forwardChecking(ArrayList<DecisionVariable> variables, ArrayList<Constraint> constraints, int depth, int[] experimentData) {
        //domains are saved to be able to undo pruning if the branch reaches dead end
        HashMap<DecisionVariable, TreeSet<Integer>> savedDomains = new HashMap<>();
        //variables are assigned in the static order, thus the next variable is at index depth
        DecisionVariable var = variables.get(depth);
        Iterator<Integer> iterator = var.getDomain().iterator();
        //try every value in the domain until the solution is found or the whole search tree is traversed
        while (iterator.hasNext()) {
            int value = iterator.next();
            var.setAssignedValue(value);
            //count number of assignments made
            experimentData[0]++;
            if (checkConsistency(variables, constraints, depth, var, savedDomains, experimentData, false)) {
                if (depth == variables.size() - 1) {
                    //all variables are assigned at this point
                    return true;
                } else {
                    if (forwardChecking(variables, constraints, depth + 1, experimentData)) {
                        return true;
                    }
                }
            }
            //branch reached dead end, undo pruning, remove the value that led to the dead end from the domain
            var.clearAssignment();
            reverseDomains(savedDomains);
            iterator.remove();
        }
        return false;
    }

    public boolean checkConsistency(ArrayList<DecisionVariable> variables, ArrayList<Constraint> constraints, int depth,
            DecisionVariable var, HashMap<DecisionVariable, TreeSet<Integer>> savedDomains, int[] experimentData, boolean sdf) {
        boolean consistent = true;
        int future = depth + 1;
        while (future < variables.size()) {
            //for smallest domain first have to check only unassigned variables
            if (variables.get(future).getAssignedValue() == null || !sdf) {
                for (Constraint constraint : constraints) {
                    if (constraint.containsVar(variables.get(future)) && constraint.containsVar(var)) {
                        //revising arcs that contain assigned variable and future one
                        consistent = revise(constraints, constraint, savedDomains, null, experimentData);
                        if (!consistent) {
                            return false;
                        }
                    }
                }
            }
            future++;
        }
        return true;
    }

    /**
     * undo pruning by setting variable domains with the saved domains
     *
     * @param savedDomains
     */
    public void reverseDomains(HashMap<DecisionVariable, TreeSet<Integer>> savedDomains) {
        for (DecisionVariable reverseVar : savedDomains.keySet()) {
            reverseVar.setDomain(savedDomains.get(reverseVar));
        }
        savedDomains.clear();
    }

    /**
     * before searching for solution reduce variable domain sizes where possible
     *
     * @param variables
     * @param constraints
     * @param unaryConstraints
     * @param experimentData
     * @return
     */
    public boolean ac3(ArrayList<DecisionVariable> variables, ArrayList<Constraint> constraints,
            ArrayList<Constraint> unaryConstraints, int[] experimentData) {
        for (DecisionVariable var : variables) {
            //checking node consistency first
            if (!nodeConsistency(constraints, unaryConstraints, var)) {
                return false;
            }
            ConcurrentLinkedQueue<Constraint> constraintsToBeRevised = new ConcurrentLinkedQueue<>();
            //initially adding all constraints
            constraintsToBeRevised.addAll(constraints);
            while (!constraintsToBeRevised.isEmpty()) {
                Constraint constraint = constraintsToBeRevised.poll();
                boolean[] changed = {false, false};
                if (!revise(constraints, constraint, null, changed, experimentData)) {
                    return false;
                } else {
                    if (changed[0]) {
                        //if the domain of the left variable is changed, the constraints that point to this value are added
                        addConstraintsToQueue(constraints, constraintsToBeRevised, constraint.getExp1().getVar());
                    } else {
                        if (changed[1]) {
                            //same with the variable on the right
                            addConstraintsToQueue(constraints, constraintsToBeRevised, constraint.getExp2().getVar());
                        }
                    }
                }

            }
        }
        return true;
    }

    //adding constraints to the queue if they are not there already
    public ConcurrentLinkedQueue<Constraint> addConstraintsToQueue(ArrayList<Constraint> constraints,
            ConcurrentLinkedQueue<Constraint> constraintsToBeRevised, DecisionVariable var) {
        for (Constraint constraint : constraints) {
            if (constraint.containsVar(var) && !constraintsToBeRevised.contains(constraint)) {
                constraintsToBeRevised.offer(constraint);
            }
        }
        return constraintsToBeRevised;
    }

    /**
     * revises arcs and reduces domain where possible before the domain is
     * reduced the previous state is saved if it was not before
     *
     * @param constraints
     * @param constraint
     * @param savedDomains
     * @param changed
     * @param experimentData
     * @return false if a domain of any variable was emptied in the process,
     * true if revision was successful
     */
    public boolean revise(ArrayList<Constraint> constraints, Constraint constraint,
            HashMap<DecisionVariable, TreeSet<Integer>> savedDomains, boolean[] changed, int[] experimentData) {
        //if the variable is assigned of there is only one value left in the domain, the expression is solved
        Integer left = constraint.getExp1().solve();
        Integer right = constraint.getExp2().solve();
        experimentData[1]++;
        if (left != null) {
            if (right != null) {
                //immediately check comparison as values of both variables are known
                return constraint.checkComparison(left, right);
            } else {
                DecisionVariable rightVar = constraint.getExp2().getVar();
                //reduce the domain of the unassigned variable if possible
                if (!checkDomain(constraints, constraint, rightVar, left, false, true, savedDomains)) {
                    return false;
                }
            }
        } else {
            //same but here the left variable is not set while the right is
            if (right != null) {
                DecisionVariable leftVar = constraint.getExp1().getVar();
                if (!checkDomain(constraints, constraint, leftVar, right, true, true, savedDomains)) {
                    return false;
                }
            } else {
                //used in ac3 algorithm to reduce domains
                DecisionVariable leftVar = constraint.getExp1().getVar();
                DecisionVariable rightVar = constraint.getExp2().getVar();
                Iterator<Integer> iterator = leftVar.getDomain().iterator();
                while (iterator.hasNext()) {
                    left = constraint.getExp1().solve(iterator.next());
                    if (!checkDomain(constraints, constraint, rightVar, left, false, false, savedDomains)) {
                        // if the value is not supported on another side, it is removed
                        changed[0] = true;
                        iterator.remove();
                        if (leftVar.isEmptyDomain()) {
                            return false;
                        }
                    }
                }
                experimentData[1]++;
                //same thing, but the different direction of the arc
                iterator = rightVar.getDomain().iterator();
                while (iterator.hasNext()) {
                    right = constraint.getExp2().solve(iterator.next());
                    if (!checkDomain(constraints, constraint, leftVar, right, true, false, savedDomains)) {
                        changed[1] = true;
                        iterator.remove();
                        if (rightVar.isEmptyDomain()) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * saves previous state of a domain
     *
     * @param savedDomains
     * @param var
     */
    public void addToSavedDomains(HashMap<DecisionVariable, TreeSet<Integer>> savedDomains, DecisionVariable var) {
        if (!savedDomains.containsKey(var)) {
            savedDomains.put(var, (TreeSet<Integer>) var.getDomain().clone());
        }
    }

    /**
     * check the domain and reduce where possible
     *
     * @param constraints
     * @param constraint
     * @param var
     * @param value
     * @param order
     * @param setVar
     * @param savedDomains
     * @return
     */
    public boolean checkDomain(ArrayList<Constraint> constraints, Constraint constraint, DecisionVariable var,
            int value, boolean order, boolean setVar, HashMap<DecisionVariable, TreeSet<Integer>> savedDomains) {
        TreeSet<Integer> domain = var.getDomain();
        Iterator<Integer> iterator = domain.iterator();
        boolean result = false;
        boolean finalResult = false;
        while (iterator.hasNext()) {
            int d = iterator.next();
            int varValue;
            if (order) {
                varValue = constraint.getExp1().solve(d);
                result = constraint.checkComparison(varValue, value);
            } else {
                varValue = constraint.getExp2().solve(d);
                result = constraint.checkComparison(value, varValue);
            }
            finalResult = result || finalResult;
            //if initially called from ac3 if there is at least one variable on another side of the arc that supports it return true
            if (!setVar && result) {
                return true;
            }
            if (setVar && !result) {
                //reduce the domain
                addToSavedDomains(savedDomains, var);
                iterator.remove();
                if (domain.isEmpty()) {
                    return false;
                }
            }
        }
        return finalResult;
    }

    public void showSolution(ArrayList<DecisionVariable> variables) {
        System.out.println("Solution");
        for (DecisionVariable var : variables) {
            System.out.println(var.getName() + " " + var.getAssignedValue());
        }
    }

    /**
     * checks unary constraints and reduces domain where possible
     *
     * @param constraints
     * @param unaryConstraints
     * @param var
     * @return
     */
    public boolean nodeConsistency(ArrayList<Constraint> constraints, ArrayList<Constraint> unaryConstraints,
            DecisionVariable var) {
        for (Constraint constraint : unaryConstraints) {
            if (constraint.containsVar(var)) {
                Iterator<Integer> iterator = var.getDomain().iterator();
                while (iterator.hasNext()) {
                    int varValue = constraint.getExp1().solve(iterator.next());
                    if (!constraint.checkComparison(varValue, constraint.getValue())) {
                        iterator.remove();
                        if (var.isEmptyDomain()) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean forwardChecking2(ArrayList<DecisionVariable> variables, ArrayList<Constraint> constraints, int[] experimentData) {
        HashMap<DecisionVariable, TreeSet<Integer>> savedDomains = new HashMap<>();
        ArrayList<DecisionVariable> orderedVariables = new ArrayList<>();
        for (DecisionVariable variable : variables) {
            if (variable.getAssignedValue() == null) {
                orderedVariables.add(variable);
            }
        }
        while (!orderedVariables.isEmpty()) {
            if (Collections.min(orderedVariables).getDomain().isEmpty()) {
                return false;
            }
            //pick any variable that was not assigned yet
            Collections.shuffle(orderedVariables);
            DecisionVariable var = orderedVariables.get(0);
            var.setAssignedValue(var.getDomain().first());
            experimentData[0]++;
            if (checkConsistency(variables, constraints, -1, var, savedDomains, experimentData, true)) {
                if (forwardChecking2(variables, constraints, experimentData)) {
                    return true;
                } else {
                    reverseDomains(savedDomains);
                    var.getDomain().remove(var.getAssignedValue());
                    var.clearAssignment();
                    forwardChecking2(variables, constraints, experimentData);

                }
            } else {
                reverseDomains(savedDomains);
                var.getDomain().remove(var.getAssignedValue());
            }
            var.clearAssignment();
        }
        return true;

    }

    public int[] runExperiment(String name) {
        ArrayList<Constraint> constraints = new ArrayList<>();
        ArrayList<Constraint> unaryConstraints = new ArrayList<>();
        ArrayList<DecisionVariable> variables = new ArrayList<>();
        ProblemReader.crystalMaze(variables, constraints, unaryConstraints);

        if (name.equals("best order")) {
            ArrayList<DecisionVariable> bestOrderVariables = new ArrayList<>();
            bestOrderVariables.add(variables.get(3));
            bestOrderVariables.add(variables.get(5));
            bestOrderVariables.add(variables.get(0));
            bestOrderVariables.add(variables.get(6));
            bestOrderVariables.add(variables.get(1));
            bestOrderVariables.add(variables.get(7));
            bestOrderVariables.add(variables.get(2));
            bestOrderVariables.add(variables.get(4));
            variables = bestOrderVariables;
        } else {
            if (name.equals("random order")) {
                Random random = new Random();
                Collections.shuffle(variables);
            }
        }
        System.out.println(variables);
        int[] experimentData = {0, 0};
        int[] experimentData1 = {0, 0};
        int[] staticVSdynamic = {0, 0, 0, 0};

        if (ac3(variables, constraints, unaryConstraints, experimentData)) {
            long startTime = System.currentTimeMillis();
            if (forwardChecking(variables, constraints, 0, experimentData1)) {
                showSolution(variables);
            } else {
                System.out.println("No solution found.");
            }
            long timeTaken = System.currentTimeMillis() - startTime;
            System.out.println("Experiment static ordering " + name);
            System.out.println("Nodes visited: " + experimentData1[0] + ", arcs revised: " + experimentData1[1] + ", time taken: " + timeTaken);
            staticVSdynamic[0] = experimentData1[0];
            staticVSdynamic[1] = experimentData1[1];
            for (DecisionVariable variable : variables) {
                variable.setInitialParameters();
            }
            for (int i = 0; i < experimentData1.length; i++) {
                experimentData1[i] = 0;
            }
            startTime = System.currentTimeMillis();
            if (forwardChecking(variables, constraints, experimentData1)) {
                showSolution(variables);
            } else {
                System.out.println("No solution found.");
            }
            timeTaken = System.currentTimeMillis() - startTime;
            staticVSdynamic[2] = experimentData1[0];
            staticVSdynamic[3] = experimentData1[1];
            System.out.println("Experiment smallest domain first " + name);
            System.out.println("Nodes visited: " + experimentData1[0] + ", arcs revised: " + experimentData1[1] + ", time taken: " + timeTaken);
        } else {
            System.out.println("No solution found.");
        }
        return staticVSdynamic;

    }

    public static void main(String[] args) {
        Solver solver = new Solver();

        solver.runExperiment("natural order");
        solver.runExperiment("best order");
        int[] staticVSdynamic = {0, 0, 0, 0};
        int numOfRuns = 10;
        for (int i = 0; i < numOfRuns; i++) {
            int[] result = solver.runExperiment("random order");
            for (int j = 0; j < result.length; j++) {
                staticVSdynamic[j] += result[j];
            }
        }
        for (int i = 0; i < staticVSdynamic.length; i++) {
            staticVSdynamic[i] = staticVSdynamic[i] / numOfRuns;
        }
        System.out.println("\n\nAveraged Result of random runs:");
        System.out.println("Static order");
        System.out.println("Nodes visited: " + staticVSdynamic[0] + ", arcs revised: " + staticVSdynamic[1]);
        System.out.println("Dynamic order (smallest domain first)");
        System.out.println("Nodes visited: " + staticVSdynamic[2] + ", arcs revised: " + staticVSdynamic[3]);

    }

}
