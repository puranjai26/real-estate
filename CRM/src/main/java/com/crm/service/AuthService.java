package com.crm.service;

import com.crm.config.JwtService;
import com.crm.dto.AuthRequest;
import com.crm.dto.AuthResponse;
import com.crm.entity.Agent;
import com.crm.entity.AppUser;
import com.crm.repository.AgentRepository;
import com.crm.repository.AppUserRepository;
import com.crm.exception.ResourceNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserDetailsService appUserDetailsService;
    private final AppUserRepository appUserRepository;
    private final AgentRepository agentRepository;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            AppUserDetailsService appUserDetailsService,
            AppUserRepository appUserRepository,
            AgentRepository agentRepository,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.appUserDetailsService = appUserDetailsService;
        this.appUserRepository = appUserRepository;
        this.agentRepository = agentRepository;
        this.jwtService = jwtService;
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails userDetails = appUserDetailsService.loadUserByUsername(request.email());
        AppUser user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtService.generateToken(userDetails);
        Long brokerId = agentRepository.findByUserId(user.getId()).map(Agent::getId).orElse(null);

        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                brokerId,
                brokerId
        );
    }

    public AuthResponse getProfile(String email) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Long brokerId = agentRepository.findByUserId(user.getId()).map(Agent::getId).orElse(null);
        return new AuthResponse(null, "Bearer", user.getId(), user.getName(), user.getEmail(), user.getRole().name(), brokerId, brokerId);
    }
}
