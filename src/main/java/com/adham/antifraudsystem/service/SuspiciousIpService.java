package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.dto.BlacklistDtos.DeletionResponse;
import com.adham.antifraudsystem.dto.BlacklistDtos.SuspiciousIpRequest;
import com.adham.antifraudsystem.dto.BlacklistDtos.SuspiciousIpResponse;
import com.adham.antifraudsystem.repository.BlacklistRepository;
import com.adham.antifraudsystem.validation.IpValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

import static com.adham.antifraudsystem.service.AuditService.Action.BLACKLIST_IP_ADDED;
import static com.adham.antifraudsystem.service.AuditService.Action.BLACKLIST_IP_REMOVED;
import static com.adham.antifraudsystem.service.AuditService.ResourceType.SUSPICIOUS_IP;

@Service
public class SuspiciousIpService {

    private final BlacklistRepository blacklistRepository;
    private final AuditService auditService;

    public SuspiciousIpService(
            BlacklistRepository blacklistRepository,
            AuditService auditService) {

        this.blacklistRepository = blacklistRepository;
        this.auditService = auditService;
    }

    @Transactional
    public SuspiciousIpResponse addIp(
            SuspiciousIpRequest request,
            String actorUsername) {

        if (request == null || !IpValidator.isValid(request.ip())) {
            throw new IllegalArgumentException("Invalid IP address");
        }

        String ip = request.ip();

        long id = blacklistRepository.addIp(ip);

        auditService.record(
                actorUsername,
                BLACKLIST_IP_ADDED,
                SUSPICIOUS_IP,
                ip,
                "absent",
                "listed"
        );

        return new SuspiciousIpResponse(id, ip);
    }

    public List<SuspiciousIpResponse> getAllIps() {

        return blacklistRepository.findAllIps().stream()
                .map(entry -> new SuspiciousIpResponse(
                        entry.id(),
                        entry.ip()
                ))
                .toList();
    }

    @Transactional
    public DeletionResponse deleteIp(String ip, String actorUsername) {

        if (!IpValidator.isValid(ip)) {
            throw new IllegalArgumentException("Invalid IP address");
        }

        int deletedRows = blacklistRepository.deleteIp(ip);

        if (deletedRows == 0) {
            throw new NoSuchElementException("IP address not found");
        }

        auditService.record(
                actorUsername,
                BLACKLIST_IP_REMOVED,
                SUSPICIOUS_IP,
                ip,
                "listed",
                "absent"
        );

        return new DeletionResponse(
                "IP " + ip + " successfully removed!"
        );
    }

}
