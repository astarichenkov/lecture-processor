package com.lectureprocessor.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.lectureprocessor.text.NoteSection;
import com.lectureprocessor.text.StructuredNotes;

class DocxExporterTest {

    private final DocxExporter exporter = new DocxExporter();

    @Test
    void exportsMarkdownNotesToDocx(@TempDir Path dir) throws IOException {
        StructuredNotes notes = new StructuredNotes(
                "# Конспект лекции\n\n## Резюме\n\nКраткое резюме.\n\n## Раздел 1\n\nОсновной текст.",
                "Краткое резюме.",
                List.of(new NoteSection(1, 0, 60_000, "## Раздел 1\n\nОсновной текст.")));
        Path output = dir.resolve("notes.docx");

        exporter.export(output, "Конспект: lecture.mp4", notes, List.of());

        assertThat(output).exists();
        assertThat(Files.size(output)).isGreaterThan(1000);
        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(output))) {
            String text = document.getParagraphs().stream()
                    .map(p -> p.getText() == null ? "" : p.getText())
                    .reduce("", (a, b) -> a + "\n" + b);
            assertThat(text).contains("Резюме").contains("Основной текст");
        }
    }

    @Test
    void insertsSlideImageIntoMatchingSection(@TempDir Path dir) throws IOException {
        Path image = dir.resolve("slide.jpg");
        writeJpeg(image);
        StructuredNotes notes = new StructuredNotes(
                "# Конспект лекции\n\n## Раздел 1\n\nТекст.",
                "",
                List.of(new NoteSection(1, 0, 60_000, "## Раздел 1\n\nТекст.")));
        Path output = dir.resolve("notes.docx");

        exporter.export(output, "Конспект", notes, List.of(new SlideImage(30_000, image, "Слайд")));

        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(output))) {
            assertThat(document.getAllPictures()).isNotEmpty();
        }
    }

    private static void writeJpeg(Path path) throws IOException {
        BufferedImage image = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 120, 80);
        g.setColor(Color.BLACK);
        g.drawString("slide", 10, 40);
        g.dispose();
        ImageIO.write(image, "jpg", path.toFile());
    }
}
