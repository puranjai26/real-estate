package com.crm.service;

import com.crm.dto.ClientResponse;
import com.crm.dto.LeadRequest;
import com.crm.dto.LeadResponse;
import com.crm.dto.PageResponse;
import com.crm.entity.Agent;
import com.crm.entity.Client;
import com.crm.entity.Lead;
import com.crm.entity.enums.LeadSource;
import com.crm.entity.enums.LeadStage;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.ClientRepository;
import com.crm.repository.LeadRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class LeadService {

    private static final Logger log = LoggerFactory.getLogger(LeadService.class);

    private final LeadRepository leadRepository;
    private final ClientRepository clientRepository;
    private final AgentService agentService;
    private final DtoMapper dtoMapper;

    public LeadService(
            LeadRepository leadRepository,
            ClientRepository clientRepository,
            AgentService agentService,
            DtoMapper dtoMapper
    ) {
        this.leadRepository = leadRepository;
        this.clientRepository = clientRepository;
        this.agentService = agentService;
        this.dtoMapper = dtoMapper;
    }

    @Transactional
    public LeadResponse createLead(LeadRequest request) {
        if (leadRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Lead email is already in use");
        }
        Lead lead = new Lead();
        applyRequest(lead, request);
        Lead savedLead = leadRepository.save(lead);
        log.info("Created lead {}", savedLead.getEmail());
        return dtoMapper.toLeadResponse(savedLead);
    }

    @Transactional(readOnly = true)
    public PageResponse<LeadResponse> getLeads(
            LeadStage stage,
            LeadSource source,
            Long agentId,
            LocalDate followUpDate,
            int page,
            int size
    ) {
        Specification<Lead> specification = buildSpecification(stage, source, agentId, followUpDate);
        Page<LeadResponse> responsePage = leadRepository.findAll(
                        specification,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "followUpDate").and(Sort.by(Sort.Direction.DESC, "createdAt"))))
                .map(dtoMapper::toLeadResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public LeadResponse getLeadById(Long id) {
        return dtoMapper.toLeadResponse(findLeadEntity(id));
    }

    @Transactional
    public LeadResponse updateLead(Long id, LeadRequest request) {
        Lead lead = findLeadEntity(id);
        if (leadRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new BadRequestException("Lead email is already in use");
        }
        applyRequest(lead, request);
        Lead updatedLead = leadRepository.save(lead);
        log.info("Updated lead {}", updatedLead.getEmail());
        return dtoMapper.toLeadResponse(updatedLead);
    }

    @Transactional
    public void deleteLead(Long id) {
        Lead lead = findLeadEntity(id);
        leadRepository.delete(lead);
        log.info("Deleted lead {}", lead.getEmail());
    }

    @Transactional
    public ClientResponse convertLead(Long id) {
        Lead lead = findLeadEntity(id);
        if (clientRepository.existsByEmail(lead.getEmail())) {
            throw new BadRequestException("A client already exists with this email");
        }

        Client client = new Client();
        client.setName(lead.getName());
        client.setEmail(lead.getEmail());
        client.setPhone(lead.getPhone());
        client.setPreferredLocation(lead.getPreferredLocation());
        Client savedClient = clientRepository.save(client);

        lead.setStage(LeadStage.CONVERTED);
        leadRepository.save(lead);
        log.info("Converted lead {} to client {}", lead.getEmail(), savedClient.getEmail());
        return dtoMapper.toClientResponse(savedClient);
    }

    @Transactional(readOnly = true)
    public Lead findLeadEntity(Long id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id " + id));
    }

    private void applyRequest(Lead lead, LeadRequest request) {
        Agent agent = request.agentId() == null ? null : agentService.findAgentEntity(request.agentId());
        lead.setName(request.name());
        lead.setEmail(request.email());
        lead.setPhone(request.phone());
        lead.setPreferredLocation(request.preferredLocation());
        lead.setBudgetMin(request.budgetMin());
        lead.setBudgetMax(request.budgetMax());
        lead.setInterestType(request.interestType());
        lead.setSource(request.source());
        lead.setFollowUpDate(request.followUpDate());
        lead.setStage(request.stage());
        lead.setNotes(request.notes());
        lead.setAgent(agent);
    }

    private Specification<Lead> buildSpecification(
            LeadStage stage,
            LeadSource source,
            Long agentId,
            LocalDate followUpDate
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (stage != null) {
                predicates.add(criteriaBuilder.equal(root.get("stage"), stage));
            }
            if (source != null) {
                predicates.add(criteriaBuilder.equal(root.get("source"), source));
            }
            if (agentId != null) {
                predicates.add(criteriaBuilder.equal(root.join("agent").get("id"), agentId));
            }
            if (followUpDate != null) {
                predicates.add(criteriaBuilder.equal(root.get("followUpDate"), followUpDate));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
