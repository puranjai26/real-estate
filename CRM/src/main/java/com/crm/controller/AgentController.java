package com.crm.controller;

import com.crm.dto.AgentRequest;
import com.crm.dto.AgentResponse;
import com.crm.dto.PageResponse;
import com.crm.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    public AgentResponse createAgent(@Valid @RequestBody AgentRequest request) {
        return agentService.createAgent(request);
    }

    @GetMapping
    public PageResponse<AgentResponse> getAgents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return agentService.getAgents(page, size);
    }

    @GetMapping("/{id}")
    public AgentResponse getAgentById(@PathVariable Long id) {
        return agentService.getAgentById(id);
    }

    @PutMapping("/{id}")
    public AgentResponse updateAgent(@PathVariable Long id, @Valid @RequestBody AgentRequest request) {
        return agentService.updateAgent(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteAgent(@PathVariable Long id) {
        agentService.deleteAgent(id);
    }
}
