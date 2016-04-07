import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import javax.naming.RefAddr;

public class ProblemReader {
	
	public void readGiven(String s) {
	}
	
	//[a b c]
	public static TreeSet<Integer>  readList(String s) {
		TreeSet<Integer> list = new TreeSet<>();
		s = s.replace("[", "");
		s = s.replace("]", "");
		String elements[] = s.split(" ");
		for(int i = 0; i<elements.length; i++) {
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
		while(index<=Integer.parseInt(stEnd[1])) {
			range.add(index);
			index++;
		}
		return range;
	}
	
	public static Matrix createMatrix(String name, int n, int m, TreeSet<Integer> domain) {
		ArrayList<ArrayList<DecisionVariable>> matrix = new ArrayList<>();
		for(int i = 0; i<n; i++) {
			ArrayList<DecisionVariable> row = new ArrayList<>();
			for(int j = 0; j<m; j++) {
				row.add(new DecisionVariable(domain));
			}
			matrix.add(row);
		}
		return new Matrix(name, matrix);		
	}
	
	public static void main(String[] args) {
		String dom = "int(1-10)";
		String list = "[1 8 6]";
		TreeSet<Integer> domain = readRange(dom);
		System.out.println(createMatrix("mm", 3,2,domain));
	}

}
