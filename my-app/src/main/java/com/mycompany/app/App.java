package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.StandardCopyOption;

public class App {
    
    private static final String ARTIST_XPATH = "//input[@name='artist']";
    private static final String TITLE_XPATH = "//input[@name='title']";
    private static final String CASE_XPATH = "//input[@name='template' and @value='jewel']";
    private static final String SIZE_XPATH = "//input[@name='size' and @value='a4']";
    private static final String SUBMIT_XPATH = "//input[@name='submit']";
    
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\ilya_\\Downloads\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");
        
        WebDriver driver = new ChromeDriver();
        
        try {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("ST-8: Генерация обложки для CD");
            System.out.println("=".repeat(60));

            String[] inputData = readDataFromFile("../data/data.txt");
            String performer = inputData[0];
            String record = inputData[1];
            String[] songList = inputData[2].split("\n");
            
            System.out.println("Исполнитель: " + performer);
            System.out.println("Альбом: " + record);
            System.out.println("Количество треков: " + songList.length);

            driver.get("http://www.papercdcase.com");
            Thread.sleep(3000);

            WebElement performerField = driver.findElement(By.xpath(ARTIST_XPATH));
            performerField.clear();
            performerField.sendKeys(performer);

            WebElement albumField = driver.findElement(By.xpath(TITLE_XPATH));
            albumField.clear();
            albumField.sendKeys(record);

            for (int idx = 0; idx < songList.length && idx < 16; idx++) {
                String trackNum = String.valueOf(idx + 1);
                String trackTitle = songList[idx].trim();
                if (trackTitle.matches("^\\d+\\..*")) {
                    trackTitle = trackTitle.substring(trackTitle.indexOf('.') + 1).trim();
                }
                WebElement trackInput = driver.findElement(By.name("track" + trackNum));
                trackInput.clear();
                trackInput.sendKeys(trackTitle);
            }

            WebElement caseOption = driver.findElement(By.xpath(CASE_XPATH));
            if (!caseOption.isSelected()) {
                caseOption.click();
            }

            WebElement sizeOption = driver.findElement(By.xpath(SIZE_XPATH));
            if (!sizeOption.isSelected()) {
                sizeOption.click();
            }

            WebElement submitBtn = driver.findElement(By.xpath(SUBMIT_XPATH));
            submitBtn.click();

            Thread.sleep(5000);

            savePdfFile(driver);
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("PDF сгенерирован и сохранён!");
            System.out.println("=".repeat(60));
            
            Thread.sleep(3000);
            
        } catch (Exception err) {
            System.out.println("Ошибка: " + err.getMessage());
            err.printStackTrace();
        } finally {
            driver.quit();
        }
    }
    
    private static String[] readDataFromFile(String filePath) throws Exception {
        Path fileLocation = Paths.get(filePath);

        if (!Files.exists(fileLocation)) {
            File parentFolder = fileLocation.getParent().toFile();
            if (!parentFolder.exists()) {
                parentFolder.mkdirs();
            }
            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
                writer.println("ARTIST=The Midnight Echoes");
                writer.println("ALBUM=Neon Dreams");
                writer.println("TRACKS=Starlight Avenue, Midnight Drive, Neon Lights, Echoes in the Dark, City of Dreams, Digital Rain, After Midnight, Lost in the Echo, Synthwave Sunset, Nightfall, Electric Heart, Retro Future, Memory Lane, Distant Signals, Neon Skies, The Last Dance");
            }
            System.out.println("Создан файл: " + filePath);
        }
        
        List<String> fileLines = Files.readAllLines(fileLocation);
        
        String artistName = "";
        String albumName = "";
        String tracksList = "";
        
        for (String line : fileLines) {
            if (line.startsWith("ARTIST=")) {
                artistName = line.substring(7).trim();
            } else if (line.startsWith("ALBUM=")) {
                albumName = line.substring(6).trim();
            } else if (line.startsWith("TRACKS=")) {
                tracksList = line.substring(7).trim().replace(", ", "\n");
            }
        }
        
        return new String[]{artistName, albumName, tracksList};
    }

    private static void savePdfFile(WebDriver driver) {
        try {
            File outputDir = new File("../result");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            String currentUrl = driver.getCurrentUrl();

            for (String windowHandle : driver.getWindowHandles()) {
                driver.switchTo().window(windowHandle);
            }

            try (InputStream inputStream = new java.net.URL(driver.getCurrentUrl()).openStream()) {
                Files.copy(inputStream, Paths.get("../result/cd.pdf"), StandardCopyOption.REPLACE_EXISTING);
            }
            
            System.out.println("PDF сохранён в: ../result/cd.pdf");
            
        } catch (Exception err) {
            System.out.println("Ошибка при сохранении PDF: " + err.getMessage());
        }
    }
}
