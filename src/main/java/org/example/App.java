package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class App {

    private static final String CHROME_DRIVER_PATH = "/Users/alexandr/Downloads/chromedriver-mac-arm64/chromedriver";
    private static final String BASE_URL = "http://www.papercdcase.com/";

    private static final String ARTIST_INPUT_XPATH = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input";
    private static final String TITLE_INPUT_XPATH = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input";
    private static final String TRACK_INPUTS_XPATH = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr/td[2]/input";
    private static final String TYPE_JEWEL_CASE_XPATH = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]";
    private static final String PAPER_A4_XPATH = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]";
    private static final String CREATE_BUTTON_XPATH = "/html/body/table[2]/tbody/tr/td[1]/div/form/p/input";

    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--allow-insecure-localhost");
        options.addArguments("--ignore-certificate-errors");

        WebDriver driver = new ChromeDriver(options);

        try {
            AlbumInfo albumDetails = parseMusicDataFile("data/data.txt");

            driver.get(BASE_URL);
            Thread.sleep(2000);

            WebElement artistInputBox = driver.findElement(By.xpath(ARTIST_INPUT_XPATH));
            artistInputBox.sendKeys(albumDetails.getArtist());

            WebElement albumTitleInput = driver.findElement(By.xpath(TITLE_INPUT_XPATH));
            albumTitleInput.sendKeys(albumDetails.getTitle());

            List<WebElement> trackFields = driver.findElements(By.xpath(TRACK_INPUTS_XPATH));
            List<String> songNames = albumDetails.getTracks();

            for (int i = 0; i < trackFields.size() && i < songNames.size(); i++) {
                trackFields.get(i).sendKeys(songNames.get(i));
            }

            WebElement jewelCaseRadio = driver.findElement(By.xpath(TYPE_JEWEL_CASE_XPATH));
            jewelCaseRadio.click();

            WebElement a4Radio = driver.findElement(By.xpath(PAPER_A4_XPATH));
            a4Radio.click();

            WebElement createButton = driver.findElement(By.xpath(CREATE_BUTTON_XPATH));
            createButton.click();

            Thread.sleep(3000);

            String pdfUrl = driver.getCurrentUrl();
            String httpPdfUrl = pdfUrl.replace("https://", "http://");

            if (httpPdfUrl != null && httpPdfUrl.contains(".pdf")) {
                fetchPdfFile(httpPdfUrl, "result/cd.pdf");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                Thread.sleep(3000);
                driver.quit();
            } catch (InterruptedException e) {
                driver.quit();
            }
        }
    }

    private static void fetchPdfFile(String pdfUrl, String outputPath) throws IOException {
        Path resultPath = Paths.get("result");
        if (!Files.exists(resultPath)) {
            Files.createDirectories(resultPath);
        }

        URL url = new URL(pdfUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.connect();

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (InputStream in = connection.getInputStream()) {
                Path targetPath = resultPath.resolve("cd.pdf");
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            throw new IOException("Не удалось скачать PDF, код ответа: " + responseCode);
        }

        connection.disconnect();
    }

    private static AlbumInfo parseMusicDataFile(String filePath) throws IOException {
        List<String> allLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    allLines.add(line);
                }
            }
        }

        if (allLines.size() < 2) {
            throw new IOException("Файл должен содержать минимум 2 строки: Artist и Title");
        }

        String artist = allLines.get(0);
        String albumName = allLines.get(1);
        List<String> songNames = new ArrayList<>();
        for (int i = 2; i < allLines.size(); i++) {
            songNames.add(allLines.get(i));
        }

        return new AlbumInfo(artist, albumName, songNames);
    }

    static class AlbumInfo {
        private final String artist;
        private final String albumName;
        private final List<String> songNames;

        public AlbumInfo(String artist, String albumName, List<String> songNames) {
            this.artist = artist;
            this.albumName = albumName;
            this.songNames = songNames;
        }

        public String getArtist() { return artist; }
        public String getTitle() { return albumName; }
        public List<String> getTracks() { return songNames; }
    }
}