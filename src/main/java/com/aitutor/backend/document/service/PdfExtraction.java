package com.aitutor.backend.document.service;

import java.util.List;

public record PdfExtraction(int pageCount, int extractedCharCount, List<PageText> pages) {
}
