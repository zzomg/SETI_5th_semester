package utility;

import java.nio.ByteBuffer;

// TODO: refactor this

public class ObservableByteBuffer
{
    private ByteBuffer byteBuffer;
    private boolean isShutdown = false;

    private BufferListener bufferListener;

    public ObservableByteBuffer(ByteBuffer byteBuffer)
    {
        this.byteBuffer = byteBuffer;
    }

    public ByteBuffer getBuf() { return byteBuffer; }

    public void notifyListener() {
        bufferListener.onUpdate();
    }

    public void registerBufferListener(BufferListener bufferListener) {
        this.bufferListener = bufferListener;
    }

    public void shutdown()
    {
        isShutdown = true;
    }

    public boolean isReadyToClose() {
        return byteBuffer.remaining() == 0 && isShutdown;
    }
}
