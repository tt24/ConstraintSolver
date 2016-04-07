import java.util.ArrayList;

public class Matrix {
	private String name;
	private ArrayList<ArrayList<DecisionVariable>> matrix = new ArrayList<>();
	public Matrix(String name, ArrayList<ArrayList<DecisionVariable>> matrix) {
		this.name = name;
		this.matrix = matrix;
	}
	public ArrayList<ArrayList<DecisionVariable>> getMatrix() {
		return this.matrix;
	}
	public String getName() {
		return this.name;
	}
	public String toString() {
		return this.matrix.toString();
	}

}
