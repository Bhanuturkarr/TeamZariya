package com.aitutor.backend.document.service;

import com.aitutor.backend.document.exception.MaterialProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfContentExtractor {

    public PdfExtraction extract(Path pdfPath) {
        if (!Files.isRegularFile(pdfPath)) {
            throw new MaterialProcessingException("The original PDF is missing from local storage.");
        }

        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            List<PageText> pages = new ArrayList<>();
            int extractedCharCount = 0;

            for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {
                PDFTextStripper textStripper = new PDFTextStripper();
                textStripper.setSortByPosition(true);
                textStripper.setStartPage(pageNumber);
                textStripper.setEndPage(pageNumber);
                String pageText = textStripper.getText(document);
                pages.add(new PageText(pageNumber, pageText));
                extractedCharCount += pageText.replaceAll("\\s", "").length();
            }

            if (extractedCharCount == 0) {
                throw new MaterialProcessingException(
                        "The PDF contains no extractable text. Scanned PDFs require OCR, which is not enabled."
                );
            }

            return new PdfExtraction(document.getNumberOfPages(), extractedCharCount, List.copyOf(pages));
        } catch (MaterialProcessingException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new MaterialProcessingException(
                    "The PDF could not be read. It may be corrupt or password-protected.",
                    exception
            );
        }
    }
}
