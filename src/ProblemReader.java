import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import javax.naming.RefAddr;

public class ProblemReader {
	
	public void readGiven(String s) {
	}
	
	//[a b c]
	public static ArrayList<ArrayList>  readMatrix(String s) {
		ArrayList<ArrayList> matrix = new ArrayList<>();
		s = s.replace("[", "");
		s = s.replace("]", "");
		System.out.println(s);
		String[] rows = s.split(",");
		for(int i = 0; i<rows.length; i++) {
			ArrayList<Integer> row = new ArrayList<>();
			String[] elements = rows[i].split(" ");
			for(int j = 0; j<elements.length; j++) {
				if(!elements[j].equals("")) {
					row.add(Integer.parseInt(elements[j]));
				}
			}
			matrix.add(row);
		}
		return matrix;
	}
	
	// int(k..m)
	public static ArrayList<Integer> readRange(String s) {
		ArrayList<Integer> range = new ArrayList<>();
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
	
	public static void main(String[] args) {
		String dom = "int(1-10)";
		String list = "[1 8 6]";
		System.out.println(readMatrix(list));
	}

}
