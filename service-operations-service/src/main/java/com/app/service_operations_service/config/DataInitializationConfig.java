package com.app.service_operations_service.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.app.service_operations_service.model.ServiceCategory;
import com.app.service_operations_service.model.ServiceItem;
import com.app.service_operations_service.repository.ServiceCategoryRepository;
import com.app.service_operations_service.repository.ServiceItemRepository;

@Configuration
public class DataInitializationConfig {

    private static final Logger logger =
            LoggerFactory.getLogger(DataInitializationConfig.class);

    private static final String INIT_START =
            "==================== DATA INITIALIZATION STARTED ====================";
    private static final String INIT_END =
            "==================== DATA INITIALIZATION COMPLETED ====================";
    private static final String INIT_FAILED =
            "==================== DATA INITIALIZATION FAILED ====================";

    private static final String INSTALLATION = "Installation";
    private static final String MAINTENANCE = "Maintenance";
    private static final String REPAIR = "Repair";

    private static final String HVAC_INSTALL = "hvac-system-installation";
    private static final String HVAC_MAINTENANCE = "hvac-seasonal-maintenance";
    private static final String AC_REPAIR = "air-conditioner-repair";

    private static final String SMART_THERMOSTAT = "smart-thermostat-installation";
    private static final String AC_TUNEUP = "ac-tuneup-cleaning";
    private static final String DUCT_CLEANING = "duct-inspection-cleaning";
    private static final String FILTER_REPLACEMENT = "filter-replacement";
    private static final String INDOOR_AIR_QUALITY = "indoor-air-quality-audit";

    private static final String FURNACE_INSTALL = "furnace-installation";
    private static final String DUCTLESS_INSTALL = "ductless-ac-installation";

    private static final String FURNACE_REPAIR = "furnace-repair";
    private static final String EMERGENCY_REPAIR = "emergency-hvac-repair";
    private static final String LEAK_REPAIR = "refrigerant-leak-repair";

    // Unsplash image URLs (free for commercial use, no attribution required) [web:22][web:39][web:40][web:28][web:37][web:36][web:21]
    private static final String DEFAULT_SERVICE_IMAGE_URL =
            "https://unsplash.com/photos/caucasian-plumbing-worker-installing-toilet-bowl-and-sanitary-system-inside-the-house-4PPY1v2xChU?w=800"; // HVAC units on roof [web:22]

    private static final String AC_REPAIR_IMAGE_URL =
            "https://unsplash.com/photos/air-conditioning-checking-and-filter-cleaning-maintenance-service-VuRql-uCgTM?w=800"; // Technician working on AC [web:28]

    private static final String HVAC_INSTALL_IMAGE_URL =
            "https://unsplash.com/photos/a-close-up-of-a-bunch-of-wires-and-wires-hP0yzStvp-M?w=800"; // Industrial HVAC piping [web:22]

    private static final String HVAC_MAINTENANCE_IMAGE_URL =
            "https://unsplash.com/photos/multiple-air-conditioning-units-mounted-on-a-wall-HVXmK1wqntk?w=800"; // Technician with gauges [web:21]

    private static final String DUCT_CLEANING_IMAGE_URL =
            "https://unsplash.com/photos/two-young-workers-of-contemporary-cleaning-service-company-in-coveralls-and-gloves-carrying-out-their-work-in-openspace-office-LEftsPMP6cs?w=800"; // Duct / ventilation [web:24][web:25]

    private static final String SMART_THERMOSTAT_IMAGE_URL =
            "https://images.unsplash.com/photo-1511452885600-a3d2c9148a31?w=800"; // Smart thermostat on wall [web:39][web:36]

    private static final String AC_TUNEUP_IMAGE_URL =
            "https://images.unsplash.com/photo-1581092918056-0c4c3acd3789?w=800"; // AC tune-up and cleaning [web:22]

    private static final String FILTER_REPLACEMENT_IMAGE_URL =
            "https://images.unsplash.com/photo-1582719478250-cc76ab7c9a3e?w=800"; // AC front panel open [web:37][web:28]

    private static final String INDOOR_AIR_QUALITY_IMAGE_URL =
            "https://images.unsplash.com/photo-1497366216548-37526070297c?w=800"; // Clean modern interior [web:40]

    private static final String FURNACE_INSTALL_IMAGE_URL =
            "https://images.unsplash.com/photo-1604014237800-1c9102c219da?w=800"; // Heating equipment [web:22]

