import java.io.Serializable;

public class Actual_Msg implements Message, Serializable {
    // Class Representing Actual Message
    private static final long serialVersionUID = 1002L;

    private int length;
    private Type msgType;

    // Payload Information
    private Payload payload;

    public Actual_Msg(Type _type) {
        this.msgType = _type;
        this.payload = null;
        this.length = 4;
    }

    public Actual_Msg(Type _msgType, Payload data) {
        this.msgType = _msgType;
        this.payload = data;
        this.length = 4 + data.payloadLength();
    }

    public Payload.PayloadTypes getPayloadType() {
        return this.payload.getType();
    }

    public Payload getPayload() {
        return this.payload;
    }

    public void print() {
        System.out.println(msgType);
    }

    public Type getMsgType() {
        return this.msgType;
    }

    public void setLength() {
        this.length = this.payload.payloadLength();
    }

    public int getPayloadLength() {
        return this.length;
    }
}
