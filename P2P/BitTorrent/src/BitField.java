import java.io.Serializable;

public class BitField implements Serializable {
    public int data = 0;
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