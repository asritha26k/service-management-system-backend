package com.app.service_operations_service.config;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;

// MongoDB configuration to handle Java 8 date/time types properly.
// Fixes the module access issue with Instant serialization.
// Also ensures MongoDB database is initialized on startup.
@ConditionalOnClass(name = "com.mongodb.client.MongoClient")
@ConditionalOnProperty(name = "app.mongo.enabled", havingValue = "true", matchIfMissing = true)
@Configuration
public class MongoConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    /**
     * Initializes MongoDB database on application startup.
     * MongoDB automatically creates databases when first accessed, but this ensures
     * the connection is established and the database is ready.
     */
    @Bean
    @Order(1)
    public ApplicationRunner initializeMongoDatabase(MongoTemplate mongoTemplate) {
        return args -> {
            try {
                // Get database name
                String dbName = mongoTemplate.getDb().getName();
                log.info("Initializing MongoDB database: {}", dbName);
                
                // Test connection and ensure database exists
                // MongoDB will create the database automatically when we access it
                mongoTemplate.getDb().listCollectionNames().first();
                
                log.info("✓ MongoDB database '{}' is ready", dbName);
            } catch (Exception e) {
                log.warn("⚠ Could not initialize MongoDB database at startup: {}", e.getMessage());
                log.info("Database will be created automatically when first data is inserted");
            }
        };
    }

    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new InstantToDateConverter());
        converters.add(new DateToInstantConverter());
        return new MongoCustomConversions(converters);
    }
    @Bean
    public MongoMappingContext mongoMappingContext(
            MongoCustomConversions customConversions) {

        MongoMappingContext context = new MongoMappingContext();
        context.setSimpleTypeHolder(customConversions.getSimpleTypeHolder());
        return context;
    }

    @Primary
    @Bean
    public MappingMongoConverter mappingMongoConverter(
            MongoDatabaseFactory mongoDatabaseFactory,
            MongoMappingContext mongoMappingContext,
            MongoCustomConversions customConversions) {
        
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDatabaseFactory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mongoMappingContext);
        
        // Remove _class field from documents
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        
        // Apply custom conversions
        converter.setCustomConversions(customConversions);
        converter.afterPropertiesSet();
        
        return converter;
    }

    @WritingConverter
    public static class InstantToDateConverter implements Converter<Instant, Date> {
        @Override
        public Date convert(@NonNull Instant source) {
            return Date.from(source);
        }
    }

    @ReadingConverter
    public static class DateToInstantConverter implements Converter<Date, Instant> {
        @Override
        public Instant convert(@NonNull Date source) {
            return source.toInstant();
        }
    }
}
