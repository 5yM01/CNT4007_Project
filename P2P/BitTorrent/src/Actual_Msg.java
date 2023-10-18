import java.io.Serializable;

public class Actual_Msg implements Message, Serializable {
    // Class Representing Actual Message
    private int length;
    private Type msgType;
    // TODO: Turn BitfieldArray here into just object?
    private BitFieldArray payload;

    public Actual_Msg(Type _type) {
        this.msgType = _type;
        this.payload = null;
    }

    public Actual_Msg(Type _type, BitFieldArray data) {
        this.msgType = _type;
        this.payload = data;
    }

    public BitFieldArray getPayload() {
        return this.payload;
    }

    public void print() {
        System.out.println(msgType);
    }

    public Type getMsgType() {
        return this.msgType;
    }

    public void setLength() {
        this.length = payloadLength() + 1;
    }

    public int payloadLength() {
        // TODO: Get length of payload
        return getPayload().totalLength;
    }
}
