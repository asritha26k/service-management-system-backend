package com.app.service_operations_service.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.app.service_operations_service.model.ServiceCategory;
import com.app.service_operations_service.model.ServiceItem;
import com.app.service_operations_service.repository.ServiceCategoryRepository;
import com.app.service_operations_service.repository.ServiceItemRepository;

// Data Initialization Configuration
// Automatically creates predefined service categories on application startup
@Configuration
public class DataInitializationConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializationConfig.class);

    @Bean
    public CommandLineRunner initializeServiceCategories(
            ServiceCategoryRepository categoryRepository,
            ServiceItemRepository itemRepository) {
        return args -> {
            try {

                logger.info("==================== DATA INITIALIZATION STARTED ====================");
                
                // Check if categories already exist
                long existingCategoryCount = categoryRepository.count();
                logger.info("Found {} existing service categories in database", existingCategoryCount);
                
                if (existingCategoryCount > 0) {
                    logger.info("Service categories already exist. Skipping category initialization.");
                } else {
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
                        logger.info("  - {} (ID: {}) - {}", category.getName(), category.getId(), category.getDescription())
                    );
                }

                // Check if service items already exist
                long existingItemCount = itemRepository.count();
                logger.info("Found {} existing service items in database", existingItemCount);
                
                if (existingItemCount > 0) {
                    logger.info("Service items already exist. Skipping service item initialization.");
                    logger.info("==================== DATA INITIALIZATION COMPLETED ====================");
                    return;
                }

                logger.info("Initializing default service items...");

            // Fetch categories to link service items
            List<ServiceCategory> categories = categoryRepository.findAll();
            ServiceCategory installationCategory = categories.stream()
                    .filter(c -> "Installation".equals(c.getName()))
                    .findFirst()
                    .orElse(null);
            ServiceCategory maintenanceCategory = categories.stream()
                    .filter(c -> "Maintenance".equals(c.getName()))
                    .findFirst()
                    .orElse(null);
            ServiceCategory repairCategory = categories.stream()
                    .filter(c -> "Repair".equals(c.getName()))
                    .findFirst()
                    .orElse(null);

            List<ServiceItem> defaultServiceItems = new ArrayList<>();

            // Installation Services
            if (installationCategory != null) {
                defaultServiceItems.add(createServiceItem(
                        installationCategory.getId(),
                        "HVAC System Installation",
                        "Complete installation of heating, ventilation, and air conditioning systems including ductwork, thermostats, and initial calibration",
                        new BigDecimal("2500.00"),
                        480, // 8 hours
                        48,
                        "hvac-system-installation"
                ));
                
                defaultServiceItems.add(createServiceItem(
                        installationCategory.getId(),
                        "Water Heater Installation",
                        "Professional installation of residential or commercial water heaters including all connections and safety checks",
                        new BigDecimal("800.00"),
                        180, // 3 hours
                        24,
                        "water-heater-installation"
                ));
                
                defaultServiceItems.add(createServiceItem(
                        installationCategory.getId(),
                        "Electrical Panel Installation",
                        "Installation of new electrical service panel with circuit breakers and proper grounding",
                        new BigDecimal("1500.00"),
                        300, // 5 hours
                        36,
                        "electrical-panel-installation"
                ));
                
                defaultServiceItems.add(createServiceItem(
                        installationCategory.getId(),
                        "Smart Home System Setup",
                        "Installation and configuration of smart home automation systems including lighting, security, and climate control",
                        new BigDecimal("1200.00"),
                        240, // 4 hours
                        24,
                        "smart-home-system-setup"
                ));
            }

            // Maintenance Services
            if (maintenanceCategory != null) {
                defaultServiceItems.add(createServiceItem(
                        maintenanceCategory.getId(),
                        "HVAC Seasonal Maintenance",
                        "Comprehensive HVAC system inspection including filter replacement, coil cleaning, refrigerant level check, and performance testing",
                        new BigDecimal("150.00"),
                        90, // 1.5 hours
                        12,
                        "hvac-seasonal-maintenance"
                ));
                
                defaultServiceItems.add(createServiceItem(
                        maintenanceCategory.getId(),
                        "Plumbing System Inspection",
                        "Thorough inspection of all plumbing fixtures, pipes, drains, and water pressure systems",
                        new BigDecimal("120.00"),
                        60, // 1 hour
                        8,
                        "plumbing-system-inspection"
                ));
                
                defaultServiceItems.add(createServiceItem(
                        maintenanceCategory.getId(),
                        "Electrical Safety Inspection",
                        "Complete electrical system safety inspection including outlets, switches, circuit breakers, and grounding",
                        new BigDecimal("180.00"),
                        120, // 2 hours
                        12,
                        "electrical-safety-inspection"
                ));
                
                defaultServiceItems.add(createServiceItem(
                        maintenanceCategory.getId(),
                        "Appliance Tune-Up Service",
                        "Preventive maintenance for major appliances including cleaning, calibration, and minor adjustments",
                        new BigDecimal("100.00"),
                        60, // 1 hour
                        8,
                        "appliance-tune-up-service"
                ));
            }

            // Repair Services
            if (repairCategory != null) {
                defaultServiceItems.add(createServiceItem(
                        repairCategory.getId(),
                        "Air Conditioner Repair",
                        "Diagnosis and repair of air conditioning units including compressor, refrigerant leaks, and electrical issues",
                        new BigDecimal("250.00"),
                        120, // 2 hours
                        24,
                        "air-conditioner-repair"
                ));
                
                defaultServiceItems.add(createServiceItem(
                        repairCategory.getId(),
                        "Plumbing Leak Repair",
                        "Emergency or scheduled repair of leaking pipes, faucets, toilets, and other plumbing fixtures",
                        new BigDecimal("180.00"),
                        90, // 1.5 hours
                        12,
                        "plumbing-leak-repair"
                ));
                
                defaultServiceItems.add(createServiceItem(
                        repairCategory.getId(),
                        "Electrical Outlet/Switch Repair",
                        "Repair or replacement of malfunctioning electrical outlets, switches, and fixtures",
                        new BigDecimal("120.00"),
                        60, // 1 hour
                        8,
                        "electrical-outlet-switch-repair"
                ));
                
                defaultServiceItems.add(createServiceItem(
                        repairCategory.getId(),
                        "Water Heater Repair",
                        "Troubleshooting and repair of water heater issues including heating elements, thermostats, and pilot lights",
                        new BigDecimal("200.00"),
                        120, // 2 hours
                        12,
                        "water-heater-repair"
                ));
                
                defaultServiceItems.add(createServiceItem(
                        repairCategory.getId(),
                        "Drain Cleaning & Unclogging",
                        "Professional drain cleaning service using advanced equipment to clear stubborn clogs and blockages",
                        new BigDecimal("150.00"),
                        90, // 1.5 hours
                        8,
                        "drain-cleaning-unclogging"
                ));
            }

            // Save all service items
            List<ServiceItem> savedItems = itemRepository.saveAll(defaultServiceItems);
            
            logger.info("Successfully initialized {} service items", savedItems.size());
            savedItems.forEach(item -> 
                logger.info("  - {} (ID: {}) - ${} - {} mins", 
                    item.getName(), 
                    item.getId(),
                    item.getBasePrice(), 
                    item.getEstimatedDuration().toMinutes())
            );
            
            logger.info("==================== DATA INITIALIZATION COMPLETED ====================");
            
            } catch (Exception e) {
                logger.error("==================== DATA INITIALIZATION FAILED ====================");
                logger.error("Error during data initialization: {}", e.getMessage(), e);
                throw e; // Re-throw to prevent application startup on critical errors
            }
        };
    }

    private ServiceCategory createCategory(String name, String description) {
        return ServiceCategory.builder()
                .name(name)
                .description(description)
                .isActive(true)
                .build();
    }

    private ServiceItem createServiceItem(
            String categoryId,
            String name,
            String description,
            BigDecimal basePrice,
            long estimatedDurationMinutes,
            int slaHours,
            String imageKey) {
        return ServiceItem.builder()
                .categoryId(categoryId)
                .name(name)
                .description(description)
                .basePrice(basePrice)
                .estimatedDuration(Duration.ofMinutes(estimatedDurationMinutes))
                .slaHours(slaHours)
                .images(createSampleImages(imageKey, name))
                .isActive(true)
                .build();
    }

    private List<ServiceItem.ServiceItemImage> createSampleImages(String key, String name) {
        // Map service-specific images with appropriate stock photo URLs
        return switch (key) {
            case "hvac-system-installation" -> List.of(
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=800")
                            .alt("Modern HVAC system installation with technician")
                            .build(),
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=800")
                            .alt("HVAC ductwork and air conditioning unit")
                            .build()
            );
            case "water-heater-installation" -> List.of(
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1607400201889-565b1ee75f8e?w=800")
                            .alt("Professional water heater installation")
                            .build(),
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=800")
                            .alt("Modern tankless water heater")
                            .build()
            );
            case "electrical-panel-installation" -> List.of(
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1621905252507-b35492cc74b4?w=800")
                            .alt("Electrical panel with circuit breakers")
                            .build(),
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1473341304170-971dccb5ac1e?w=800")
                            .alt("Technician working on electrical panel")
                            .build()
            );
            case "smart-home-system-setup" -> List.of(
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1558002038-1055907df827?w=800")
                            .alt("Smart home automation control panel")
                            .build(),
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1585079542156-2755d9c8a094?w=800")
                            .alt("Smart home devices and lighting control")
                            .build()
            );
            case "hvac-seasonal-maintenance" -> List.of(
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://unsplash.com/photos/close-up-of-a-repairmans-hand-fixing-window-xqFquocCtvE?w=800")
                            .alt("HVAC technician performing maintenance")
                            .build(),
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=800")
                            .alt("Air conditioning filter replacement")
                            .build()
            );
            case "plumbing-system-inspection" -> List.of(
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1607472586893-edb57bdc0e39?w=800")
                            .alt("Plumber inspecting pipes and fixtures")
                            .build(),
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1581244277943-fe4a9c777189?w=800")
                            .alt("Professional plumbing tools and inspection")
                            .build()
            );
            case "electrical-safety-inspection" -> List.of(
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://unsplash.com/photos/apartment-fusebox-installation-by-caucasian-electrician-in-his-40s-residential-home-electric-system-theme-SCAZpCdVZk4?w=800")
                            .alt("Electrician testing electrical outlets")
                            .build(),
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1621905252507-b35492cc74b4?w=800")
                            .alt("Electrical safety inspection equipment")
                            .build()
            );
            case "appliance-tune-up-service" -> List.of(
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1556911220-bff31c812dba?w=800")
                            .alt("Technician servicing kitchen appliances")
                            .build(),
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1556912172-45b7abe8b7e1?w=800")
                            .alt("Modern kitchen appliances maintenance")
                            .build()
            );
            case "air-conditioner-repair" -> List.of(
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://unsplash.com/photos/rear-view-of-a-man-cleaning-air-conditioning-system-HsNtqUNWOqk?w=800")
                            .alt("Technician repairing air conditioning unit")
                            .build(),
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1604754742629-3e694021c0ac?w=800")
                            .alt("Air conditioner compressor repair")
                            .build()
            );
            case "plumbing-leak-repair" -> List.of(
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1581244277943-fe4a9c777189?w=800")
                            .alt("Plumber fixing leaking pipe")
                            .build(),
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1607472586893-edb57bdc0e39?w=800")
                            .alt("Leak repair tools and equipment")
                            .build()
            );
            case "electrical-outlet-switch-repair" -> List.of(
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://unsplash.com/photos/a-person-is-holding-a-piece-of-electrical-equipment-NC5XIqIZEbk?w=800")
                            .alt("Electrician replacing outlet")
                            .build(),
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1581092160562-40aa08e78837?w=800")
                            .alt("Electrical switch and outlet repair")
                            .build()
            );
            case "water-heater-repair" -> List.of(
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=800")
                            .alt("Water heater repair and maintenance")
                            .build(),
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1607400201889-565b1ee75f8e?w=800")
                            .alt("Technician fixing water heater thermostat")
                            .build()
            );
            case "drain-cleaning-unclogging" -> List.of(
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1607472586893-edb57bdc0e39?w=800")
                            .alt("Professional drain cleaning service")
                            .build(),
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1581244277943-fe4a9c777189?w=800")
                            .alt("Drain unclogging equipment and tools")
                            .build()
            );
            default -> List.of(
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=800")
                            .alt(name + " - professional service")
                            .build(),
                    ServiceItem.ServiceItemImage.builder()
                            .url("https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=800")
                            .alt(name + " - quality workmanship")
                            .build()
            );
        };
    }
}
