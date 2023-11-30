import java.io.Serializable;

public class Actual_Msg implements Message, Serializable {
    private static final long serialVersionUID = 1002L;

    // Class Representing Actual Message
    private Type msgType;
    private Payload payload;
    private int msg_length;

    // Constructor For Message With No Payload
    public Actual_Msg(Type _type) {
        this.msgType = _type;
        this.payload = null;
        this.msg_length = 4;
    }

    // Constructor For Message With Payload
    public Actual_Msg(Type _msgType, Payload data) {
        this.msgType = _msgType;
        this.payload = data;
        setLength();
    }

    public Type getMsgType() {
        return this.msgType;
    }

    public Integer getLength() {
        return this.msg_length;
    }

    public void setLength() {
        this.msg_length = 5 + this.payload.payloadLength();
    }

    public Payload getPayload() {
        return this.payload;
    }

    public Payload.PayloadTypes getPayloadType() {
        return this.payload.getType();
    }
}
