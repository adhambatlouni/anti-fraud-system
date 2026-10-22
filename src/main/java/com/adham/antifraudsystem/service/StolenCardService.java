package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.dto.BlacklistDtos.DeletionResponse;
import com.adham.antifraudsystem.dto.BlacklistDtos.StolenCardRequest;
import com.adham.antifraudsystem.dto.BlacklistDtos.StolenCardResponse;
import com.adham.antifraudsystem.validation.CardValidator;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

import static com.adham.antifraudsystem.service.AuditService.Action.CARD_FLAGGED;
import static com.adham.antifraudsystem.service.AuditService.Action.CARD_UNFLAGGED;
import static com.adham.antifraudsystem.service.AuditService.ResourceType.STOLEN_CARD;

@Service
public class StolenCardService {

    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;

    public StolenCardService(
            JdbcTemplate jdbcTemplate,
            AuditService auditService) {

        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
    }

    @Transactional
    public StolenCardResponse addCard(
            StolenCardRequest request,
            String actorUsername) {

        if (request == null || !CardValidator.isValid(request.number())) {
            throw new IllegalArgumentException("Invalid card number");
        }

        String number = request.number();

        jdbcTemplate.update(
                "INSERT INTO stolen_cards (number) VALUES (?)",
                number
        );

        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM stolen_cards WHERE number = ?",
                Long.class,
                number
        );

        auditService.record(
                actorUsername,
                CARD_FLAGGED,
                STOLEN_CARD,
                CardValidator.mask(number),
                "absent",
                "listed"
        );

        return new StolenCardResponse(id, number);
    }

    public List<StolenCardResponse> getAllCards() {

        return jdbcTemplate.query(
                "SELECT id, number FROM stolen_cards ORDER BY id ASC",
                (rs, rowNum) -> new StolenCardResponse(
                        rs.getLong("id"),
                        rs.getString("number")
                )
        );
    }

    @Transactional
    public DeletionResponse deleteCard(
            String number,
            String actorUsername) {

        if (!CardValidator.isValid(number)) {
            throw new IllegalArgumentException("Invalid card number");
        }

        int deletedRows = jdbcTemplate.update(
                "DELETE FROM stolen_cards WHERE number = ?",
                number
        );

        if (deletedRows == 0) {
            throw new NoSuchElementException("Card not found");
        }

        auditService.record(
                actorUsername,
                CARD_UNFLAGGED,
                STOLEN_CARD,
                CardValidator.mask(number),
                "listed",
                "absent"
        );

        return new DeletionResponse(
                "Card " + number + " successfully removed!"
        );
    }
}
