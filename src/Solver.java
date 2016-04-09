
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Solver {

    private ArrayList<DecisionVariable> variables = new ArrayList<>();
    private ArrayList<Constraint> unaryConstraints = new ArrayList<>();
    private ArrayList<Constraint> constraints = new ArrayList<>();

    public void forwardChecking(int depth) {
        HashMap<DecisionVariable, TreeSet<Integer>> savedDomains = new HashMap<>();
        DecisionVariable var = variables.get(depth);
        Iterator<Integer> iterator = var.getDomain().iterator();
        while (iterator.hasNext()) {
            int value = iterator.next();
            if(value==2 && var.equals(variables.get(0))) 
            	System.out.println("HERE");
            var.setAssignedValue(value);
            boolean consistent = true;
            for (int future = depth + 1; future < variables.size(); future++) {
                for (Constraint constraint : constraints) {
                    if (constraint.containsVar(variables.get(future)) && constraint.containsVar(var)) {
                        consistent = revise(constraint, savedDomains, null);
                    }
                }
            }
            if (consistent) {
                if (depth == variables.size() - 1) {
                    showSolution();
                    return;
                } else {
                    forwardChecking(depth + 1);
                }
            }
            System.out.println("clear "+var.getName()+" "+value);
            var.clearAssignment();
            for (DecisionVariable reverseVar : savedDomains.keySet()) {
                reverseVar.setDomain(savedDomains.get(reverseVar));
            }
            savedDomains.clear();
            iterator.remove();
        }
    }

    public boolean ac3(HashMap<DecisionVariable, TreeSet<Integer>> savedDomains) {
        for (DecisionVariable var : variables) {
            if (!nodeConsistency(var, savedDomains)) {
                return false;
            }
            ConcurrentLinkedQueue<Constraint> constraintsToBeRevised = new ConcurrentLinkedQueue<>();
            constraintsToBeRevised.addAll(constraints);
            while (!constraintsToBeRevised.isEmpty()) {
                Constraint constraint = constraintsToBeRevised.poll();
                boolean[] changed = {false, false};
                if (!revise(constraint, savedDomains, changed)) {
                    return false;
                } else {
                    if (changed[0]) {
                        addConstraintsToQueue(constraintsToBeRevised, constraint.getExp1().getVar());
                    } else {
                        if (changed[1]) {
                            addConstraintsToQueue(constraintsToBeRevised, constraint.getExp2().getVar());
                        }
                    }
                }

            }
        }
        return false;
    }

    public ConcurrentLinkedQueue<Constraint> addConstraintsToQueue(ConcurrentLinkedQueue<Constraint> constraintsToBeRevised, DecisionVariable var) {
        for (Constraint constraint : constraints) {
            if (constraint.containsVar(var) && !constraintsToBeRevised.contains(constraint)) {
                constraintsToBeRevised.offer(constraint);
            }
        }
        return constraintsToBeRevised;
    }

    public boolean revise(Constraint constraint, HashMap<DecisionVariable, TreeSet<Integer>> savedDomains, boolean[] changed) {
        Integer left = constraint.getExp1().solve();
        Integer right = constraint.getExp2().solve();
        if (left != null) {
            if (right != null) {
                return constraint.checkComparison(left, right);
            } else {
                DecisionVariable rightVar = constraint.getExp2().getVar();
                System.out.println("Before "+ rightVar);
                if (!checkDomain(constraint, rightVar, left, false, true, savedDomains)) {
                    return false;
                }
            }
        } else {
            if (right != null) {
                DecisionVariable leftVar = constraint.getExp1().getVar();
                System.out.println("Before "+ leftVar);
                if (!checkDomain(constraint, leftVar, right, true, true, savedDomains)) {
                    return false;
                }
            } else {
                DecisionVariable leftVar = constraint.getExp1().getVar();
                DecisionVariable rightVar = constraint.getExp2().getVar();
                Iterator<Integer> iterator = leftVar.getDomain().iterator();
                while (iterator.hasNext()) {
                    left = constraint.getExp1().solve(iterator.next());
                    if (!checkDomain(constraint, rightVar, left, false, false, savedDomains)) {
                        addToSavedDomains(savedDomains, leftVar);
                        if (changed != null) {
                            changed[0] = true;
                        }
                        iterator.remove();
                        if (leftVar.isEmptyDomain()) {
                            return false;
                        }
                    }
                }
                iterator = rightVar.getDomain().iterator();
                while (iterator.hasNext()) {
                    right = constraint.getExp2().solve(iterator.next());
                    if (!checkDomain(constraint, leftVar, right, true, false, savedDomains)) {
                        addToSavedDomains(savedDomains, rightVar);
                        if (changed != null) {
                            changed[1] = true;
                        }
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

    public boolean checkDomain(Constraint constraint, DecisionVariable var, int value, boolean order, boolean setVar, HashMap<DecisionVariable, TreeSet<Integer>> savedDomains) {
        System.out.println("check domain " + var.toString() + " " + value + " " + order + " "+constraint + " "+ setVar);
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
            System.out.println(d);
            
            if (order) {
                result = constraint.checkComparison(varValue, value);
            } else {
                result = constraint.checkComparison(value, varValue);
            }
            finalResult = result ||finalResult;
            if (!setVar&&result) {
            	System.out.println("result true");
                return true;
            }
            if(setVar&&!result) {
            	addToSavedDomains(savedDomains, var);
            	iterator.remove();
            	System.out.println("removing "+d+" from "+var);
            	if(domain.isEmpty()) {
            		System.out.println("emptied domain");
            		return false;
            	}
            }
        }
        System.out.println("result "+finalResult);
        return finalResult;
    }

    public void showSolution() {
    	System.out.println("Solution");
    	for(DecisionVariable var: variables) {
    		System.out.println(var.getName()+ " "+var.getAssignedValue());
    	}
    }

    public boolean nodeConsistency(DecisionVariable var, HashMap<DecisionVariable, TreeSet<Integer>> savedDomains) {
        for (Constraint constraint : unaryConstraints) {
            if (constraint.containsVar(var)) {
                Iterator<Integer> iterator = var.getDomain().iterator();
                while (iterator.hasNext()) {
                    int varValue = constraint.getExp1().solve(iterator.next());
                    if (!constraint.checkComparison(varValue, constraint.getValue())) {
                        addToSavedDomains(savedDomains, var);
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

    public void changeString(boolean[] array) {
        array[0] = true;
    }

    public static void main(String[] args) {
        Solver solver = new Solver();
//        Expression x = new Expression(new DecisionVariable("x", ProblemReader.readRange("int(2-5)")), Operator.PLUS, 1,
//                true);
		// Expression y = new Expression(new
        // DecisionVariable("y",ProblemReader.readRange("int(0-1)")),Operator.PLUS,
        // 1, true);
        // Constraint c = new Constraint(x, y, Comparator.EQ);
        // System.out.println(solver.revise(c));
        // System.out.println(x.getVar());
        // System.out.println(y.getVar());
//        Constraint c1 = new Constraint(x, 6, Comparator.LESS);
//        Constraint c2 = new Constraint(x, 2, Comparator.MORE);
//        solver.unaryConstraints.add(c1);
//        solver.unaryConstraints.add(c2);
//        boolean[] array = {false, true};
//        solver.changeString(array);
//        System.out.println(array[0]);
        solver.crystalMaze();
        System.out.println("set up");
        solver.showSolution();
        for(Constraint constraint: solver.constraints) {
        	System.out.println(constraint);
        }
        solver.forwardChecking(0);
    }

    public void crystalMaze() {
        int size = 8;
        for (int i = 0; i < size; i++) {
            String name = "x" + i;
            variables.add(new DecisionVariable(name, ProblemReader.readRange("int(0-7)")));
        }
        for (int i = 0; i < variables.size(); i++) {
            for (int j = i+1; j < variables.size(); j++) {
                Constraint constraint = new Constraint(new Expression(variables.get(i), true), new Expression(variables.get(j), true), Comparator.NEQ);
                constraints.add(constraint);
            }
        }
        Expression ex1 = new Expression(variables.get(0),Operator.PLUS, 1, true);
        Expression ex2 = new Expression(variables.get(0),Operator.MINUS, 1, true);
        constraints.add(new Constraint(ex1, new Expression(variables.get(1), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(1), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(3), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(3), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(2), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(2), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(4), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(4), true), Comparator.NEQ));
        ex1 = new Expression(variables.get(1),Operator.PLUS, 1, true);
        ex2 = new Expression(variables.get(1),Operator.MINUS, 1, true);
        constraints.add(new Constraint(ex1, new Expression(variables.get(4), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(4), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(3), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(3), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(5), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(5), true), Comparator.NEQ));
        ex1 = new Expression(variables.get(2),Operator.PLUS, 1, true);
        ex2 = new Expression(variables.get(2),Operator.MINUS, 1, true);
        constraints.add(new Constraint(ex1, new Expression(variables.get(3), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(3), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(6), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(6), true), Comparator.NEQ));
        ex1 = new Expression(variables.get(3),Operator.PLUS, 1, true);
        ex2 = new Expression(variables.get(3),Operator.MINUS, 1, true);
        constraints.add(new Constraint(ex1, new Expression(variables.get(4), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(4), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(7), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(7), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(6), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(6), true), Comparator.NEQ));
        ex1 = new Expression(variables.get(4),Operator.PLUS, 1, true);
        ex2 = new Expression(variables.get(4),Operator.MINUS, 1, true);
        constraints.add(new Constraint(ex1, new Expression(variables.get(5), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(5), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(7), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(7), true), Comparator.NEQ));
        constraints.add(new Constraint(ex1, new Expression(variables.get(6), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(6), true), Comparator.NEQ));
        ex1 = new Expression(variables.get(5),Operator.PLUS, 1, true);
        ex2 = new Expression(variables.get(5),Operator.MINUS, 1, true);
        constraints.add(new Constraint(ex1, new Expression(variables.get(7), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(7), true), Comparator.NEQ));
        ex1 = new Expression(variables.get(6),Operator.PLUS, 1, true);
        ex2 = new Expression(variables.get(6),Operator.MINUS, 1, true);
        constraints.add(new Constraint(ex1, new Expression(variables.get(7), true), Comparator.NEQ));
        constraints.add(new Constraint(ex2, new Expression(variables.get(7), true), Comparator.NEQ));
        
    }

}
