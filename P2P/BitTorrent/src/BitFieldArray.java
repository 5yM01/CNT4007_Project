import java.io.Serializable;

public class BitFieldArray implements Serializable {
    // Class Representing Peer Bitfield Array
    public BitField[] fields;

    public BitFieldArray(){};

    public BitFieldArray(int size, Boolean isFull) {
        this.fields = new BitField[size];
        for (int i = 0; i < size; i++) {
            this.fields[i] = new BitField((isFull ? 255 : 0 ), i);
        }
    }

    public void setAllFields(int val) {
        for (BitField  b : this.fields) {
            b.setData(val);
        }
    }

    public void setAllFieldsToOne() {
        for (BitField  b : this.fields) {
            b.setData(255);
        }
    }
}