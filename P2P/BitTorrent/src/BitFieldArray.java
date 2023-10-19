import java.io.Serializable;

public class BitFieldArray implements Serializable {
    private static final long serialVersionUID = 1004L;

    // Class Representing Peer Bitfield Array
    public BitField[] fields;
    public int totalLength;

    public BitFieldArray(){};

    public BitFieldArray(int size, Boolean isFull) {
        this.fields = new BitField[size];
        for (int i = 0; i < size; i++) {
            this.fields[i] = new BitField((isFull ? 255 : 0 ), i);
        }
        this.totalLength = size;
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
    
    public int currBitFieldSize() {
        int curr = 0;

        for (BitField  b : this.fields) {
            if (b.data != 0) {
                curr += 1;
            };
        }

        return curr;
    }

    public int arraySize() {
        return this.totalLength;
    }
}