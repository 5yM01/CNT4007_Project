import java.io.Serializable;

public class Payload implements Serializable {
    private PayloadTypes type;
    private BitFieldArray bfa = null;
    private Integer index = null;
    private BitField content = null;
    private static final long serialVersionUID = 1003L;

    public enum PayloadTypes {
        BitFieldArray_Type,
        PieceIndex_Type,
        PieceContent_Type
    }

    public Payload(PayloadTypes _type, BitFieldArray data) {
        this.type = _type;
        this.bfa = data;

    }

    public Payload(PayloadTypes _type, Integer data) {
        this.type = _type;
        this.index = data;
        
    }
    
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

    public BitFieldArray getPayloadBFA() {
        return this.bfa;
    }

    public Integer getPayloadIndex() {
        return this.index;
    }

    public BitField getPayloadPiece() {
        return this.content;
    }
    
    public int payloadLength() {
        // TODO: Get length of payload
        switch(this.type) {
            case BitFieldArray_Type:
                return getPayloadBFA().totalLength;
            case PieceIndex_Type:
                return 4;
            case PieceContent_Type:
                return 4 + getPayloadPiece().data;
            default:
                return 0;
        }
    }
}