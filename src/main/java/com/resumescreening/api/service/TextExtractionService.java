package com.resumescreening.api.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@Slf4j
public class TextExtractionService {

    // Extract text from PDF
    public String extractTextFromPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            log.info("Extracted {} characters from PDF", text.length());
            return cleanText(text);

        } catch (IOException e) {
            log.error("Error extracting text from PDF: {}", e.getMessage());
            throw new RuntimeException("Failed to extract text from PDF", e);
        }
    }

    // Extract text from DOCX
    public String extractTextFromDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder text = new StringBuilder();

            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                text.append(paragraph.getText()).append("\n");
            }

            log.info("Extracted {} characters from DOCX", text.length());
            return cleanText(text.toString());

        } catch (IOException e) {
            log.error("Error extracting text from DOCX: {}", e.getMessage());
            throw new RuntimeException("Failed to extract text from DOCX", e);
        }
    }

    // Extract text based on file type
    public String extractText(MultipartFile file) throws IOException {
        String contentType = file.getContentType();

        if ("application/pdf".equals(contentType)) {
            return extractTextFromPdf(file.getInputStream());
        } else if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) {
            return extractTextFromDocx(file.getInputStream());
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }
    }

    // Clean extracted text
    private String cleanText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replaceAll("\\s+", " ")  // Multiple spaces to single space
                .replaceAll("\\r\\n|\\r|\\n", " ")  // Remove line breaks
                .trim();
    }
}