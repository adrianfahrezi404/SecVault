package secvault;

import java.io.Serializable;

/**
 * Merepresentasikan satu baris dalam Finger Table DHT.
 */
public class FingerTableEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int start;
    public int successor;

    public FingerTableEntry(int start, int successor) {
        this.start = start;
        this.successor = successor;
    }

    @Override
    public String toString() {
        return "Start: " + start + ", Successor: " + successor;
    }
}
