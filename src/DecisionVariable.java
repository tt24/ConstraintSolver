import java.util.TreeSet;


public class DecisionVariable {
    
    private String name;
    private TreeSet<Integer> domain = new TreeSet<Integer>() {};
    public DecisionVariable(String name, TreeSet<Integer> domain) {
        this.name = name;
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
    
    
    
    
}
