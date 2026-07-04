package secvault;

import java.io.Serializable;

public class FingerTable implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int start;
    public int successor;

    public FingerTable(int start, int successor) {
        this.start = start;
        this.successor = successor;
    }

    @Override
    public String toString() {
        return "Start: " + start + ", Successor: " + successor;
    }
}
