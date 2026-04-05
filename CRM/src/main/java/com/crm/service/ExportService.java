package com.crm.service;

import com.crm.entity.Booking;
import com.crm.entity.Client;
import com.crm.entity.Lead;
import com.crm.entity.Property;
import com.crm.repository.BookingRepository;
import com.crm.repository.ClientRepository;
import com.crm.repository.LeadRepository;
import com.crm.repository.PropertyRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class ExportService {

    private final PropertyRepository propertyRepository;
    private final BookingRepository bookingRepository;
    private final ClientRepository clientRepository;
    private final LeadRepository leadRepository;

    public ExportService(
            PropertyRepository propertyRepository,
            BookingRepository bookingRepository,
            ClientRepository clientRepository,
            LeadRepository leadRepository
    ) {
        this.propertyRepository = propertyRepository;
        this.bookingRepository = bookingRepository;
        this.clientRepository = clientRepository;
        this.leadRepository = leadRepository;
    }

    @Transactional(readOnly = true)
    public byte[] exportPropertiesToCsv() {
        StringBuilder builder = new StringBuilder();
        builder.append("Id,Title,City,Locality,Type,Configuration,AreaSqFt,Price,Status,Featured,Broker\n");
        List<Property> properties = propertyRepository.findAll();
        for (Property property : properties) {
            builder.append(property.getId()).append(',')
                    .append(escape(property.getTitle())).append(',')
                    .append(escape(property.getCity())).append(',')
                    .append(escape(property.getLocality())).append(',')
                    .append(escape(property.getPropertyType() == null ? "" : property.getPropertyType().name())).append(',')
                    .append(escape(property.getConfiguration())).append(',')
                    .append(property.getAreaSqFt()).append(',')
                    .append(property.getPrice()).append(',')
                    .append(property.getStatus().name()).append(',')
                    .append(property.isFeatured()).append(',')
                    .append(escape(property.getAgent() == null ? "Unassigned" : property.getAgent().getName()))
                    .append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportClientsToCsv() {
        StringBuilder builder = new StringBuilder();
        builder.append("Id,Name,Email,Phone,PreferredLocation,Bookings\n");
        List<Client> clients = clientRepository.findAll();
        for (Client client : clients) {
            builder.append(client.getId()).append(',')
                    .append(escape(client.getName())).append(',')
                    .append(escape(client.getEmail())).append(',')
                    .append(escape(client.getPhone())).append(',')
                    .append(escape(client.getPreferredLocation() == null ? "" : client.getPreferredLocation())).append(',')
                    .append(client.getBookings().size())
                    .append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportLeadsToCsv() {
        StringBuilder builder = new StringBuilder();
        builder.append("Id,Name,Email,Phone,PreferredLocation,InterestType,Source,Stage,BudgetMin,BudgetMax,FollowUpDate,Broker\n");
        List<Lead> leads = leadRepository.findAll();
        for (Lead lead : leads) {
            builder.append(lead.getId()).append(',')
                    .append(escape(lead.getName())).append(',')
                    .append(escape(lead.getEmail())).append(',')
                    .append(escape(lead.getPhone())).append(',')
                    .append(escape(lead.getPreferredLocation())).append(',')
                    .append(escape(lead.getInterestType().name())).append(',')
                    .append(escape(lead.getSource().name())).append(',')
                    .append(escape(lead.getStage().name())).append(',')
                    .append(lead.getBudgetMin()).append(',')
                    .append(lead.getBudgetMax()).append(',')
                    .append(escape(lead.getFollowUpDate() == null ? "" : lead.getFollowUpDate().toString())).append(',')
                    .append(escape(lead.getAgent() == null ? "Unassigned" : lead.getAgent().getName()))
                    .append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportBookingsToExcel() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bookings");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Id");
            header.createCell(1).setCellValue("Property");
            header.createCell(2).setCellValue("Client");
            header.createCell(3).setCellValue("Agent");
            header.createCell(4).setCellValue("Date");
            header.createCell(5).setCellValue("Status");
            header.createCell(6).setCellValue("City");
            header.createCell(7).setCellValue("Locality");
            header.createCell(8).setCellValue("Booking Amount");
            header.createCell(9).setCellValue("Amount Paid");
            header.createCell(10).setCellValue("Payment Status");

            List<Booking> bookings = bookingRepository.findAll();
            for (int index = 0; index < bookings.size(); index++) {
                Booking booking = bookings.get(index);
                Row row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue(booking.getId());
                row.createCell(1).setCellValue(booking.getProperty().getTitle());
                row.createCell(2).setCellValue(booking.getClient().getName());
                row.createCell(3).setCellValue(booking.getAgent().getName());
                row.createCell(4).setCellValue(booking.getBookingDate().toString());
                row.createCell(5).setCellValue(booking.getStatus().name());
                row.createCell(6).setCellValue(booking.getProperty().getCity());
                row.createCell(7).setCellValue(booking.getProperty().getLocality());
                row.createCell(8).setCellValue(booking.getBookingAmount() == null ? 0 : booking.getBookingAmount().doubleValue());
                row.createCell(9).setCellValue(booking.getAmountPaid() == null ? 0 : booking.getAmountPaid().doubleValue());
                row.createCell(10).setCellValue(booking.getPaymentStatus() == null ? "NOT_STARTED" : booking.getPaymentStatus().name());
            }

            for (int column = 0; column < 11; column++) {
                sheet.autoSizeColumn(column);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private String escape(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
