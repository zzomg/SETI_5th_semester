package utility;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Connection
{
    private ObservableByteBuffer outputBuffer;
    private ObservableByteBuffer inputBuffer;
    private SocketChannel socketChannel;
    private int startPos = 0;

    public Connection(ObservableByteBuffer outputBuffer, ObservableByteBuffer inputBuffer) {
        this.outputBuffer = outputBuffer;
        this.inputBuffer = inputBuffer;
    }

    public Connection(int buffLength) {
        this.inputBuffer = new ObservableByteBuffer(ByteBuffer.allocate(buffLength));
        this.outputBuffer = new ObservableByteBuffer(ByteBuffer.allocate(buffLength));
    }

    public ByteBuffer getOutputBuffer() { return outputBuffer.getBuf(); }

    public ByteBuffer getInputBuffer() { return inputBuffer.getBuf(); }

    public void setSockChannel(SocketChannel sc)
    {
        this.socketChannel = sc;
    }

    public ObservableByteBuffer getObservableOutputBuffer()
    {
        return outputBuffer;
    }

    public ObservableByteBuffer getObservableInputBuffer()
    {
        return inputBuffer;
    }

    public void registerBufferListener(BufferListener bufferListener) {
        inputBuffer.registerBufferListener(bufferListener);
    }

    public void notifyBufferListener()
    {
        outputBuffer.notifyListener();
    }

    public void closeSockChannel() throws IOException {
        if(socketChannel != null)
        {
            System.out.println("Socket closed: " + socketChannel.getRemoteAddress());
            socketChannel.close();
        }
    }

    public void shutdown()
    {
        outputBuffer.shutdown();
    }

    public boolean isBufShutDown() { return inputBuffer.isReadyToClose(); }

    public void setBuf() {
        ByteBuffer inputBuffer = getInputBuffer();
        inputBuffer.flip();
        inputBuffer.position(startPos);
    }

    public boolean canClose() {
        return outputBuffer.isReadyToClose() && inputBuffer.isReadyToClose();
    }

    public void resetStartPos()
    {
        this.startPos = 0;
    }

    public void setStartPos() {
        ByteBuffer inputBuffer = getInputBuffer();
        this.startPos = inputBuffer.position();

        int newStartPosition = inputBuffer.limit();
        inputBuffer.clear();
        inputBuffer.position(newStartPosition);
    }
}
