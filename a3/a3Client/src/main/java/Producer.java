import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;

public class Producer implements Runnable{
    private int workNum;
    private BlockingQueue<Task> queue;
    private SyncCounter remainingWork;

    public Producer(int workNum, BlockingQueue<Task> queue, SyncCounter remainingWork) {
        this.workNum = workNum;
        this.queue = queue;
        this.remainingWork = remainingWork;
    }

    @Override
    public void run() {
        for (int i = 0; i < workNum; i++){
            Task task = new Task(ContentGererator.createBody());
            try {
                queue.put(task);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        remainingWork.decrease();
    }
}
