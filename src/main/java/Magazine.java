import java.io.IOException;
import java.io.InputStream;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.domain.TableOfContents;

public class Magazine {
    private final Book book = new Book();
    private final TableOfContents toc = new TableOfContents();

    public Magazine() throws IOException {
        String name = "0-pre.html";
        try (InputStream stream = Magazine.class.getResourceAsStream("meta/" + name)) {
            if (stream != null) {
                Resource text = new Resource(stream, name);
                TOCReference tocRef = book.addSection("", text);
                toc.addTOCReference(tocRef);
            }
        }
    }

    public void add(Article article) throws IOException {
        try (InputStream stream = Magazine.class.getResourceAsStream("xhtml/" + article.getResourceName())) {
            if (stream != null) {
                Resource text = new Resource(stream, article.getResourceName());
                TOCReference tocRef = book.addSection(article.getTitle(), text);
                tocRef.setTitle(article.getTitle());
                toc.addTOCReference(tocRef);
            } else {
                System.out.printf("Resource %s is empty\n", article.getPath());
            }
        }
    }

    public Book compile() throws IOException {
        book.getMetadata().addTitle("Психология без соплей\nИнтеллектуальный вытрезвитель");
        book.getMetadata().addAuthor(new Author("Олег", "Сатов"));

        try (InputStream coverAsStream = Magazine.class.getResourceAsStream("meta/cover.png")) {
            if (coverAsStream != null) {
                book.setCoverImage(new Resource(coverAsStream, "cover.png"));
            } else {
                System.out.println("Cover is empty");
            }
        }

        book.setTableOfContents(toc);
        book.generateSpineFromTableOfContents();
        return book;
    }
}
