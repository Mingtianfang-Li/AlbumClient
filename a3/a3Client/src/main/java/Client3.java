import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class Client3 {
    private static final int threadGroupSize = 10;
    private static final int numThreadGroups = 10;
    private static final int delay = 2;
    private static final int numProducer = 10;
    private static final int totalRequests = 100;
    private static final int NUM_QUEUE_SIZE = 100;

    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();

        Counter unsuccessfulCounter = new Counter();
        BlockingQueue<Task> queue = new LinkedBlockingQueue<>(NUM_QUEUE_SIZE);
        int taskNum = totalRequests / numProducer;
        SyncCounter syncCounter = new SyncCounter(taskNum);
        for (int i = 0; i < numProducer; i++) {
            Runnable producer = new Producer(taskNum, queue, syncCounter);
            Thread thread = new Thread(producer);
            thread.start();
        }
        for (int j = 0; j < threadGroupSize; j++) {
            CountDownLatch completed = new CountDownLatch(numThreadGroups);
            for (int i = 0; i < numThreadGroups; i++) {
                new Thread(new Consumer(queue, unsuccessfulCounter, completed)).start();
            }
            completed.await();
            Thread.sleep(delay * 1000);
        }
        long end = System.currentTimeMillis();
        long wallTime = end - start;
        int successfulCount = (threadGroupSize*totalRequests - unsuccessfulCounter.getVal());
        System.out.println("total time: " + wallTime + " ms");
        System.out.println("number of successful requests sent is: " + successfulCount);
        System.out.println("number of unsuccessful requests is " + unsuccessfulCounter.getVal());
        System.out.println("Throughput: " + successfulCount * 1000L / wallTime);
    }
}
