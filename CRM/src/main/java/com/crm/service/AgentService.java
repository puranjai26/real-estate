package com.crm.service;

import com.crm.dto.AgentRequest;
import com.crm.dto.AgentResponse;
import com.crm.dto.PageResponse;
import com.crm.entity.Agent;
import com.crm.entity.AppUser;
import com.crm.entity.enums.Role;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AgentRepository;
import com.crm.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final AgentRepository agentRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final DtoMapper dtoMapper;

    public AgentService(
            AgentRepository agentRepository,
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            DtoMapper dtoMapper
    ) {
        this.agentRepository = agentRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.dtoMapper = dtoMapper;
    }

    @Transactional
    public AgentResponse createAgent(AgentRequest request) {
        if (appUserRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email is already in use");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BadRequestException("Password is required for a new agent");
        }

        AppUser user = new AppUser();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.AGENT);

        Agent agent = new Agent();
        agent.setName(request.name());
        agent.setEmail(request.email());
        agent.setUser(user);

        Agent savedAgent = agentRepository.save(agent);
        log.info("Created agent {}", savedAgent.getEmail());
        return dtoMapper.toAgentResponse(savedAgent);
    }

    @Transactional(readOnly = true)
    public PageResponse<AgentResponse> getAgents(int page, int size) {
        Page<AgentResponse> responsePage = agentRepository.findAll(
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name")))
                .map(dtoMapper::toAgentResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public AgentResponse getAgentById(Long id) {
        return dtoMapper.toAgentResponse(findAgentEntity(id));
    }

    @Transactional
    public AgentResponse updateAgent(Long id, AgentRequest request) {
        Agent agent = findAgentEntity(id);
        if (!agent.getEmail().equalsIgnoreCase(request.email()) && appUserRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email is already in use");
        }

        agent.setName(request.name());
        agent.setEmail(request.email());
        agent.getUser().setName(request.name());
        agent.getUser().setEmail(request.email());

        if (request.password() != null && !request.password().isBlank()) {
            agent.getUser().setPassword(passwordEncoder.encode(request.password()));
        }

        log.info("Updated agent {}", agent.getEmail());
        return dtoMapper.toAgentResponse(agentRepository.save(agent));
    }

    @Transactional
    public void deleteAgent(Long id) {
        Agent agent = findAgentEntity(id);
        if (!agent.getProperties().isEmpty() || !agent.getBookings().isEmpty()) {
            throw new BadRequestException("Agent cannot be deleted while assigned to properties or bookings");
        }
        agentRepository.delete(agent);
        log.info("Deleted agent {}", agent.getEmail());
    }

    @Transactional(readOnly = true)
    public Agent findAgentEntity(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id " + id));
    }
}
