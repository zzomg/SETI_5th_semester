package TreeChat.Master;

import TreeChat.TreeNode;

import java.util.TimerTask;

public class MessageBufferMaster extends TimerTask {
    private final TreeNode node;
    public MessageBufferMaster(TreeNode node){
        this.node = node;
    }

    @Override
    public void run() {
        var savedPacketsToSend = node.getSavedPacketsToSend();
        savedPacketsToSend.removeIf(item -> item.getTtl() < 1);
        var packetsToSend = node.getPacketsToSend();
        packetsToSend.addAll(savedPacketsToSend);
    }
}

