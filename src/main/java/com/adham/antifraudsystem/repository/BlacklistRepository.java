package com.adham.antifraudsystem.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BlacklistRepository {

    private final JdbcTemplate jdbcTemplate;

    public BlacklistRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long addIp(String ip) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO suspicious_ips (ip) VALUES (?) RETURNING id",
                Long.class,
                ip
        );
        return requiredId(id);
    }

    public List<SuspiciousIpEntry> findAllIps() {
        return jdbcTemplate.query(
                "SELECT id, ip FROM suspicious_ips ORDER BY id ASC",
                (rs, rowNum) -> new SuspiciousIpEntry(
                        rs.getLong("id"),
                        rs.getString("ip")
                )
        );
    }

    public boolean containsIp(String ip) {
        return count(
                "SELECT COUNT(*) FROM suspicious_ips WHERE ip = ?",
                ip
        ) > 0;
    }

    public int deleteIp(String ip) {
        return jdbcTemplate.update(
                "DELETE FROM suspicious_ips WHERE ip = ?",
                ip
        );
    }

    public long addCard(String number) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO stolen_cards (number) VALUES (?) RETURNING id",
                Long.class,
                number
        );
        return requiredId(id);
    }

    public List<StolenCardEntry> findAllCards() {
        return jdbcTemplate.query(
                "SELECT id, number FROM stolen_cards ORDER BY id ASC",
                (rs, rowNum) -> new StolenCardEntry(
                        rs.getLong("id"),
                        rs.getString("number")
                )
        );
    }

    public boolean containsCard(String number) {
        return count(
                "SELECT COUNT(*) FROM stolen_cards WHERE number = ?",
                number
        ) > 0;
    }

    public int deleteCard(String number) {
        return jdbcTemplate.update(
                "DELETE FROM stolen_cards WHERE number = ?",
                number
        );
    }

    private long count(String sql, String value) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, value);
        return count == null ? 0 : count;
    }

    private long requiredId(Long id) {
        if (id == null) {
            throw new IllegalStateException("Blacklist insert failed");
        }
        return id;
    }

    public record SuspiciousIpEntry(long id, String ip) {
    }

    public record StolenCardEntry(long id, String number) {
    }
}
