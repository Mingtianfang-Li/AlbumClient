import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonIOException;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;

@WebServlet(name = "AlbumServlet", value = "/AlbumServlet")
public class AlbumServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        String urlPath = request.getPathInfo();
        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("missing parameters");
            return;
        }

        String[] urlParts = urlPath.split("/");

        if (!isUrlValid(urlParts)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            // do any sophisticated processing with urlParts which contains all the url params
            // TODO: process url params in `urlParts`
            response.getWriter().write("It works!");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        processRequest(request, response);
        response.setContentType("application/json");

//        Gson gson = new Gson();

        // Retrieve the request's input stream
        try (BufferedReader reader = request.getReader()) {
//            StringBuilder sb = new StringBuilder();
//            String line;
//
//            while ((line = reader.readLine()) != null) {
//                sb.append(line);
//            }
//
//            // Convert the JSON data into an instance of AlbumData
//            ImageMetaData albumData = gson.fromJson(sb.toString(), ImageMetaData.class);
//
//            // You can now access the albumID and imageSize fields
//            String albumID = albumData.getAlbumID();
//            String imageSize = albumData.getImageSize();

            // Handle the albumID and imageSize as needed
            // For example, you can process them or store them in your application

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("JSON data received and processed successfully.");
        } catch (Exception ex) {
            ex.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Error processing JSON data.");
        }
    }

//    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
//        response.setContentType("application/json");
////    Gson gson = new Gson();
//        String urlPath = request.getPathInfo();
//        long start = System.currentTimeMillis();
//        if (urlPath == null || urlPath.isEmpty()) {
//            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//            response.getWriter().write("missing parameters");
//            return;
//        }
//
//        String[] urlParts = urlPath.split("/");
//
//        response.setStatus(HttpServletResponse.SC_OK);
//        try {
//            StringBuilder sb = new StringBuilder();
//            String s;
//            while ((s = request.getReader().readLine()) != null) {
//                sb.append(s);
//            }
////        System.out.println(sb.toString());
////        Swipe swipe = (Swipe) gson.fromJson(sb.toString(), Swipe.class);
//
//            response.setStatus(HttpServletResponse.SC_OK);
//            response.getWriter().write("It works!");;
//            long end =  System.currentTimeMillis();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//        }
//    }

    private boolean isUrlValid(String[] urlParts) {
        return true;
    }
}
