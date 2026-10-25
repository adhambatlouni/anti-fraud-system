package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.dto.BlacklistDtos.DeletionResponse;
import com.adham.antifraudsystem.dto.BlacklistDtos.StolenCardRequest;
import com.adham.antifraudsystem.dto.BlacklistDtos.StolenCardResponse;
import com.adham.antifraudsystem.repository.BlacklistRepository;
import com.adham.antifraudsystem.validation.CardValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

import static com.adham.antifraudsystem.service.AuditService.Action.CARD_FLAGGED;
import static com.adham.antifraudsystem.service.AuditService.Action.CARD_UNFLAGGED;
import static com.adham.antifraudsystem.service.AuditService.ResourceType.STOLEN_CARD;

@Service
public class StolenCardService {

    private final BlacklistRepository blacklistRepository;
    private final AuditService auditService;

    public StolenCardService(
            BlacklistRepository blacklistRepository,
            AuditService auditService) {

        this.blacklistRepository = blacklistRepository;
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

        long id = blacklistRepository.addCard(number);

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

        return blacklistRepository.findAllCards().stream()
                .map(entry -> new StolenCardResponse(
                        entry.id(),
                        entry.number()
                ))
                .toList();
    }

    @Transactional
    public DeletionResponse deleteCard(
            String number,
            String actorUsername) {

        if (!CardValidator.isValid(number)) {
            throw new IllegalArgumentException("Invalid card number");
        }

        int deletedRows = blacklistRepository.deleteCard(number);

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
