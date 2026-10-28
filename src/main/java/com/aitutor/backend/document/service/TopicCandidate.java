package com.aitutor.backend.document.service;

import com.aitutor.backend.document.TopicExtractionMethod;

public record TopicCandidate(String name, int sourcePage, TopicExtractionMethod extractionMethod) {
}
