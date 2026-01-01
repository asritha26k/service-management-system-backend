package com.app.service_operations_service.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.app.service_operations_service.model.ServiceCategory;
import com.app.service_operations_service.repository.ServiceCategoryRepository;

// Data Initialization Configuration
// Automatically creates predefined service categories on application startup
@Configuration
public class DataInitializationConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializationConfig.class);

    @Bean
    public CommandLineRunner initializeServiceCategories(ServiceCategoryRepository categoryRepository) {
        return args -> {
            // Check if categories already exist
            long existingCount = categoryRepository.count();
            
            if (existingCount > 0) {
                logger.info("Service categories already exist. Skipping initialization.");
                return;
            }

            logger.info("Initializing default service categories...");

            // Create predefined service categories
            List<ServiceCategory> defaultCategories = List.of(
                    createCategory(
                            "Installation",
                            "New equipment and system installation services including setup, configuration, and testing"
                    ),
                    createCategory(
                            "Maintenance",
                            "Regular maintenance services including inspection, cleaning, and preventive care"
                    ),
                    createCategory(
                            "Repair",
                            "Repair and troubleshooting services for existing equipment and systems"
                    )
            );

            // Save all categories
            List<ServiceCategory> saved = categoryRepository.saveAll(defaultCategories);
            
            logger.info("Successfully initialized {} service categories", saved.size());
            saved.forEach(category -> 
                logger.info("  - {} ({})", category.getName(), category.getDescription())
            );
        };
    }

    private ServiceCategory createCategory(String name, String description) {
        ServiceCategory category = new ServiceCategory();
        category.setName(name);
        category.setDescription(description);
        category.setActive(true);
        return category;
    }
}
