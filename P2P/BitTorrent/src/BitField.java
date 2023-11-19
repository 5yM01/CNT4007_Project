import java.io.Serializable;

public class BitField implements Serializable {
    private static final long serialVersionUID = 1005L;
    
    // Class Representing Singular BitField in BitFieldArray
    public byte[] data;
    public int id;
    public int length;
    public Boolean empty = true;

    public BitField(){};

    public BitField(byte[] val, int _id) {
        this.id = _id;
        setData(val);
    }

    public void setData(byte[] val) {
        this.data = val;
    }

    public void setPiece(byte[] data, int size) {
        this.length = size;
        setData(data);
        this.empty = false;
    }
}