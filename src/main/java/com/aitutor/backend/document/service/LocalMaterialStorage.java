package com.aitutor.backend.document.service;

import com.aitutor.backend.document.exception.MaterialProcessingException;
import com.aitutor.backend.document.exception.MaterialStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class LocalMaterialStorage implements MaterialStorage {

    private final Path storageRoot;

    public LocalMaterialStorage(@Value("${app.material-storage.directory}") String storageDirectory) {
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    @Override
    public Path store(UUID materialId, MultipartFile file) {
        Path destination = storageRoot.resolve(materialId + ".pdf").normalize();
        ensureWithinStorageRoot(destination);
        try {
            Files.createDirectories(storageRoot);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return destination;
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(destination);
            } catch (IOException ignored) {
                // The original storage error is more useful to the caller.
            }
            throw new MaterialStorageException("The uploaded PDF could not be saved.", exception);
        }
    }

    @Override
    public Path resolve(String storedPath) {
        try {
            Path file = Path.of(storedPath).toAbsolutePath().normalize();
            ensureWithinStorageRoot(file);
            return file;
        } catch (RuntimeException exception) {
            if (exception instanceof MaterialProcessingException materialProcessingException) {
                throw materialProcessingException;
            }
            throw new MaterialProcessingException("The stored PDF path is invalid.", exception);
        }
    }

    @Override
    public void delete(UUID materialId) {
        Path file = storageRoot.resolve(materialId + ".pdf").normalize();
        ensureWithinStorageRoot(file);
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw new MaterialStorageException("The stored PDF could not be removed.", exception);
        }
    }

    private void ensureWithinStorageRoot(Path path) {
        if (!path.startsWith(storageRoot)) {
            throw new MaterialProcessingException("The stored PDF path is outside the configured storage directory.");
        }
    }
}
