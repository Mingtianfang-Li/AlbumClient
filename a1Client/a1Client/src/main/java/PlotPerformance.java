//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//
//public class plotPerformance {
//
//    public static void main(String[] args) {
//        String csvFilePath = "C:\\Users\\74559\\Desktop\\neu\\cs6650\\a1\\a1Client\\a1Client\\JavaTestGroup3.csv";
//}
package PlotPerformance;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;

public class PlotPerformance {

    public static void main(String[] args) {
        plotPerformance("C:\\Users\\74559\\Desktop\\neu\\cs6650\\a1\\a1Client\\a1Client\\JavaTestGroup3.csv");
    }
    public static long[] collectData(long start,long end)
            throws IOException {
        // 2. put record into buckets, 1s
        int bucketNum = (int) Math.ceil((end - start) / 1000); // index  1s = 1000ms
        long[] buckets = new long[bucketNum+1];
        return buckets;
    }

    public static void plotPerformance(String fileName) {

        try {
            File myObj = new File(fileName);
            Scanner myReader = new Scanner(myObj);
            List<Long> startTimes = new ArrayList<>();
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;
            while (myReader.hasNextLine()) {
                String line = myReader.nextLine();
                String[] records = line.split(",");
                min = Math.min(min,Long.parseLong(records[0]));
                max = Math.max(max, Long.parseLong(records[0]));
            }
            System.out.println("min: " + min + " max: " + max);
            try{
                long[] buckets= collectData(min, max);
                myReader = new Scanner(myObj);
                while (myReader.hasNextLine()) {
                    String line = myReader.nextLine();
                    String[] records = line.split(",");
                    long startTime = Long.valueOf(records[0]);
                    int index = (int) Math.ceil((startTime - min)/1000);
                    buckets[index]++;
                }
                // 3. write to a csv file
                writeFile(buckets);
            } catch (IOException e) {
                e.printStackTrace();
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static void writeFile(long[] buckets) throws IOException {
        // write to file
//    System.out.println(Arrays.toString(synList.toArray()));
        Format f=new SimpleDateFormat("dd-MM-yyyy-HH.mm.ss");
        String fileName = "Plot-"+ f.format(new Date())+"-output.csv";
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));

        for (int i = 0; i < buckets.length; i++) {
            writer.write(i + ",");
            writer.write(String.valueOf(buckets[i]));
            writer.write(System.lineSeparator());
        }
        writer.close();
    }
}
