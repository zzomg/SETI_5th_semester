package sockets;

import java.nio.ByteBuffer;

public class SocketResponse
{
    private static final int RESPONSE_LEN = 10;
    private byte version = 0x05;
    private byte reply = 0x00;
    private byte addressType = 0x01;
    private byte[] boundIp4Addr;
    private short boundPort;

    public void setReply(byte reply)
    {
        this.reply = reply;
    }

    public void setBoundIp4Addr(byte[] boundIp4Addr) {
        this.boundIp4Addr = boundIp4Addr;
    }

    public void setBoundPort(short boundPort)
    {
        this.boundPort = boundPort;
    }

    public ByteBuffer toByteBuf() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(RESPONSE_LEN);
        byteBuffer.put(version)
                .put(reply)
                .put((byte) 0x00)
                .put(addressType)
                .put(boundIp4Addr)
                .putShort(boundPort);

        byteBuffer.flip();
        return byteBuffer;
    }

    public ByteBuffer toSimpleByteBuf()
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate(RESPONSE_LEN);
        byteBuffer.put(version)
                .put(reply)
                .put((byte) 0x00);

        byteBuffer.flip();
        return byteBuffer;
    }
}
