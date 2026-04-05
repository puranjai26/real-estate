package com.crm.service;

import com.crm.dto.ClientRequest;
import com.crm.dto.ClientResponse;
import com.crm.dto.PageResponse;
import com.crm.entity.Client;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    private static final Logger log = LoggerFactory.getLogger(ClientService.class);

    private final ClientRepository clientRepository;
    private final DtoMapper dtoMapper;

    public ClientService(ClientRepository clientRepository, DtoMapper dtoMapper) {
        this.clientRepository = clientRepository;
        this.dtoMapper = dtoMapper;
    }

    @Transactional
    public ClientResponse createClient(ClientRequest request) {
        if (clientRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Client email is already in use");
        }

        Client client = new Client();
        client.setName(request.name());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        client.setPreferredLocation(request.preferredLocation());

        Client savedClient = clientRepository.save(client);
        log.info("Created client {}", savedClient.getEmail());
        return dtoMapper.toClientResponse(savedClient);
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientResponse> getClients(int page, int size) {
        Page<ClientResponse> responsePage = clientRepository.findAll(
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(dtoMapper::toClientResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public ClientResponse getClientById(Long id) {
        return dtoMapper.toClientResponse(findClientEntity(id));
    }

    @Transactional
    public ClientResponse updateClient(Long id, ClientRequest request) {
        Client client = findClientEntity(id);
        if (clientRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new BadRequestException("Client email is already in use");
        }

        client.setName(request.name());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        client.setPreferredLocation(request.preferredLocation());

        log.info("Updated client {}", client.getEmail());
        return dtoMapper.toClientResponse(clientRepository.save(client));
    }

    @Transactional
    public void deleteClient(Long id) {
        Client client = findClientEntity(id);
        if (!client.getBookings().isEmpty()) {
            throw new BadRequestException("Client cannot be deleted while bookings exist");
        }
        clientRepository.delete(client);
        log.info("Deleted client {}", client.getEmail());
    }

    @Transactional(readOnly = true)
    public Client findClientEntity(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id " + id));
    }
}
