package com.crm.config;

import com.crm.entity.Agent;
import com.crm.entity.AppUser;
import com.crm.entity.Booking;
import com.crm.entity.Client;
import com.crm.entity.Lead;
import com.crm.entity.Property;
import com.crm.entity.enums.BookingStatus;
import com.crm.entity.enums.InterestType;
import com.crm.entity.enums.LeadSource;
import com.crm.entity.enums.LeadStage;
import com.crm.entity.enums.PaymentStatus;
import com.crm.entity.enums.PropertyStatus;
import com.crm.entity.enums.PropertyType;
import com.crm.entity.enums.Role;
import com.crm.repository.AgentRepository;
import com.crm.repository.AppUserRepository;
import com.crm.repository.BookingRepository;
import com.crm.repository.ClientRepository;
import com.crm.repository.LeadRepository;
import com.crm.repository.PropertyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedSampleData(
            AppUserRepository appUserRepository,
            AgentRepository agentRepository,
            ClientRepository clientRepository,
            PropertyRepository propertyRepository,
            BookingRepository bookingRepository,
            LeadRepository leadRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            boolean hasSeedUsers = appUserRepository.count() > 0;
            boolean hasSeedProperties = propertyRepository.count() > 0;

            if (!hasSeedUsers) {
                AppUser admin = new AppUser();
                admin.setName("System Admin");
                admin.setEmail("admin@realestatecrm.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ADMIN);
                appUserRepository.save(admin);
            }

            Agent agentOne = agentRepository.findByEmail("aarav@realestatecrm.com")
                    .orElseGet(() -> createAgent("Aarav Mehta", "aarav@realestatecrm.com", "agent123", passwordEncoder));
            Agent agentTwo = agentRepository.findByEmail("diya@realestatecrm.com")
                    .orElseGet(() -> createAgent("Diya Sharma", "diya@realestatecrm.com", "agent123", passwordEncoder));
            Agent agentThree = agentRepository.findByEmail("vikram@realestatecrm.com")
                    .orElseGet(() -> createAgent("Vikram Bedi", "vikram@realestatecrm.com", "agent123", passwordEncoder));
            agentRepository.saveAll(List.of(agentOne, agentTwo, agentThree));

            Client clientOne = ensureClient(clientRepository, "riya@example.com", "Riya Kapoor", "+91-9876543210", "Baner, Pune");
            Client clientTwo = ensureClient(clientRepository, "kabir@example.com", "Kabir Verma", "+91-9988776655", "Andheri West, Mumbai");
            Client clientThree = ensureClient(clientRepository, "neha@example.com", "Neha Joshi", "+91-9123456780", "Whitefield, Bengaluru");
            Client clientFour = ensureClient(clientRepository, "ananya@example.com", "Ananya Khanna", "+91-9810012345", "Sector 57, Gurugram");
            Client clientFive = ensureClient(clientRepository, "siddharth@example.com", "Siddharth Rao", "+91-9899098989", "Vaishali Nagar, Jaipur");
            clientRepository.saveAll(List.of(clientOne, clientTwo, clientThree, clientFour, clientFive));

            Lead leadOne = ensureLead(leadRepository, "ishita.lead@example.com", "Ishita Malhotra", "+91-9811111111",
                    "Whitefield, Bengaluru", "11000000", "15000000", InterestType.BUY, LeadSource.PORTAL,
                    LocalDate.now().plusDays(1), LeadStage.NEW, "Interested in ready-to-move 3 BHK inventory", agentOne);
            Lead leadTwo = ensureLead(leadRepository, "arjun.lead@example.com", "Arjun Sethi", "+91-9822222222",
                    "Andheri West, Mumbai", "25000000", "32000000", InterestType.INVEST, LeadSource.REFERRAL,
                    LocalDate.now().plusDays(2), LeadStage.CONTACTED, "Looking for commercial investment with rental yield", agentTwo);
            Lead leadThree = ensureLead(leadRepository, "meera.lead@example.com", "Meera Nair", "+91-9833333333",
                    "Baner, Pune", "7000000", "10000000", InterestType.BUY, LeadSource.SOCIAL_MEDIA,
                    LocalDate.now().plusDays(4), LeadStage.SITE_VISIT_SCHEDULED, "Prefers gated community with amenities", agentOne);
            Lead leadFour = ensureLead(leadRepository, "karan.lead@example.com", "Karan Gill", "+91-9844444444",
                    "Sector 57, Gurugram", "8500000", "12000000", InterestType.BUY, LeadSource.DIRECT_CALL,
                    LocalDate.now().plusDays(3), LeadStage.NEGOTIATION, "Comparing plot options for self-use", agentThree);
            Lead leadFive = ensureLead(leadRepository, "sana.lead@example.com", "Sana Qureshi", "+91-9855555555",
                    "Vaishali Nagar, Jaipur", "6000000", "9000000", InterestType.BUY, LeadSource.WALK_IN,
                    LocalDate.now().plusDays(5), LeadStage.NEW, "First-time buyer, needs financing support", agentTwo);
            leadRepository.saveAll(List.of(leadOne, leadTwo, leadThree, leadFour, leadFive));

            Property propertyOne;
            Property propertyTwo;
            Property propertyThree;
            Property propertyFour;
            Property propertyFive;

            if (!hasSeedProperties) {
                propertyOne = createProperty("Lakeview Residency", "Pune", "Baner", PropertyType.FLAT, "3 BHK", 1480, "12500000", PropertyStatus.AVAILABLE, true, agentOne);
                propertyTwo = createProperty("Skyline Towers", "Mumbai", "Andheri West", PropertyType.COMMERCIAL, "Retail + Office", 3200, "28500000", PropertyStatus.SOLD, true, agentTwo);
                propertyThree = createProperty("Garden Estate", "Bengaluru", "Whitefield", PropertyType.VILLA, "4 BHK", 2650, "17900000", PropertyStatus.AVAILABLE, true, agentOne);
                propertyFour = createProperty("Sector 57 Greens", "Gurugram", "Sector 57", PropertyType.PLOT, "Residential Plot", 2400, "9200000", PropertyStatus.AVAILABLE, false, agentThree);
                propertyFive = createProperty("Pink City Square", "Jaipur", "Vaishali Nagar", PropertyType.FLAT, "2 BHK", 1180, "7800000", PropertyStatus.AVAILABLE, false, agentTwo);
                propertyRepository.saveAll(List.of(propertyOne, propertyTwo, propertyThree, propertyFour, propertyFive));

                Booking bookingOne = createBooking(propertyOne, clientOne, agentOne, LocalDate.now().plusDays(3), BookingStatus.PENDING, "1250000", "250000", PaymentStatus.PARTIAL, "TOK-PUNE-101");
                Booking bookingTwo = createBooking(propertyTwo, clientTwo, agentTwo, LocalDate.now().minusDays(2), BookingStatus.CONFIRMED, "2850000", "2850000", PaymentStatus.PAID, "TOK-MUM-202");
                Booking bookingThree = createBooking(propertyThree, clientThree, agentOne, LocalDate.now().plusDays(7), BookingStatus.CANCELLED, "1790000", "0", PaymentStatus.NOT_STARTED, "TOK-BLR-303");
                Booking bookingFour = createBooking(propertyFour, clientFour, agentThree, LocalDate.now().plusDays(5), BookingStatus.PENDING, "920000", "100000", PaymentStatus.PARTIAL, "TOK-GGN-404");
                Booking bookingFive = createBooking(propertyFive, clientFive, agentTwo, LocalDate.now().plusDays(9), BookingStatus.PENDING, "780000", "0", PaymentStatus.NOT_STARTED, "TOK-JPR-505");
                bookingRepository.saveAll(List.of(bookingOne, bookingTwo, bookingThree, bookingFour, bookingFive));
            } else {
                List<Property> existingProperties = propertyRepository.findAll();
                existingProperties.forEach(property -> {
                    if (property.getPropertyType() == null) {
                        property.setPropertyType(inferPropertyType(property.getTitle()));
                    }
                    enrichLegacyProperty(property);
                    propertyRepository.save(property);
                });

                Set<String> existingTitles = existingProperties.stream()
                        .map(Property::getTitle)
                        .collect(Collectors.toSet());
                List<Property> topUpProperties = new ArrayList<>();
                addPropertyIfMissing(topUpProperties, existingTitles, createProperty("Lakeview Residency", "Pune", "Baner", PropertyType.FLAT, "3 BHK", 1480, "12500000", PropertyStatus.AVAILABLE, true, agentOne));
                addPropertyIfMissing(topUpProperties, existingTitles, createProperty("Skyline Towers", "Mumbai", "Andheri West", PropertyType.COMMERCIAL, "Retail + Office", 3200, "28500000", PropertyStatus.SOLD, true, agentTwo));
                addPropertyIfMissing(topUpProperties, existingTitles, createProperty("Garden Estate", "Bengaluru", "Whitefield", PropertyType.VILLA, "4 BHK", 2650, "17900000", PropertyStatus.AVAILABLE, true, agentOne));
                addPropertyIfMissing(topUpProperties, existingTitles, createProperty("Sector 57 Greens", "Gurugram", "Sector 57", PropertyType.PLOT, "Residential Plot", 2400, "9200000", PropertyStatus.AVAILABLE, false, agentThree));
                addPropertyIfMissing(topUpProperties, existingTitles, createProperty("Pink City Square", "Jaipur", "Vaishali Nagar", PropertyType.FLAT, "2 BHK", 1180, "7800000", PropertyStatus.AVAILABLE, false, agentTwo));

                if (!topUpProperties.isEmpty()) {
                    propertyRepository.saveAll(topUpProperties);
                }

                List<Booking> existingBookings = bookingRepository.findAll();
                existingBookings.forEach(booking -> enrichLegacyBooking(booking, propertyRepository));
                bookingRepository.saveAll(existingBookings);
            }

            log.info("Seeded sample real estate CRM data");
        };
    }

    private Agent createAgent(String name, String email, String password, PasswordEncoder passwordEncoder) {
        AppUser user = new AppUser();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.AGENT);

        Agent agent = new Agent();
        agent.setName(name);
        agent.setEmail(email);
        agent.setUser(user);
        return agent;
    }

    private Client ensureClient(ClientRepository clientRepository, String email, String name, String phone, String preferredLocation) {
        return clientRepository.findAll().stream()
                .filter(client -> email.equalsIgnoreCase(client.getEmail()))
                .findFirst()
                .map(client -> {
                    if (client.getPreferredLocation() == null || client.getPreferredLocation().isBlank()) {
                        client.setPreferredLocation(preferredLocation);
                    }
                    return client;
                })
                .orElseGet(() -> createClient(name, email, phone, preferredLocation));
    }

    private Client createClient(String name, String email, String phone, String preferredLocation) {
        Client client = new Client();
        client.setName(name);
        client.setEmail(email);
        client.setPhone(phone);
        client.setPreferredLocation(preferredLocation);
        return client;
    }

    private Lead ensureLead(
            LeadRepository leadRepository,
            String email,
            String name,
            String phone,
            String preferredLocation,
            String budgetMin,
            String budgetMax,
            InterestType interestType,
            LeadSource source,
            LocalDate followUpDate,
            LeadStage stage,
            String notes,
            Agent agent
    ) {
        return leadRepository.findAll().stream()
                .filter(lead -> email.equalsIgnoreCase(lead.getEmail()))
                .findFirst()
                .map(lead -> {
                    if (lead.getAgent() == null) {
                        lead.setAgent(agent);
                    }
                    if (lead.getFollowUpDate() == null) {
                        lead.setFollowUpDate(followUpDate);
                    }
                    return lead;
                })
                .orElseGet(() -> createLead(name, email, phone, preferredLocation, budgetMin, budgetMax, interestType, source, followUpDate, stage, notes, agent));
    }

    private Lead createLead(
            String name,
            String email,
            String phone,
            String preferredLocation,
            String budgetMin,
            String budgetMax,
            InterestType interestType,
            LeadSource source,
            LocalDate followUpDate,
            LeadStage stage,
            String notes,
            Agent agent
    ) {
        Lead lead = new Lead();
        lead.setName(name);
        lead.setEmail(email);
        lead.setPhone(phone);
        lead.setPreferredLocation(preferredLocation);
        lead.setBudgetMin(new BigDecimal(budgetMin));
        lead.setBudgetMax(new BigDecimal(budgetMax));
        lead.setInterestType(interestType);
        lead.setSource(source);
        lead.setFollowUpDate(followUpDate);
        lead.setStage(stage);
        lead.setNotes(notes);
        lead.setAgent(agent);
        return lead;
    }

    private Property createProperty(
            String title,
            String city,
            String locality,
            PropertyType propertyType,
            String configuration,
            Integer areaSqFt,
            String price,
            PropertyStatus status,
            boolean featured,
            Agent agent
    ) {
        Property property = new Property();
        property.setTitle(title);
        property.setCity(city);
        property.setLocality(locality);
        property.setLocation(locality + ", " + city);
        property.setPropertyType(propertyType);
        property.setConfiguration(configuration);
        property.setAreaSqFt(areaSqFt);
        property.setPrice(new BigDecimal(price));
        property.setStatus(status);
        property.setFeatured(featured);
        property.setAgent(agent);
        return property;
    }

    private Booking createBooking(
            Property property,
            Client client,
            Agent agent,
            LocalDate date,
            BookingStatus status,
            String bookingAmount,
            String amountPaid,
            PaymentStatus paymentStatus,
            String paymentReference
    ) {
        Booking booking = new Booking();
        booking.setProperty(property);
        booking.setClient(client);
        booking.setAgent(agent);
        booking.setBookingDate(date);
        booking.setStatus(status);
        booking.setBookingAmount(new BigDecimal(bookingAmount));
        booking.setAmountPaid(new BigDecimal(amountPaid));
        booking.setPaymentStatus(paymentStatus);
        booking.setPaymentDate(new BigDecimal(amountPaid).compareTo(BigDecimal.ZERO) > 0 ? date.minusDays(1) : null);
        booking.setPaymentReference(paymentReference);
        return booking;
    }

    private void addPropertyIfMissing(List<Property> target, Set<String> existingTitles, Property property) {
        if (!existingTitles.contains(property.getTitle())) {
            target.add(property);
            existingTitles.add(property.getTitle());
        }
    }

    private PropertyType inferPropertyType(String title) {
        String normalizedTitle = title == null ? "" : title.toLowerCase();
        if (normalizedTitle.contains("tower") || normalizedTitle.contains("square") || normalizedTitle.contains("office")) {
            return PropertyType.COMMERCIAL;
        }
        if (normalizedTitle.contains("estate") || normalizedTitle.contains("villa")) {
            return PropertyType.VILLA;
        }
        if (normalizedTitle.contains("sector") || normalizedTitle.contains("plot")) {
            return PropertyType.PLOT;
        }
        return PropertyType.FLAT;
    }

    private void enrichLegacyProperty(Property property) {
        if (property.getCity() == null || property.getCity().isBlank() || property.getLocality() == null || property.getLocality().isBlank()) {
            String[] locationParts = splitLocation(property.getLocation());
            property.setLocality(locationParts[0]);
            property.setCity(locationParts[1]);
        }
        if (property.getLocation() == null || property.getLocation().isBlank()) {
            property.setLocation(property.getLocality() + ", " + property.getCity());
        }
        if (property.getConfiguration() == null || property.getConfiguration().isBlank()) {
            property.setConfiguration(inferConfiguration(property.getPropertyType()));
        }
        if (property.getAreaSqFt() == null || property.getAreaSqFt() < 100) {
            property.setAreaSqFt(inferArea(property.getPropertyType()));
        }
    }

    private String[] splitLocation(String location) {
        if (location == null || location.isBlank()) {
            return new String[]{"Central", "Delhi NCR"};
        }
        String[] parts = location.split(",");
        if (parts.length >= 2) {
            return new String[]{parts[0].trim(), parts[parts.length - 1].trim()};
        }
        return new String[]{location.trim(), location.trim()};
    }

    private String inferConfiguration(PropertyType propertyType) {
        if (propertyType == PropertyType.VILLA) {
            return "4 BHK";
        }
        if (propertyType == PropertyType.PLOT) {
            return "Residential Plot";
        }
        if (propertyType == PropertyType.COMMERCIAL) {
            return "Office Space";
        }
        return "3 BHK";
    }

    private int inferArea(PropertyType propertyType) {
        if (propertyType == PropertyType.VILLA) {
            return 2400;
        }
        if (propertyType == PropertyType.PLOT) {
            return 2200;
        }
        if (propertyType == PropertyType.COMMERCIAL) {
            return 1800;
        }
        return 1350;
    }

    private void enrichLegacyBooking(Booking booking, PropertyRepository propertyRepository) {
        BigDecimal bookingAmount = booking.getBookingAmount();
        if (bookingAmount == null || bookingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            Long propertyId = booking.getProperty() == null ? null : booking.getProperty().getId();
            BigDecimal propertyPrice = propertyId == null
                    ? null
                    : propertyRepository.findById(propertyId)
                    .map(Property::getPrice)
                    .orElse(null);
            bookingAmount = propertyPrice == null
                    ? new BigDecimal("100000")
                    : propertyPrice.multiply(new BigDecimal("0.10"));
            booking.setBookingAmount(bookingAmount);
        }
        if (booking.getAmountPaid() == null) {
            booking.setAmountPaid(booking.getStatus() == BookingStatus.CONFIRMED ? bookingAmount : BigDecimal.ZERO);
        }
        if (booking.getPaymentStatus() == null) {
            BigDecimal amountPaid = booking.getAmountPaid() == null ? BigDecimal.ZERO : booking.getAmountPaid();
            if (amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
                booking.setPaymentStatus(PaymentStatus.NOT_STARTED);
            } else if (amountPaid.compareTo(bookingAmount) >= 0) {
                booking.setPaymentStatus(PaymentStatus.PAID);
            } else {
                booking.setPaymentStatus(PaymentStatus.PARTIAL);
            }
        }
        if (booking.getPaymentReference() == null || booking.getPaymentReference().isBlank()) {
            booking.setPaymentReference("PAY-" + booking.getId());
        }
    }
}
