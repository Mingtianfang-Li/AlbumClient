import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class client {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final int threadGroupSize = 10;
    private static final int numThreadGroups = 30;
    private static final int delay = 2; // in seconds
    private static final String url = "http://35.87.33.215:8080/AlbumServlet_war/Albums/";// Replace with your server URI

    private static final String goUrl = "http://35.87.33.215:8080/AlbumStore/1.0.0/albums";
    private static final String getUrl = "http://35.87.33.215:8080/AlbumServlet_war/Albums/1";
    private static final String goGetUrl = "http://35.87.33.215:8080/AlbumStore/1.0.0/albums/1";
    private static final int totalRequests = 100;

    public static void main(String[] args) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        final CountDownLatch latch = new CountDownLatch(threadGroupSize * numThreadGroups);

        ExecutorService executorService = Executors.newFixedThreadPool(threadGroupSize);

        for (int i = 0; i < numThreadGroups; i++) {
            // Start a thread group
            for (int j = 0; j < threadGroupSize; j++) {
                executorService.execute(() -> {
                    sendRequests(totalRequests);
                    latch.countDown();
                });
            }

            // Wait for delay seconds
            try {
                Thread.sleep(delay * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Wait for all threads to complete
        latch.await();

        long endTime = System.currentTimeMillis();

        // Calculate wall time in seconds
        double wallTime = (endTime - startTime) / 1000.0;
        System.out.println("Wall Time: " + wallTime + " seconds");

        // Calculate throughput
        int totalRequestsSent = numThreadGroups * threadGroupSize * totalRequests * 2;
        double throughput = totalRequestsSent / wallTime;
        System.out.println("Throughput: " + throughput + " requests/second");

        executorService.shutdown();
    }

    private static void sendRequests(int numRequests) {
        double sum = 0;
        for (int i = 0; i < numRequests; i++) {
            // Simulate sending POST and GET requests here
            // You can use libraries like HttpClient to send HTTP requests
            // Example: CloseableHttpClient httpClient = HttpClients.createDefault();
            // Example: HttpPost httpPost = new HttpPost(IPAddr + "/post");
            // Example: HttpResponse response = httpClient.execute(httpPost);
            // Example: HttpGet httpGet = new HttpGet(IPAddr + "/get");
            // Example: HttpResponse response = httpClient.execute(httpGet);

            HttpRequest request1 = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(goGetUrl))
                    .setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
                    .build();

            String jsonData = "{\"albumID\":\"123\", \"imageSize\":\"321\"}";

            HttpRequest request2 = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                    .uri(URI.create(goUrl))
                    .setHeader("User-Agent", "Java 11 HttpClient Bot")
                    .header("Content-Type", "application/json") // Set the content type to JSON
                    .build();


            HttpResponse<String> response = null;
            HttpResponse<String> response2 = null;
            try {
                long time1 = System.currentTimeMillis();
                response = httpClient.send(request1, HttpResponse.BodyHandlers.ofString());
                response2 = httpClient.send(request2, HttpResponse.BodyHandlers.ofString());
                long time2 = System.currentTimeMillis();
                double wallTime = (time2 - time1);
                sum += wallTime;
                //    completed.countDown();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Sleep for a short time to simulate processing time
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
        }
//        double singleRequest = sum / totalRequests;
//        System.out.println("each request" + singleRequest/2 + "ms");
    }
}
