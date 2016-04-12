
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Solver {

    public boolean checkConsistency(ArrayList<DecisionVariable> variables, ArrayList<Constraint> constraints, int depth,
            DecisionVariable var, HashMap<DecisionVariable, TreeSet<Integer>> savedDomains, int[] experimentData, boolean sdf) {
        boolean consistent = true;
        int future = depth + 1;
        while (future < variables.size()) {
            if (variables.get(future).getAssignedValue() == null || !sdf) {
                for (Constraint constraint : constraints) {
                    if (constraint.containsVar(variables.get(future)) && constraint.containsVar(var)) {
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

    public boolean forwardChecking(ArrayList<DecisionVariable> variables, ArrayList<Constraint> constraints, int[] experimentData) {
        HashMap<DecisionVariable, TreeSet<Integer>> savedDomains = new HashMap<>();
        ArrayList<DecisionVariable> orderedVariables = new ArrayList<>();
        for (DecisionVariable variable : variables) {
            if (variable.getAssignedValue() == null) {
                orderedVariables.add(variable);
            }
        }
        while (!orderedVariables.isEmpty()) {
            Collections.sort(orderedVariables);
            DecisionVariable var = orderedVariables.get(0);
            if (var.getDomain().isEmpty()) {
                return false;
            }
            var.setAssignedValue(var.getDomain().first());
            experimentData[0]++;
            if (checkConsistency(variables, constraints, -1, var, savedDomains, experimentData, true)) {
                if (forwardChecking(variables, constraints, experimentData)) {
                    return true;
                }
            }
            int value = var.getAssignedValue();
            var.clearAssignment();
            reverseDomains(savedDomains);
            var.getDomain().remove(value);
        }
        return true;
    }

    public boolean forwardChecking(ArrayList<DecisionVariable> variables, ArrayList<Constraint> constraints, int depth, int[] experimentData) {
        HashMap<DecisionVariable, TreeSet<Integer>> savedDomains = new HashMap<>();
        DecisionVariable var = variables.get(depth);
        Iterator<Integer> iterator = var.getDomain().iterator();
        while (iterator.hasNext()) {
            int value = iterator.next();
            var.setAssignedValue(value);
            experimentData[0]++;
            if (checkConsistency(variables, constraints, depth, var, savedDomains, experimentData, false)) {
                if (depth == variables.size() - 1) {
                    return true;
                } else {
                    if (forwardChecking(variables, constraints, depth + 1, experimentData)) {
                        return true;
                    }
                }
            }
            var.clearAssignment();
            reverseDomains(savedDomains);
            iterator.remove();
        }
        return false;
    }

    public void reverseDomains(HashMap<DecisionVariable, TreeSet<Integer>> savedDomains) {
        for (DecisionVariable reverseVar : savedDomains.keySet()) {
            reverseVar.setDomain(savedDomains.get(reverseVar));
        }
        savedDomains.clear();
    }

    public boolean ac3(ArrayList<DecisionVariable> variables, ArrayList<Constraint> constraints,
            ArrayList<Constraint> unaryConstraints, int[] experimentData) {
        for (DecisionVariable var : variables) {
            if (!nodeConsistency(constraints, unaryConstraints, var)) {
                return false;
            }
            ConcurrentLinkedQueue<Constraint> constraintsToBeRevised = new ConcurrentLinkedQueue<>();
            constraintsToBeRevised.addAll(constraints);
            while (!constraintsToBeRevised.isEmpty()) {
                Constraint constraint = constraintsToBeRevised.poll();
                boolean[] changed = {false, false};
                if (!revise(constraints, constraint, null, changed, experimentData)) {
                    return false;
                } else {
                    if (changed[0]) {
                        addConstraintsToQueue(constraints, constraintsToBeRevised, constraint.getExp1().getVar());
                    } else {
                        if (changed[1]) {
                            addConstraintsToQueue(constraints, constraintsToBeRevised, constraint.getExp2().getVar());
                        }
                    }
                }

            }
        }
        return true;
    }

    public ConcurrentLinkedQueue<Constraint> addConstraintsToQueue(ArrayList<Constraint> constraints,
            ConcurrentLinkedQueue<Constraint> constraintsToBeRevised, DecisionVariable var) {
        for (Constraint constraint : constraints) {
            if (constraint.containsVar(var) && !constraintsToBeRevised.contains(constraint)) {
                constraintsToBeRevised.offer(constraint);
            }
        }
        return constraintsToBeRevised;
    }

    public boolean revise(ArrayList<Constraint> constraints, Constraint constraint,
            HashMap<DecisionVariable, TreeSet<Integer>> savedDomains, boolean[] changed, int[] experimentData) {
        Integer left = constraint.getExp1().solve();
        Integer right = constraint.getExp2().solve();
        experimentData[1]++;
        if (left != null) {
            if (right != null) {
                return constraint.checkComparison(left, right);
            } else {
                DecisionVariable rightVar = constraint.getExp2().getVar();
                if (!checkDomain(constraints, constraint, rightVar, left, false, true, savedDomains)) {
                    return false;
                }
            }
        } else {
            if (right != null) {
                DecisionVariable leftVar = constraint.getExp1().getVar();
                if (!checkDomain(constraints, constraint, leftVar, right, true, true, savedDomains)) {
                    return false;
                }
            } else {
                DecisionVariable leftVar = constraint.getExp1().getVar();
                DecisionVariable rightVar = constraint.getExp2().getVar();
                Iterator<Integer> iterator = leftVar.getDomain().iterator();
                while (iterator.hasNext()) {
                    left = constraint.getExp1().solve(iterator.next());
                    if (!checkDomain(constraints, constraint, rightVar, left, false, false, savedDomains)) {
                        changed[0] = true;
                        iterator.remove();
                        if (leftVar.isEmptyDomain()) {
                            return false;
                        }
                    }
                }
                experimentData[1]++;
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

    public void addToSavedDomains(HashMap<DecisionVariable, TreeSet<Integer>> savedDomains, DecisionVariable var) {
        if (!savedDomains.containsKey(var)) {
            savedDomains.put(var, (TreeSet<Integer>) var.getDomain().clone());
        }
    }

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
            } else {
                varValue = constraint.getExp2().solve(d);
            }

            if (order) {
                result = constraint.checkComparison(varValue, value);
            } else {
                result = constraint.checkComparison(value, varValue);
            }
            finalResult = result || finalResult;
            if (!setVar && result) {
                return true;
            }
            if (setVar && !result) {
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

    public void runExperiment(String name) {

        ArrayList<Constraint> constraints = new ArrayList<>();
        ArrayList<Constraint> unaryConstraints = new ArrayList<>();
        ArrayList<DecisionVariable> variables = new ArrayList<>();
        ProblemReader.crystalMaze(variables, constraints, unaryConstraints);
        int[] order = new int[variables.size()];

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
            if (order.equals("random order")) {
                Random random = new Random();
                Collections.shuffle(variables);
            }
        }
        System.out.println(variables);
        int[] experimentData = {0, 0};
        int[] experimentData1 = {0, 0};
        
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
            for(DecisionVariable variable: variables) {
                variable.setInitialParameters();
            }
            for(int i = 0; i<experimentData1.length; i++) {
                experimentData1[i] = 0;
            }
            startTime = System.currentTimeMillis();
            if (forwardChecking(variables, constraints, experimentData1)) {
                showSolution(variables);
            } else {
                System.out.println("No solution found.");
            }
            timeTaken = System.currentTimeMillis() - startTime;
        System.out.println("Experiment smallest domain first " + name);
        System.out.println("Nodes visited: " + experimentData1[0] + ", arcs revised: " + experimentData1[1] + ", time taken: " + timeTaken);
        } else {
            System.out.println("No solution found.");
        }
        
    }

    public static void main(String[] args) {
        Solver solver = new Solver();

        solver.runExperiment("natural order");
        solver.runExperiment("best order");

        //experimentData 148 1133
//
//        int[] experimentData2 = {0, 0};
//        int[] experimentData3 = {0, 0};
//        startTime = System.currentTimeMillis();
//        if (solver.ac3(variables, constraints, unaryConstraints,experimentData2)) {
//            solver.forwardChecking(variables, constraints, 0,experimentData3);
//            solver.showSolution(variables);
//        } else {
//            System.out.println("No solution found.");
//        }
//        timeTaken = System.currentTimeMillis() - startTime;
//        System.out.println("experimentData "+experimentData3[0]+ " "+experimentData3[1]);
//        //experimentData 237 1531
//        System.out.println("Time taken: " + timeTaken);
    }

}
