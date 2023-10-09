import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class clientPart2 {
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

    public static void main(String[] args) throws InterruptedException, IOException {
        long startTime = System.currentTimeMillis();
        final CountDownLatch latch = new CountDownLatch(threadGroupSize * numThreadGroups);

        ExecutorService executorService = Executors.newFixedThreadPool(threadGroupSize);

        // Create a CSV file for recording data
        String csvFileName = "GoTestGroup3.csv";
        BufferedWriter csvWriter = new BufferedWriter(new FileWriter(csvFileName));
        csvWriter.write("Start Time,Request Type,Latency (ms),Response Code\n");

        List<Double> postResponseTimes = new ArrayList<>();
        List<Double> getResponseTimes = new ArrayList<>();

        for (int i = 0; i < numThreadGroups; i++) {
            // Start a thread group
            for (int j = 0; j < threadGroupSize; j++) {
                executorService.execute(() -> {
                    sendRequests(totalRequests, csvWriter, postResponseTimes, getResponseTimes);
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

        calculateStatistics("POST", postResponseTimes);
        calculateStatistics("GET", getResponseTimes);

        csvWriter.close();

        executorService.shutdown();
    }

    private static void calculateStatistics(String type, List<Double> responseTimes) {
        // Calculate mean response time
        double meanResponseTime = responseTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // Calculate median response time
        Collections.sort(responseTimes);
        double medianResponseTime = responseTimes.get(responseTimes.size() / 2);

        // Calculate p99 (99th percentile) response time
        int p99Index = (int) Math.ceil(0.99 * responseTimes.size());
        double p99ResponseTime = responseTimes.get(p99Index - 1);

        // Calculate min and max response times
        double minResponseTime = Collections.min(responseTimes);
        double maxResponseTime = Collections.max(responseTimes);

        System.out.println("Request Type: " + type);
        System.out.println("Mean Response Time (ms): " + meanResponseTime);
        System.out.println("Median Response Time (ms): " + medianResponseTime);
        System.out.println("P99 Response Time (ms): " + p99ResponseTime);
        System.out.println("Min Response Time (ms): " + minResponseTime);
        System.out.println("Max Response Time (ms): " + maxResponseTime);
    }

    private static void sendRequests(int numRequests, BufferedWriter csvWriter, List<Double> postResponseTimes, List<Double> getResponseTimes) {
        for (int i = 0; i < numRequests; i++) {

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
                long time2 = System.currentTimeMillis();
                double wallTime = (time2 - time1);
                long time3 = System.currentTimeMillis();
                response2 = httpClient.send(request2, HttpResponse.BodyHandlers.ofString());
                long time4 = System.currentTimeMillis();
                double wallTime2 = (time4 - time3);

                // Record data in CSV format
                recordData(response, csvWriter, wallTime, time1, "GET");
                recordData(response2, csvWriter, wallTime2, time2, "POST");

                // adding to the list
                getResponseTimes.add(wallTime);
                postResponseTimes.add(wallTime2);


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

    private static void recordData (HttpResponse<String> response, BufferedWriter csvWriter,double wallTime, long time1,
                                    String requestType) throws IOException {
        String startTime = String.valueOf(time1);
        String latency = String.valueOf(wallTime);
        String responseCode = String.valueOf(response.statusCode());
        csvWriter.write(startTime + "," + requestType + "," + latency + "," + responseCode + "\n");

    }

}
