import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class Consumer implements Runnable{
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final BlockingQueue<Task> queue;
    private static final int MAX_RETRIES = 3;
    final static private String url = "http://18.246.212.185:8080//AlbumServlet_war/Albums/";
    private Counter unsuccessfulCounter;
    private CountDownLatch completed;
    public Consumer(BlockingQueue<Task> queue, Counter unsuccessfulCounter, CountDownLatch completed) {
        this.queue = queue;
        this.unsuccessfulCounter = unsuccessfulCounter;
        this.completed = completed;
    }

    public void run(){
        while (!queue.isEmpty()){
            try {
                Task task = queue.take();
                postRequest(task);
                getRequest(task);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        completed.countDown();
    }

    private void postRequest(Task task) {
        int retries = 0;
        boolean retry;
        String jsonData = toJson(task.getAlbumID(), task.getAlbumDetails().getImageSize());
        do {
            try {
                CompletableFuture<HttpResponse<String>> response1 = httpClient.sendAsync(postR(jsonData), HttpResponse.BodyHandlers.ofString());
                retry = false;
                response1.get();
            } catch (Exception e){
                retries += 1;
                retry = true;
            }
        } while (retry && (retries < MAX_RETRIES));
        if (retries == MAX_RETRIES){
            unsuccessfulCounter.inc();
        }

    }
    private void getRequest(Task task) {
        int retries = 0;
        boolean retry;
        String albumID = task.getAlbumID();
        do {
            try {
                CompletableFuture<HttpResponse<String>> response1 = httpClient.sendAsync(getR(albumID), HttpResponse.BodyHandlers.ofString());
                retry = false;
                response1.get();
            } catch (Exception e) {
                retries += 1;
                retry = true;
            }
        } while (retry && (retries < MAX_RETRIES));
        if (retries == MAX_RETRIES){
            unsuccessfulCounter.inc();
        }
    }

    private HttpRequest postR(String jsonData) {
        return HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .uri(URI.create(url))
                .header("User-Agent", "Java 11 HttpClient Bot")
                .header("Content-Type", "application/json")
                .build();
    }

    private HttpRequest getR(String str){
        return HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url + str))
                .header("User-Agent", "Java 11 HttpClient Bot")
                .build();
    }

    private String toJson(String albumID, String imageSize){
        return String.format("{\"albumID\":\"%s\", \"imageSize\":\"%s\"}", albumID, imageSize);
    }
}
