
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Solver {

    public boolean checkConsistency(ArrayList<DecisionVariable> variables, ArrayList<Constraint> constraints, int depth,
            DecisionVariable var, HashMap<DecisionVariable, TreeSet<Integer>> savedDomains) {
        boolean consistent = true;
        for (int future = depth + 1; future < variables.size(); future++) {
            for (Constraint constraint : constraints) {
                if (constraint.containsVar(variables.get(future)) && constraint.containsVar(var)) {
                    consistent = revise(constraints, constraint, savedDomains, null);
                    if (!consistent) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean checkConsistency(ArrayList<DecisionVariable> variables, ArrayList<Constraint> constraints,
            DecisionVariable var, HashMap<DecisionVariable, TreeSet<Integer>> savedDomains) {
        boolean consistent = true;
        for (int future = 0; future < variables.size(); future++) {
            if (variables.get(future).getAssignedValue() == null) {
                for (Constraint constraint : constraints) {
                    if (constraint.containsVar(variables.get(future)) && constraint.containsVar(var)) {
                        consistent = revise(constraints, constraint, savedDomains, null);
                        if (!consistent) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean forwardChecking(ArrayList<DecisionVariable> variables, ArrayList<Constraint> constraints) {
        HashMap<DecisionVariable, TreeSet<Integer>> savedDomains = new HashMap<>();
        ArrayList<DecisionVariable> orderedVariables = new ArrayList<>();
        for(DecisionVariable variable: variables) {
            if(variable.getAssignedValue()==null) {
                orderedVariables.add(variable);
            }
        }
        while(!orderedVariables.isEmpty()){
        Collections.sort(orderedVariables);
        DecisionVariable var = orderedVariables.get(0);
        if(var.getDomain().isEmpty())
            return false;
        var.setAssignedValue(var.getDomain().first());
        if (checkConsistency(variables, constraints, var, savedDomains)) {
            if (forwardChecking(variables, constraints)) {
                return true;
            }
        }
        System.out.println("clear " + var.getName() + " " + var.getAssignedValue());
        int value = var.getAssignedValue();
        var.clearAssignment();
        for (DecisionVariable reverseVar : savedDomains.keySet()) {
            reverseVar.setDomain(savedDomains.get(reverseVar));
        }
        var.getDomain().remove(value);
        savedDomains.clear();
        orderedVariables.clear();
        for(DecisionVariable variable: variables) {
            if(variable.getAssignedValue()==null) {
                orderedVariables.add(variable);
            }
        }
        }
        return true;
    }

    public boolean forwardChecking(ArrayList<DecisionVariable> variables, ArrayList<Constraint> constraints, int depth) {
        HashMap<DecisionVariable, TreeSet<Integer>> savedDomains = new HashMap<>();
        DecisionVariable var = variables.get(depth);
        Iterator<Integer> iterator = var.getDomain().iterator();
        while (iterator.hasNext()) {
            int value = iterator.next();
            var.setAssignedValue(value);
            if (checkConsistency(variables, constraints, depth, var, savedDomains)) {
                if (depth == variables.size() - 1) {
                    showSolution(variables);
                    return true;
                } else {
                    if (forwardChecking(variables, constraints, depth + 1)) {
                        return true;
                    }
                }
            }
            System.out.println("clear " + var.getName() + " " + value);
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
            ArrayList<Constraint> unaryConstraints) {
        for (DecisionVariable var : variables) {
            if (!nodeConsistency(constraints, unaryConstraints, var)) {
                return false;
            }
            ConcurrentLinkedQueue<Constraint> constraintsToBeRevised = new ConcurrentLinkedQueue<>();
            constraintsToBeRevised.addAll(constraints);
            while (!constraintsToBeRevised.isEmpty()) {
                Constraint constraint = constraintsToBeRevised.poll();
                boolean[] changed = {false, false};
                if (!revise(constraints, constraint, null, changed)) {
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
            HashMap<DecisionVariable, TreeSet<Integer>> savedDomains, boolean[] changed) {
        Integer left = constraint.getExp1().solve();
        Integer right = constraint.getExp2().solve();
        if (left != null) {
            if (right != null) {
                return constraint.checkComparison(left, right);
            } else {
                DecisionVariable rightVar = constraint.getExp2().getVar();
                System.out.println("Before " + rightVar);
                if (!checkDomain(constraints, constraint, rightVar, left, false, true, savedDomains)) {
                    return false;
                }
            }
        } else {
            if (right != null) {
                DecisionVariable leftVar = constraint.getExp1().getVar();
                System.out.println("Before " + leftVar);
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
        System.out.println("check domain " + var.toString() + " " + value + " " + order + " " + constraint + " " + setVar);
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
            // System.out.println(d);

            if (order) {
                result = constraint.checkComparison(varValue, value);
            } else {
                result = constraint.checkComparison(value, varValue);
            }
            finalResult = result || finalResult;
            if (!setVar && result) {
                System.out.println("result true");
                return true;
            }
            if (setVar && !result) {
                addToSavedDomains(savedDomains, var);
                iterator.remove();
                System.out.println("removing " + d + " from " + var);
                if (domain.isEmpty()) {
                    System.out.println("emptied domain");
                    return false;
                }
            }
        }
        System.out.println("result " + finalResult);
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
        ArrayList<DecisionVariable> orderedVariables = new ArrayList<>();
        solver.crystalMaze(variables, constraints, unaryConstraints);
        orderedVariables = (ArrayList<DecisionVariable>) variables.clone();
        System.out.println(orderedVariables);

//        if (solver.ac3(variables, constraints, unaryConstraints)) {
//            solver.forwardChecking(variables, constraints, 0);
//            solver.showSolution(variables);
//        } else {
//            System.out.println("No solution found.");
//        }
        
                if (solver.ac3(variables, constraints, unaryConstraints)) {
            solver.forwardChecking(variables, constraints);
            solver.showSolution(variables);
        } else {
            System.out.println("No solution found.");
        }

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
