import java.io.Serializable;

public class Payload implements Serializable {
    private static final long serialVersionUID = 1003L;

    // Class Representing the Payload For An Actual Message
    private PayloadTypes type;
    private BitFieldArrayBits bfa = null;
    private Integer index = null;
    private BitField content = null;

    public enum PayloadTypes {
        BitFieldArray_Type,
        PieceIndex_Type,
        PieceContent_Type
    }

    // Constructor For Payload With Bitfield Array (Bitfield)
    public Payload(PayloadTypes _type, BitFieldArrayBits bitfield_bits) {
        this.type = _type;
        this.bfa = bitfield_bits;
    }

    // Constructor For Payload With Piece Index (Request/Have)
    public Payload(PayloadTypes _type, Integer data) {
        this.type = _type;
        this.index = data;
    }

    // Constructor For Payload With Bitfield Piece (Piece)
    public Payload(PayloadTypes _type, BitField data) {
        this.type = _type;
        this.content = data;
    }

    public PayloadTypes getType() {
        return type;
    }

    public void setType(PayloadTypes type) {
        this.type = type;
    }

    public BitFieldArrayBits getPayloadBFA() {
        return this.bfa;
    }

    public Integer getPayloadIndex() {
        return this.index;
    }

    public BitField getPayloadPiece() {
        return this.content;
    }
    
    public int payloadLength() {
        switch(this.type) {
            case BitFieldArray_Type:
                return getPayloadBFA().totalLength;
            case PieceIndex_Type:
                return 4;
            case PieceContent_Type:
                return getPayloadPiece().length;
            default:
                return 0;
        }
    }
}