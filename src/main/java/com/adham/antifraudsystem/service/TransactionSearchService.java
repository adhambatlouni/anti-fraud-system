package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.dto.TransactionDtos.TransactionSearchItem;
import com.adham.antifraudsystem.dto.TransactionDtos.TransactionSearchPage;
import com.adham.antifraudsystem.model.FraudTypes.TransactionResult;
import com.adham.antifraudsystem.model.Region;
import com.adham.antifraudsystem.validation.CardValidator;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TransactionSearchService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss")
                    .withResolverStyle(ResolverStyle.STRICT);

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "transactionId", "id",
            "amount", "amount",
            "date", "transaction_date",
            "result", "result"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TransactionSearchService(
            NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public TransactionSearchPage search(SearchCriteria criteria) {
        validate(criteria);

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        MapSqlParameterSource parameters = new MapSqlParameterSource();

        LocalDateTime from = parseDate(criteria.from(), "from");
        LocalDateTime to = parseDate(criteria.to(), "to");
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "from must be earlier than or equal to to"
            );
        }

        addEnumFilter(where, parameters, "result", "result",
                criteria.result(), TransactionResult.class);
        addEnumFilter(where, parameters, "region", "region",
                criteria.region(), Region.class);
        addFeedbackFilter(where, criteria.feedbackStatus());
        addDateFilter(where, parameters, "fromDate", ">=", from);
        addDateFilter(where, parameters, "toDate", "<=", to);

        if (criteria.cardLast4() != null) {
            if (!criteria.cardLast4().matches("[0-9]{4}")) {
                throw new IllegalArgumentException(
                        "cardLast4 must contain exactly four digits"
                );
            }
            where.append(" AND RIGHT(number, 4) = :cardLast4");
            parameters.addValue("cardLast4", criteria.cardLast4());
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transaction_history" + where,
                parameters,
                Long.class
        );
        long totalElements = total == null ? 0 : total;

        String sortColumn = SORT_COLUMNS.get(criteria.sort());
        String direction = criteria.direction().toUpperCase(Locale.ROOT);
        String secondarySort = sortColumn.equals("id")
                ? ""
                : ", id " + direction;

        parameters.addValue("limit", criteria.size());
        parameters.addValue("offset", (long) criteria.page() * criteria.size());

        List<TransactionSearchItem> content = jdbcTemplate.query(
                """
                SELECT id, amount, ip, number, region,
                       transaction_date, result, feedback
                FROM transaction_history
                """ + where
                        + " ORDER BY " + sortColumn + " " + direction
                        + secondarySort
                        + " LIMIT :limit OFFSET :offset",
                parameters,
                (rs, rowNum) -> new TransactionSearchItem(
                        rs.getLong("id"),
                        rs.getLong("amount"),
                        rs.getString("ip"),
                        CardValidator.mask(rs.getString("number")),
                        rs.getString("region"),
                        rs.getTimestamp("transaction_date")
                                .toLocalDateTime()
                                .format(DATE_FORMATTER),
                        rs.getString("result"),
                        rs.getString("feedback")
                )
        );

        int totalPages = totalElements == 0
                ? 0
                : (int) ((totalElements + criteria.size() - 1)
                / criteria.size());

        return new TransactionSearchPage(
                content,
                criteria.page(),
                criteria.size(),
                totalElements,
                totalPages
        );
    }

    private void validate(SearchCriteria criteria) {
        if (criteria.page() < 0) {
            throw new IllegalArgumentException("page must be zero or greater");
        }
        if (criteria.size() < 1 || criteria.size() > 100) {
            throw new IllegalArgumentException(
                    "size must be between 1 and 100"
            );
        }
        if (!SORT_COLUMNS.containsKey(criteria.sort())) {
            throw new IllegalArgumentException("Unsupported sort field");
        }
        if (!criteria.direction().equalsIgnoreCase("asc")
                && !criteria.direction().equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException(
                    "direction must be asc or desc"
            );
        }
    }

    private <E extends Enum<E>> void addEnumFilter(
            StringBuilder where,
            MapSqlParameterSource parameters,
            String column,
            String parameter,
            String value,
            Class<E> enumType) {

        if (value == null) {
            return;
        }

        try {
            E parsed = Enum.valueOf(enumType, value);
            where.append(" AND ").append(column)
                    .append(" = :").append(parameter);
            parameters.addValue(parameter, parsed.name());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unsupported " + parameter + " value"
            );
        }
    }

    private void addFeedbackFilter(
            StringBuilder where,
            String feedbackStatus) {

        if (feedbackStatus == null) {
            return;
        }

        switch (feedbackStatus) {
            case "PENDING" -> where.append(" AND feedback = ''");
            case "REVIEWED" -> where.append(" AND feedback <> ''");
            default -> throw new IllegalArgumentException(
                    "feedbackStatus must be PENDING or REVIEWED"
            );
        }
    }

    private void addDateFilter(
            StringBuilder where,
            MapSqlParameterSource parameters,
            String parameter,
            String operator,
            LocalDateTime value) {

        if (value != null) {
            where.append(" AND transaction_date ")
                    .append(operator)
                    .append(" :")
                    .append(parameter);
            parameters.addValue(parameter, value);
        }
    }

    private LocalDateTime parseDate(String value, String parameter) {
        if (value == null) {
            return null;
        }

        try {
            return LocalDateTime.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    parameter + " must use yyyy-MM-dd'T'HH:mm:ss"
            );
        }
    }

    public record SearchCriteria(
            String result,
            String region,
            String feedbackStatus,
            String from,
            String to,
            String cardLast4,
            int page,
            int size,
            String sort,
            String direction
    ) {
    }
}
