
public class Constraint {
    
    private Expression exp1;
    private Expression exp2;
    private int value;    
    private Comparator comparator;
    private boolean binary;
    
    public Constraint(Expression exp1, Expression exp2, Comparator comparator) {
        this.exp1= exp1;
        this.exp2= exp2;
        this.comparator = comparator;
        this.binary = true;
    }
    public Constraint(Expression exp1, int value, Comparator comparator){
        this.exp1 = exp1;
        this.value = value;
        this.comparator = comparator;
        this.binary = false;
    }
    public Expression getExp1(){
        return this.exp1;
    }
    public Expression getExp2() {
        return this.exp2;
    }
    public int getValue() {
        return this.value;
    }
    public boolean getComplex() {
        return this.binary;
    }
    public Comparator getOperator() {
        return this.comparator;
    }
    public boolean containsVar(DecisionVariable var) {
    	return exp1.getVar().equals(var)||exp2.getVar().equals(var);
    }
    
    public boolean checkComparison(int var1, int var2) {
    	switch(comparator) {
    		case MORE:
    			if(var1>var2)
    				return true;
    			break;
    		case LESS:
    			if(var1<var2)
    				return true;
    			break;
    		case EQ:
    			if(var1==var2)
    				return true;
    			break;
    		case NEQ:
    			if(var1!=var2) 
    				return true;
    			break;
    		case MEQ:
    			if(var1>=var2) 
    				return true;
    			break;
    		case LEQ:
    			if(var1<=var2)
    				return true;
    			break;
    		default:
    			return false;
    	}
    	return false;
    }
    
    public String toString() {
    	return exp1.toString() +" "+ comparator+ " "+ exp2.toString();
    }
    
}