    private static final String DUCTLESS_INSTALL_IMAGE_URL =
            "https://images.unsplash.com/photo-1599751449128-16a7faa75d14?w=800"; // Wall-mounted split AC [web:34][web:40]

    private static final String FURNACE_REPAIR_IMAGE_URL =
            "https://unsplash.com/photos/a-worker-is-using-a-grinder-in-a-workshop-BDaEI0L5hXI?w=800"; // Technician in boiler room [web:22]

    private static final String EMERGENCY_REPAIR_IMAGE_URL =
            "https://images.unsplash.com/photo-1581091012184-5c8afae1c4d7?w=800"; // Night technician / tools [web:21]

    private static final String LEAK_REPAIR_IMAGE_URL =
            "https://unsplash.com/photos/a-faucet-running-water-HutGSBkIrzs?w=800"; // Close-up tools / piping [web:21]

    @Bean
    public CommandLineRunner initialize(
            ServiceCategoryRepository categoryRepository,
            ServiceItemRepository itemRepository
    ) {
        return args -> {
            try {
                logger.info(INIT_START);

                if (categoryRepository.count() == 0) {
                    categoryRepository.saveAll(List.of(
                            createCategory(INSTALLATION,
                                    "New equipment and system installation services"),
                            createCategory(MAINTENANCE,
                                    "Regular inspection and preventive maintenance services"),
                            createCategory(REPAIR,
                                    "Repair and troubleshooting services")
                    ));
                }

                if (itemRepository.count() > 0) {
                    logger.info(INIT_END);
                    return;
                }

                Map<String, String> categoryIds =
                        categoryRepository.findAll()
                                .stream()
                                .collect(Collectors.toMap(
                                        ServiceCategory::getName,
                                        ServiceCategory::getId
                                ));

                List<ServiceItem> items = new ArrayList<>();

                // Existing items
                items.add(createServiceItem(
                        categoryIds.get(INSTALLATION),
                        "HVAC System Installation",
                        "Complete HVAC system installation for residential or light commercial spaces",
                        new BigDecimal("2500.00"),
                        480,
                        48,
                        HVAC_INSTALL
                ));

                items.add(createServiceItem(
                        categoryIds.get(MAINTENANCE),
                        "HVAC Seasonal Maintenance",
                        "Seasonal HVAC inspection, cleaning, and performance optimization",
                        new BigDecimal("150.00"),
                        90,
                        12,
                        HVAC_MAINTENANCE
                ));

                items.add(createServiceItem(
                        categoryIds.get(REPAIR),
                        "Air Conditioner Repair",
                        "AC diagnosis and repair service for common cooling issues",
                        new BigDecimal("250.00"),
                        120,
                        24,
                        AC_REPAIR
                ));

                // Additional INSTALLATION services
                items.add(createServiceItem(
                        categoryIds.get(INSTALLATION),
                        "Smart Thermostat Installation",
                        "Professional installation and configuration of a smart Wi‑Fi thermostat",
                        new BigDecimal("180.00"),
                        60,
                        12,
                        SMART_THERMOSTAT
                ));

                items.add(createServiceItem(
                        categoryIds.get(INSTALLATION),
                        "Ductless AC Installation",
                        "Installation of wall-mounted ductless mini-split air conditioner units",
                        new BigDecimal("2200.00"),
                        420,
                        48,
                        DUCTLESS_INSTALL
                ));

                items.add(createServiceItem(
                        categoryIds.get(INSTALLATION),
                        "Furnace Installation",
                        "High-efficiency gas or electric furnace installation",
                        new BigDecimal("3000.00"),
                        540,
                        72,
                        FURNACE_INSTALL
                ));

                // Additional MAINTENANCE services
                items.add(createServiceItem(
                        categoryIds.get(MAINTENANCE),
                        "AC Tune-Up & Cleaning",
                        "Comprehensive AC tune-up with coil, drain line, and component cleaning",
                        new BigDecimal("120.00"),
                        75,
                        12,
                        AC_TUNEUP
                ));

                items.add(createServiceItem(
                        categoryIds.get(MAINTENANCE),
                        "Duct Inspection & Cleaning",
                        "Inspection and cleaning of HVAC air ducts to improve air quality",
                        new BigDecimal("300.00"),
                        180,
                        48,
                        DUCT_CLEANING
                ));

                items.add(createServiceItem(
                        categoryIds.get(MAINTENANCE),
                        "Filter Replacement Service",
                        "Scheduled air filter inspection and replacement service",
                        new BigDecimal("60.00"),
                        30,
                        24,
                        FILTER_REPLACEMENT
                ));

                items.add(createServiceItem(
                        categoryIds.get(MAINTENANCE),
                        "Indoor Air Quality Audit",
                        "Assessment of indoor air quality with recommendations for improvement",
                        new BigDecimal("200.00"),
                        120,
                        48,
                        INDOOR_AIR_QUALITY
                ));

                // Additional REPAIR services
                items.add(createServiceItem(
                        categoryIds.get(REPAIR),
                        "Furnace Repair Service",
                        "Diagnosis and repair of common furnace heating and ignition issues",
                        new BigDecimal("280.00"),
                        150,
                        24,
                        FURNACE_REPAIR
                ));

                items.add(createServiceItem(
                        categoryIds.get(REPAIR),
                        "Emergency HVAC Repair",
                        "Priority emergency HVAC repair service during off-hours",
                        new BigDecimal("400.00"),
                        120,
                        6,
                        EMERGENCY_REPAIR
                ));

                items.add(createServiceItem(
                        categoryIds.get(REPAIR),
                        "Refrigerant Leak Detection & Repair",
                        "Detection and repair of refrigerant leaks with recharging if required",
                        new BigDecimal("320.00"),
                        180,
                        24,
                        LEAK_REPAIR
                ));

                itemRepository.saveAll(items);

                logger.info(INIT_END);

            } catch (Exception e) {
                logger.error(INIT_FAILED, e);
                throw new IllegalStateException(
                        "Service data initialization failed", e);
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
            BigDecimal price,
            long minutes,
            int slaHours,
            String imageKey
    ) {
        return ServiceItem.builder()
                .categoryId(categoryId)
                .name(name)
                .description(description)
                .basePrice(price)
                .estimatedDuration(Duration.ofMinutes(minutes))
                .slaHours(slaHours)
                .images(images(imageKey, name))
                .isActive(true)
                .build();
    }

    private List<ServiceItem.ServiceItemImage> images(String key, String name) {
        return switch (key) {
            case HVAC_INSTALL -> List.of(
                    image(HVAC_INSTALL_IMAGE_URL, "HVAC system installation")
            );
            case HVAC_MAINTENANCE -> List.of(
                    image(HVAC_MAINTENANCE_IMAGE_URL, "HVAC seasonal maintenance")
            );
            case AC_REPAIR -> List.of(
                    image(AC_REPAIR_IMAGE_URL, "Air conditioner repair service")
            );
            case SMART_THERMOSTAT -> List.of(
                    image(SMART_THERMOSTAT_IMAGE_URL, "Smart thermostat on wall")
            );
            case AC_TUNEUP -> List.of(
                    image(AC_TUNEUP_IMAGE_URL, "AC tune-up and cleaning")
            );
            case DUCT_CLEANING -> List.of(
                    image(DUCT_CLEANING_IMAGE_URL, "HVAC duct cleaning")
            );
            case FILTER_REPLACEMENT -> List.of(
                    image(FILTER_REPLACEMENT_IMAGE_URL, "AC filter replacement")
            );
            case INDOOR_AIR_QUALITY -> List.of(
                    image(INDOOR_AIR_QUALITY_IMAGE_URL, "Indoor air quality assessment")
            );
            case FURNACE_INSTALL -> List.of(
                    image(FURNACE_INSTALL_IMAGE_URL, "Furnace installation")
            );
            case DUCTLESS_INSTALL -> List.of(
                    image(DUCTLESS_INSTALL_IMAGE_URL, "Ductless AC installation")
            );
            case FURNACE_REPAIR -> List.of(
                    image(FURNACE_REPAIR_IMAGE_URL, "Furnace repair service")
            );
            case EMERGENCY_REPAIR -> List.of(
                    image(EMERGENCY_REPAIR_IMAGE_URL, "Emergency HVAC repair")
            );
            case LEAK_REPAIR -> List.of(
                    image(LEAK_REPAIR_IMAGE_URL, "Refrigerant leak repair")
            );
            default -> List.of(
                    image(DEFAULT_SERVICE_IMAGE_URL, name + " service")
            );
        };
    }

    private ServiceItem.ServiceItemImage image(String url, String alt) {
        return ServiceItem.ServiceItemImage.builder()
                .url(url)
                .alt(alt)
                .build();
    }
}
