package com.aitutor.backend.document.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.UUID;

public interface MaterialStorage {

    Path store(UUID materialId, MultipartFile file);

    Path resolve(String storedPath);

    void delete(UUID materialId);
}
