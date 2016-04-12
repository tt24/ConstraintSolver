
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeSet;

public class ProblemReader {

    //[a b c]
    public static TreeSet<Integer> readList(String s) {
        TreeSet<Integer> list = new TreeSet<>();
        s = s.replace("[", "");
        s = s.replace("]", "");
        String elements[] = s.split(" ");
        for (int i = 0; i < elements.length; i++) {
            list.add(Integer.parseInt(elements[i]));
        }
        return list;
    }

    // int(k..m)
    public static TreeSet<Integer> readRange(String s) {
        TreeSet<Integer> range = new TreeSet<>();
        s = s.replace("int(", "");
        s = s.replace(" ", "");
        s = s.replace(")", "");
        String[] stEnd = s.split("-");
        int index = Integer.parseInt(stEnd[0]);
        while (index <= Integer.parseInt(stEnd[1])) {
            range.add(index);
            index++;
        }
        return range;
    }

    public static Matrix createMatrix(String name, int n, int m, TreeSet<Integer> domain) {
        ArrayList<ArrayList<DecisionVariable>> matrix = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ArrayList<DecisionVariable> row = new ArrayList<>();
            for (int j = 0; j < m; j++) {
                row.add(new DecisionVariable(domain));
            }
            matrix.add(row);
        }
        return new Matrix(name, matrix);
    }

    public static Constraint readContraint(String s, ArrayList<DecisionVariable> variables) {
        s = s.replace(" ", "");
        Comparator comparator;
        String parts[];
        if (s.contains("<=")) {
            comparator = Comparator.LEQ;
            parts = s.split("<=");
        } else {
            if (s.contains("<")) {
                comparator = Comparator.LESS;
                parts = s.split("<");
            } else {
                if (s.contains(">=")) {
                    comparator = Comparator.MEQ;
                    parts = s.split(">=");
                } else {
                    if (s.contains(">")) {
                        comparator = Comparator.MORE;
                        parts = s.split(">");
                    } else {
                        if (s.contains("==")) {
                            comparator = Comparator.EQ;
                            parts = s.split("==");
                        } else {
                            comparator = Comparator.NEQ;
                            parts = s.split("!=");
                        }
                    }
                }
            }
        }
        return new Constraint(readExpression(parts[0], variables), readExpression(parts[1], variables), comparator);
    }

    @SuppressWarnings("empty-statement")
    public static Expression readExpression(String s, ArrayList<DecisionVariable> variables) {
        Operator operator = null;
        String[] parts = {s};
        if (s.contains("+")) {
            operator = Operator.PLUS;
            parts = s.split("+");
        } else {
            if (s.contains("-")) {
                operator = Operator.MINUS;
                parts = s.split("-");
            } else {
                if (s.contains("*")) {
                    operator = Operator.MULTIPLY;
                    parts = s.split("*");
                } else {
                    if (s.contains("/")) {
                        operator = Operator.DIVIDE;
                        parts = s.split("/");
                    }
                }
            }
        }
        Expression expression = null;
        for (DecisionVariable var : variables) {
            if (parts[0].equals(var.getName())) {
                if (parts.length > 1) {
                    expression = new Expression(var, operator, Integer.parseInt(parts[1]), true);
                } else {
                    expression = new Expression(var, true);
                }
            } else {
                if (parts.length > 1 && parts[1].equals(var.getName())) {
                    expression = new Expression(var, operator, Integer.parseInt(parts[0]), false);
                } else {
                    expression = new Expression(var, false);
                }
            }
        }
        return expression;
    }

    public static DecisionVariable readVariable(String s) {

        return null;
    }

    public static void readProblem(String filename) throws FileNotFoundException, IOException {
        String let = "let";
        String find = "find";
        BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
        String s;
        ArrayList<Constraint> constraints = new ArrayList<>();
        ArrayList<DecisionVariable> variables = new ArrayList<>();
        while ((s = br.readLine()) != null) {
            if (s.contains(let)) {
                s = s.replace(let, "");
                variables.add(readVariable(s));
            } else {
                if (s.contains(find)) {

                } else {
                    constraints.add(readContraint(s, variables));
                    System.out.println(constraints.get(constraints.size() - 1));
                }
            }
        }
    }

    public static void crystalMaze(ArrayList<DecisionVariable> variables, ArrayList<Constraint> constraints,
            ArrayList<Constraint> unaryConstraints) {
        int size = 8;
        for (int i = 0; i < size; i++) {
            String name = "x" + i;
            variables.add(new DecisionVariable(name, readRange("int(0-7)")));
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
