package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.dto.BlacklistDtos.DeletionResponse;
import com.adham.antifraudsystem.dto.BlacklistDtos.SuspiciousIpRequest;
import com.adham.antifraudsystem.dto.BlacklistDtos.SuspiciousIpResponse;
import com.adham.antifraudsystem.validation.IpValidator;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

import static com.adham.antifraudsystem.service.AuditService.Action.BLACKLIST_IP_ADDED;
import static com.adham.antifraudsystem.service.AuditService.Action.BLACKLIST_IP_REMOVED;
import static com.adham.antifraudsystem.service.AuditService.ResourceType.SUSPICIOUS_IP;

@Service
public class SuspiciousIpService {

    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;

    public SuspiciousIpService(
            JdbcTemplate jdbcTemplate,
            AuditService auditService) {

        this.jdbcTemplate = jdbcTemplate;
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

        jdbcTemplate.update(
                "INSERT INTO suspicious_ips (ip) VALUES (?)",
                ip
        );

        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM suspicious_ips WHERE ip = ?",
                Long.class,
                ip
        );

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

        return jdbcTemplate.query(
                "SELECT id, ip FROM suspicious_ips ORDER BY id ASC",
                (rs, rowNum) -> new SuspiciousIpResponse(
                        rs.getLong("id"),
                        rs.getString("ip")
                )
        );
    }

    @Transactional
    public DeletionResponse deleteIp(String ip, String actorUsername) {

        if (!IpValidator.isValid(ip)) {
            throw new IllegalArgumentException("Invalid IP address");
        }

        int deletedRows = jdbcTemplate.update(
                "DELETE FROM suspicious_ips WHERE ip = ?",
                ip
        );

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
