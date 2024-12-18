import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;

public class RobustFileDownloader {

    // Method to check if the URL is valid
    public static boolean isValidURL(String urlString) {
        try {
            new URI(urlString);  // Use URI instead of URL
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Method to download the file using HttpClient
    public static void downloadFile(String fileURL, String saveDir) throws IOException, InterruptedException {
        // Validate the URL
        if (!isValidURL(fileURL)) {
            throw new IllegalArgumentException("The URL provided is not valid.");
        }

        // Create the directory if it doesn't exist
        Path dirPath = Paths.get(saveDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            System.out.println("Directory created: " + saveDir);
        }

        // Create an HttpClient instance
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fileURL))
                .build();

        // Send GET request to download the file
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        // Get the response code
        int responseCode = response.statusCode();
        if (responseCode == 200) { // HTTP_OK
            // Get the file name from the URL or response headers
            String fileName = "";
            HttpHeaders headers = response.headers();
            List<String> contentDisposition = headers.allValues("Content-Disposition");

            if (!contentDisposition.isEmpty() && contentDisposition.get(0).contains("attachment")) {
                // Extract the file name from Content-Disposition header
                String disposition = contentDisposition.get(0);
                int index = disposition.indexOf("filename=");
                if (index > 0) {
                    fileName = disposition.substring(index + 10, disposition.length() - 1);
                }
            } else {
                // If there's no Content-Disposition header, get the file name from the URL
                fileName = new File(URI.create(fileURL).getPath()).getName();
            }

            // Save the file to the specified directory
            String saveFilePath = saveDir + File.separator + fileName;
            try (InputStream inputStream = response.body();
                 FileOutputStream outputStream = new FileOutputStream(saveFilePath)) {

                // Get the file size to show the progress
                int fileSize = response.headers().firstValue("Content-Length").map(Integer::parseInt).orElse(0);

                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
                System.out.println("Downloading: " + fileName);

                // Start downloading the file with progress
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    totalBytesRead += bytesRead;
                    outputStream.write(buffer, 0, bytesRead);

                    // Print progress
                    if (fileSize > 0) {
                        int progress = (int) ((totalBytesRead * 100) / fileSize);
                        System.out.print("\rProgress: " + progress + "%");
                    }
                }
                System.out.println("\nDownload complete: " + fileName);
            }
        } else {
            System.out.println("No file to download. Server returned HTTP code: " + responseCode);
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java RobustFileDownloader <fileURL> [saveDir]");
            return;
        }

        // Get the file URL from the command-line arguments
        String fileURL = args[0];

        // Get the save directory from the command-line arguments or use the current directory if not provided
        String saveDir = args.length > 1 ? args[1] : "."; // Default to current directory if no directory is provided

        // Validate the URL
        if (!isValidURL(fileURL)) {
            System.out.println("Error: The URL provided is not valid.");
            return;
        }

        // Try to download the file using the provided URL and save directory
        try {
            downloadFile(fileURL, saveDir);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IOException | InterruptedException e) {
            System.out.println("Error downloading file: " + e.getMessage());
        }
    }
}

// usages
// java RobustFileDownloader <fileURL> [saveDir]
