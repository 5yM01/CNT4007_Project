import java.io.Serializable;

public class BitField implements Serializable {
    // Class Representing Singular BitField in BitFieldArray
    private static final long serialVersionUID = 1005L;

    public int data = 0; // Data stored as int between 0 and 255
    public int id;

    public BitField(){};

    public BitField(int val, int _id) {
        this.id = _id;
        setData(val);
    }

    public void setData(int val) {
        if (this.checkIfValidByte(val)) {
            this.data = val;
        } else {
            throw new IllegalArgumentException("Value " + val + " Not Valid Byte!");
        }

    }

    public Boolean checkIfValidByte(int val) {
        return (val >= 0 && val <= 255);
    }
}