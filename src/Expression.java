
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

	public Integer solve() {
		Integer varValue;
		if ((varValue = var.getAssignedValue()) != null) {
			return solve(varValue);
		} else {
			return null;
		}
	}

	public Integer solve(int varValue) {
		if (operator != null) {
			int left;
			int right;
			if (order) {
				left = varValue;
				right = value;
			} else {
				left = value;
				right = varValue;
			}
			switch (operator) {
			case PLUS:
				return left + right;
			case MINUS:
				return left - right;
			case MULTIPLY:
				return left * right;
			case DIVIDE:
				return left / right;
			default:
				return null;
			}
		} else {
			return varValue;
		}
	}

	public String toString() {
		if (this.operator != null) {
			return this.var.getName() + " " + operator + " " + this.value + " " + this.order;
		}
		return this.var.getName();
	}

}
