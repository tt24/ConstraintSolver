
import java.util.TreeSet;

public class DecisionVariable implements Comparable<DecisionVariable> {

    private String name = null;
    private TreeSet<Integer> domain = new TreeSet<>();
    private TreeSet<Integer> initialDomain = new TreeSet<>();
    private Integer assignedValue;

    public DecisionVariable(String name, TreeSet<Integer> domain) {
        this.name = name;
        this.domain = domain;
        this.assignedValue = null;
        this.initialDomain = (TreeSet<Integer>) domain.clone();
    }

    public DecisionVariable(TreeSet<Integer> domain) {
        this.domain = domain;
        this.assignedValue = null;
        this.initialDomain = (TreeSet<Integer>) domain.clone();
    }

    public String getName() {
        return this.name;
    }

    public TreeSet<Integer> getDomain() {
        return this.domain;
    }

    public void setDomain(TreeSet<Integer> domain) {
        this.domain = (TreeSet<Integer>) domain.clone();
    }

    public String toString() {
        return name + " " + domain.toString();
    }

    public boolean equals(DecisionVariable var) {
        return (var.getName().equals(this.name));
    }

    public boolean isEmptyDomain() {
        return domain.isEmpty();
    }

    public void clearAssignment() {
        this.assignedValue = null;
    }

    public void setAssignedValue(int value) {
        this.assignedValue = value;
    }

    public Integer getAssignedValue() {
        return this.assignedValue;
    }

    @Override
    public int compareTo(DecisionVariable other) {
        return Integer.compare(this.domain.size(), other.domain.size());
    }
    
    public void setInitialParameters() {
        domain = (TreeSet<Integer>) initialDomain.clone();
        assignedValue = null;
    }

}
