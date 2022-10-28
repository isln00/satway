import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import nl.siegmann.epublib.epub.EpubWriter;

public class App {
    public static final String RAW_PAGES = "src/main/resources/raw/";
    public static final String ARTICLE_PAGES = "src/main/resources/xhtml/";
    private static final FileNameComparator FILE_NAME_COMPARATOR = new FileNameComparator();
    private static final boolean RELOAD_INDEX = false;
    private static final boolean RELOAD_ARTICLES = false;
    private static final boolean REMAKE_XHTMLS = true;

    public static void main(String[] args) throws IOException, InterruptedException {
        List<String> hrefs;
        Magazine satway = new Magazine();

        if (RELOAD_INDEX) {
            hrefs = Loader.getHrefsOnline();
        } else {
            hrefs = Loader.getHrefsFromLocalIndex();
        }

        if (RELOAD_ARTICLES) {
            Loader.savePages(hrefs);
        }

        List<Article> articles;
        if (REMAKE_XHTMLS) {
            articles = createAndSaveArticles();
        } else {
            articles = createArticles();
        }

        for (Article a : articles) {
            satway.add(a);
        }

        EpubWriter eWriter = new EpubWriter();
        eWriter.write(satway.compile(), new FileOutputStream("src/main/resources/out/satway.epub"));
    }

    private static boolean emptyDir(String path) {
        File xDir = new File(path);
        return xDir.isDirectory() && xDir.list().length == 0;
    }

    private static List<Article> createAndSaveArticles() throws IOException {
        try (Stream<Path> walk = Files.walk(Path.of(RAW_PAGES))) {
            return walk
                .filter(str -> str.toString().endsWith(".html"))
                .sorted(FILE_NAME_COMPARATOR)
                .map(Path::toFile)
                .map(Article::new)
                .peek(Article::saveXHTML)
                .collect(Collectors.toList());
        }
    }

    private static List<Article> createArticles() throws IOException {
        try (Stream<Path> walk = Files.walk(Path.of(ARTICLE_PAGES))) {
            return walk
                .filter(str -> str.toString().endsWith(".html"))
                .sorted(FILE_NAME_COMPARATOR)
                .map(Path::toFile)
                .map(Article::new)
                .collect(Collectors.toList());
        }
    }
}

class FileNameComparator implements Comparator<Path> {
    @Override
    public int compare(Path p1, Path p2) {
        {
            String str1 = p1.toString();
            String str2 = p2.toString();

            String numStr1 = str1
                .replaceAll("\\\\", "")
                .replaceAll("[a-zA-Z]", "")
                .split("-")[0];

            String numStr2 = str2
                .replaceAll("\\\\", "")
                .replaceAll("[a-zA-Z]", "")
                .split("-")[0];

            int n1 = Integer.parseInt(numStr1);
            int n2 = Integer.parseInt(numStr2);

            return n1 - n2;
        }
    }
}