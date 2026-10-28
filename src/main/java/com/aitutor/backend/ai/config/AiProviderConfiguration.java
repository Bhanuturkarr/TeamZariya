package com.aitutor.backend.ai.config;

import com.aitutor.backend.ai.service.OpenAiTutorAiProvider;
import com.aitutor.backend.ai.service.TutorAiProvider;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class AiProviderConfiguration {

    @Bean
    TutorAiProvider openAiTutorAiProvider(ChatModel chatModel) {
        return new OpenAiTutorAiProvider(chatModel);
    }

    @Bean
    VectorStore pgVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1536)
                .initializeSchema(true)
                .build();
    }
}
