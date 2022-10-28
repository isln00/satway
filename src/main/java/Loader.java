import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Loader {
    private static final String mainUrl = "https://satway.ru/articles/";
    private static final String indexPath = "src/main/resources/meta/index.html";
    private static final String rawPath = "src/main/resources/raw/";

    public static List<String> getHrefsOnline() throws InterruptedException, IOException {
        System.out.println("Loading index page");
        final By buttonClass = By.className("wpgb-load-more");

        WebDriver driver = new ChromeDriver();

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        driver.get(mainUrl);

        WebElement button;
        while (true) {
            try {
                button = driver.findElement(buttonClass);
                System.out.println("Found it!");
                System.out.println("Waiting...");
                Thread.sleep(1000);
                new Actions(driver).scrollToElement(button).perform();
                System.out.println("Scroll. Waiting...");
                Thread.sleep(1000);

            } catch (NoSuchElementException | ElementNotInteractableException nse) {
                break;
            }

            button.click();
            System.out.println("Click. Waiting...");
            Thread.sleep(2000);
        }

        String index = driver.getPageSource();
        FileUtils.writeStringToFile(new File(indexPath), index, StandardCharsets.UTF_8);

        return extractHrefsFromIndex(index);
    }

    public static List<String> getHrefsFromLocalIndex() throws IOException {
        System.out.println("Reading locally saved index page");
        List<String> hrefs;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(indexPath)))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                builder.append(line).append("\n");
            }
            String index = builder.toString();
            hrefs = Loader.extractHrefsFromIndex(index);
            return hrefs;
        }
    }

    public static List<String> extractHrefsFromIndex(String index) {

        Map<String, LocalDateTime> hrefDates = new HashMap<>();
        Document page = Jsoup.parse(index);
        Elements hrefNodes = page.select("div.wpgb-card-body");

        for (Element el : hrefNodes) {
            Element card = el.select("time.wpgb-block-2").first();
            hrefDates.put(
                    card.select("a").attr("href"),
                    LocalDateTime.parse(card.attr("datetime").replaceAll("\\+03:00", "")));
        }

        return hrefDates.keySet()
                .stream()
                .sorted((k1, k2) -> hrefDates.get(k2).compareTo(hrefDates.get(k1)))
                .collect(Collectors.toList());
    }

    public static void savePages(List<String> hrefs) {
        Map<String, String> urlToFilename = new HashMap<>();
        Response response;
        Document doc;
        String fileName;
        String[] splitUrl;
        String writeUrlToCommentTemplate = "<!--%s-->\n";

        int size = hrefs.size();
        int i = 0;

        for (String url : hrefs) {
            splitUrl = url.split("/");
            fileName = size - i + "-" + splitUrl[splitUrl.length - 1];
            i++;
            try {
                System.out.println("Loading " + url);
                Thread.sleep(500);
                response = Jsoup.connect(url).execute();
                doc = response.parse();
                String filePath = rawPath + fileName + ".html";
                urlToFilename.put(url, fileName + ".html");

                FileUtils.writeStringToFile(new File(filePath),
                        String.format(writeUrlToCommentTemplate, url) + doc.outerHtml(),
                        StandardCharsets.UTF_8);
                System.out.println(url + " loaded and saved");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            String json = objectMapper.writeValueAsString(urlToFilename);
            FileUtils.writeStringToFile(
                    new File("src/main/resources/meta/urlMap.json"),
                    json,
                    StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static List<String> getHrefsFromFirstPage() throws IOException {
        URL url = new URL(mainUrl);
        Document page = Jsoup.parse(url, 10000);
        List<String> hrefs = new ArrayList<>();
        Elements hrefNodes = page.select("a.wpgb-card-layer-link");

        for (Element el : hrefNodes) {
            hrefs.add(el.attr("href"));
        }
        return hrefs;
    }
}
