package com.app.service_operations_service.config;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
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
@ConditionalOnClass(name = "com.mongodb.client.MongoClient")
@ConditionalOnProperty(name = "app.mongo.enabled", havingValue = "true", matchIfMissing = true)
@Configuration
public class MongoConfig {

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
