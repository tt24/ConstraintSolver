
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Solver {

//    public boolean checkConsistency(ArrayList<DecisionVariable> variables, ArrayList<Constraint> constraints, int depth,
//            DecisionVariable var, HashMap<DecisionVariable, TreeSet<Integer>> savedDomains, int[] experimentData) {
//        boolean consistent = true;
//        for (int future = depth + 1; future < variables.size(); future++) {
//            for (Constraint constraint : constraints) {
//                if (constraint.containsVar(variables.get(future)) && constraint.containsVar(var)) {
//                    consistent = revise(constraints, constraint, savedDomains, null, experimentData);
//                    if (!consistent) {
//                        return false;
//                    }
//                }
//            }
//        }
//        return true;
//    }

    public boolean checkConsistency(ArrayList<DecisionVariable> variables, ArrayList<Constraint> constraints, int depth,
            DecisionVariable var, HashMap<DecisionVariable, TreeSet<Integer>> savedDomains, int[] experimentData, boolean sdf) {
        boolean consistent = true;
        int future= depth+1;
        while (future < variables.size()) {
            if (variables.get(future).getAssignedValue() == null ||!sdf) {
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
            if (checkConsistency(variables, constraints,-1, var, savedDomains, experimentData, true)) {
                if (forwardChecking(variables, constraints, experimentData)) {
                    return true;
                }
            }
            int value = var.getAssignedValue();
            var.clearAssignment();
            for (DecisionVariable reverseVar : savedDomains.keySet()) {
                reverseVar.setDomain(savedDomains.get(reverseVar));
            }
            var.getDomain().remove(value);
            savedDomains.clear();
            orderedVariables.clear();
            for (DecisionVariable variable : variables) {
                if (variable.getAssignedValue() == null) {
                    orderedVariables.add(variable);
                }
            }
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
            for (DecisionVariable reverseVar : savedDomains.keySet()) {
                reverseVar.setDomain(savedDomains.get(reverseVar));
            }
            savedDomains.clear();
            iterator.remove();
        }
        return false;
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

    public static void main(String[] args) {
        Solver solver = new Solver();
        ArrayList<Constraint> constraints = new ArrayList<>();
        ArrayList<Constraint> unaryConstraints = new ArrayList<>();
        ArrayList<DecisionVariable> variables = new ArrayList<>();
        solver.crystalMaze(variables, constraints, unaryConstraints);

        int[] experimentData = {0, 0};
        int[] experimentData1 = {0, 0};
        long startTime = System.currentTimeMillis();
//        if (solver.ac3(variables, constraints, unaryConstraints, experimentData)) {
//            solver.forwardChecking(variables, constraints, experimentData1);
//            solver.showSolution(variables);
//        } else {
//            System.out.println("No solution found.");
//        }
        long timeTaken = System.currentTimeMillis() - startTime;
//        System.out.println("experimentData " + experimentData1[0] + " " + experimentData1[1]);
//        System.out.println("Time taken: " + timeTaken);
        //experimentData 148 1133

        int[] experimentData2 = {0, 0};
        int[] experimentData3 = {0, 0};
        startTime = System.currentTimeMillis();
        if (solver.ac3(variables, constraints, unaryConstraints,experimentData2)) {
            solver.forwardChecking(variables, constraints, 0,experimentData3);
            solver.showSolution(variables);
        } else {
            System.out.println("No solution found.");
        }
        timeTaken = System.currentTimeMillis() - startTime;
        System.out.println("experimentData "+experimentData3[0]+ " "+experimentData3[1]);
        //experimentData 237 1531
        System.out.println("Time taken: " + timeTaken);

    }

    public void crystalMaze(ArrayList<DecisionVariable> variables, ArrayList<Constraint> constraints,
            ArrayList<Constraint> unaryConstraints) {
        int size = 8;
        for (int i = 0; i < size; i++) {
            String name = "x" + i;
            variables.add(new DecisionVariable(name, ProblemReader.readRange("int(0-7)")));
        }
        for (int i = 0; i < variables.size(); i++) {
            for (int j = i + 1; j < variables.size(); j++) {
                Constraint constraint = new Constraint(new Expression(variables.get(i), true),
                        new Expression(variables.get(j), true), Comparator.NEQ);
                constraints.add(constraint);
            }
        }
        Expression ex1 = new Expression(variables.get(0), Operator.PLUS, 1, true);
        Expression ex2 = new Expression(variables.get(0), Operator.MINUS, 1, true);
        constraints.add(new Constraint(ex1, new Expression(variables.get(1), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(1), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(3), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(3), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(2), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(2), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(4), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(4), true), Comparator.NEQ));
        ex1 = new Expression(variables.get(1), Operator.PLUS, 1, true);
        ex2 = new Expression(variables.get(1), Operator.MINUS, 1, true);
        constraints.add(new Constraint(ex1, new Expression(variables.get(4), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(4), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(3), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(3), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(5), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(5), true), Comparator.NEQ));
        ex1 = new Expression(variables.get(2), Operator.PLUS, 1, true);
        ex2 = new Expression(variables.get(2), Operator.MINUS, 1, true);
        constraints.add(new Constraint(ex1, new Expression(variables.get(3), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(3), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(6), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(6), true), Comparator.NEQ));
        ex1 = new Expression(variables.get(3), Operator.PLUS, 1, true);
        ex2 = new Expression(variables.get(3), Operator.MINUS, 1, true);
        constraints.add(new Constraint(ex1, new Expression(variables.get(4), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(4), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(7), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(7), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(6), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(6), true), Comparator.NEQ));
        ex1 = new Expression(variables.get(4), Operator.PLUS, 1, true);
        ex2 = new Expression(variables.get(4), Operator.MINUS, 1, true);
        constraints.add(new Constraint(ex1, new Expression(variables.get(5), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(5), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(7), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(7), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(6), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(6), true), Comparator.NEQ));
        ex1 = new Expression(variables.get(5), Operator.PLUS, 1, true);
        ex2 = new Expression(variables.get(5), Operator.MINUS, 1, true);
        constraints.add(new Constraint(ex1, new Expression(variables.get(7), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(7), true), Comparator.NEQ));
        ex1 = new Expression(variables.get(6), Operator.PLUS, 1, true);
        ex2 = new Expression(variables.get(6), Operator.MINUS, 1, true);
        constraints.add(new Constraint(ex1, new Expression(variables.get(7), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(7), true), Comparator.NEQ));

    }

}
