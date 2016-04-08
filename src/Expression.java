
public class Expression {

    private DecisionVariable var;
    private Operator operator;
    private int value;
    private boolean order;

    public Expression(DecisionVariable var, Operator operator, int value, boolean order) {
        this.var = var;
        this.operator = operator;
        this.value = value;
        this.order = order;
    }

    public Expression(DecisionVariable var, boolean order) {
        this.var = var;
        this.order = order;
    }

    public DecisionVariable getVar() {
        return this.var;
    }

    public Operator getOperator() {
        return this.operator;
    }

    public int getValue() {
        return this.value;
    }
    
    public boolean getOrder() {
        return this.order;
    }

    public int solve(int varValue) {
        return 0;
    }
    
    public String toString() {
        if(this.operator!=null) {
            return this.var.getName()+" "+operator+" "+this.value+" "+this.order;
        }
        return this.var.getName();
    }

}
