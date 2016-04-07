import java.util.ArrayList;

public class Matrix {
	
	private ArrayList<ArrayList<DecisionVariable>> matrix = new ArrayList<>();
	public Matrix(ArrayList<ArrayList<DecisionVariable>> matrix) {
		this.matrix = matrix;
	}
	public ArrayList<ArrayList<DecisionVariable>> getMatrix() {
		return this.matrix;
	}

}
