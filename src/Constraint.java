
import java.beans.DesignMode;


public class Constraint {
    
    private DecisionVariable var1;
    private DecisionVariable var2;
    private int value;    
    private Operator operator;
    private boolean complex;
    
    public Constraint(DecisionVariable name1, DecisionVariable name2, Operator operator) {
        this.var1= name1;
        this.var2= name2;
        this.operator = operator;
        this.complex = true;
    }
    public Constraint(DecisionVariable name1, int value, Operator operator){
        this.var1 = name1;
        this.value = value;
        this.operator = operator;
        this.complex = false;
    }
    public DecisionVariable getVar1(){
        return this.var1;
    }
    public DecisionVariable getVar2() {
        return this.var2;
    }
    public int getValue() {
        return this.value;
    }
    public boolean getComplex() {
        return this.complex;
    }
    public Operator getOperator() {
        return this.operator;
    }
    
}
