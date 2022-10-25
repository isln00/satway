import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Article {
    public static final String XHTML_PATH = "src/main/resources/xhtml/";
    private final String path;
    private final File file;
    private final String title;
    private final String date;
    private static final Pattern paragraphsPattern = Pattern.compile("<section class=\"entry-content\">(?<flesh>[\\w\\W]*?)</section>");
    private static final Pattern articleUrlPattern = Pattern
        .compile("(?<full><a href=\"(?<url>https?://satway\\.ru/(articles|blog)/[\\w-]+/?)[\\w\\W]*?</a>)");
    private static final Pattern titlePattern = Pattern.compile("title=\"(?<title>[\\w\\W]*?)\"");
    private static final Pattern imgPattern = Pattern.compile("<img[\\w\\W]*?>");
    private final String url;
    private final String xhtml;
    private static final String xhtmlTemplate = "<!DOCTYPE html>\n" +
        "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
        "<head>\n" +
        "<title>%s</title>\n" +
        "</head>\n" +
        "<body>\n" +
        "<h1>%s</h1>" +
        "%s\n" +
        "<p><br>%s<br>\n" +
        "<a href=\"%s\">Оригинал статьи на сайте</a></p>\n" +
        "</body>\n" +
        "</html>";

    private static final Map<String, String> urlToFilename;

    static {
        ObjectMapper mapper = new ObjectMapper();
        try {
            urlToFilename = mapper.readValue(new File("src/main/resources/meta/urlMap.json"), Map.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Article(File file) {
        this.file = file;
        this.path = file.getPath();
        Document rawPage;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            this.url = br.readLine().replaceAll("<!--", "").replaceAll("-->", "");
            rawPage = Jsoup.parse(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.title = rawPage.select("h1").text();
        this.date = rawPage.select("div.elementor-element.elementor-element-1645b7a.elementor-widget.elementor-widget-heading > div > span").text();

        this.xhtml = createXHTML();
    }

    public String prepareArticle() {
        String stub = "";

        Matcher paragraphMatcher;
        try {
            paragraphMatcher = paragraphsPattern.matcher(
                FileUtils.readFileToString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (paragraphMatcher.find()) {
            stub = paragraphMatcher.group("flesh");
        }
        Matcher urlMatcher = articleUrlPattern.matcher(stub);
        String url;
        while (urlMatcher.find()) {
            url = urlMatcher.group("url");
            String linkToChapter = urlToFilename.get(url
                .replaceAll("blog", "articles")
                .replaceAll("http://", "https://"));
            try {
                stub = stub.replaceAll(url, linkToChapter);
                System.out.println(url + " -> " + linkToChapter + " in " + this.file);
            } catch (NullPointerException e) {
                String full = urlMatcher.group("full");
                Matcher titleMatcher = titlePattern.matcher(full);
                if (titleMatcher.find()) {
                    String title = titleMatcher.group("title");
                    stub = stub.replaceAll(full, title);
                    System.out.println(full + " -> " + title + " in " + this.file + " no such article present");
                } else {
                    System.out.println("Error replacing link: " + full + " in " + this.file.getPath());
                }
            }
        }
        stub = stub.replaceAll("<img[\\w\\W]*?>", "");
        return stub;
    }

    private String createXHTML() {
        String stub = prepareArticle();
        return String.format(xhtmlTemplate, title, title, stub, date, url);
    }

    public void saveXHTML() {
        File f = new File(XHTML_PATH + this.getResourceName());
        try {
            FileUtils.writeStringToFile(f, xhtml, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(this.title + " saved");
    }

    public String getPath() {
        return path;
    }

    public String getUrl() {
        return url;
    }

    public String getResourceName() {
        String[] parts = path.split("\\\\");
        return parts[parts.length - 1];
    }

    public String getTitle() {
        return title;
    }
}
