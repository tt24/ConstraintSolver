
public class Expression {
	private DecisionVariable var;
	private Operator operator;
	private int value;
	
	public Expression(DecisionVariable var, Operator operator, int value) {
		this.var = var;
		this.operator = operator;
		this.value = value;
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
	

}
