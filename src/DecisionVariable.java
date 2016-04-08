import java.util.TreeSet;


public class DecisionVariable {
    
    private String name;
    private TreeSet<Integer> domain = new TreeSet<>();
    private TreeSet<Integer> checkpointDomain = new TreeSet<>();
    private Integer assignedValue = null;
    public DecisionVariable(String name, TreeSet<Integer> domain) {
        this.name = name;
        this.domain = domain;
    }
    public DecisionVariable(TreeSet<Integer> domain) {
        this.domain = domain;
    }
    public String getName() {
        return this.name;
    }
    public TreeSet<Integer> getDomain() {
        return this.domain;
    }
    public void setDomain(TreeSet<Integer> domain) {
        this.domain= domain;
    }
    public String toString() {
    	return name+" "+domain.toString();
    }
    public boolean equals(DecisionVariable var) {
    	return(var.getName().equals(this.name));
    }
    public void checkpoint() {
    	this.checkpointDomain = (TreeSet<Integer>) domain.clone();
    }
    public void reverseCheckpoint() {
    	this.domain = (TreeSet<Integer>) checkpointDomain.clone();
    }
    public boolean isEmptyDomain() {
    	return domain.isEmpty();
    }
    public void clearAssignment() {
    	this.assignedValue=null;
    }
    public void setAssignedValue(int value) {
    	this.assignedValue = value;
    }
    public Integer getAssignedValue() {
    	return this.assignedValue;
    }
    
    
    
    
    
}
