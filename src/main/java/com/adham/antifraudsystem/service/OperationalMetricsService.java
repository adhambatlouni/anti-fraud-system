package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.dto.TransactionDtos.DecisionMetrics;
import com.adham.antifraudsystem.dto.TransactionDtos.FraudLimitsMetric;
import com.adham.antifraudsystem.dto.TransactionDtos.OperationalMetricsResponse;
import com.adham.antifraudsystem.dto.TransactionDtos.RegionMetric;
import com.adham.antifraudsystem.dto.TransactionDtos.ReviewMetrics;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

@Service
public class OperationalMetricsService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss")
                    .withResolverStyle(ResolverStyle.STRICT);

    private static final long MAX_WINDOW_DAYS = 366;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final FraudLimitService fraudLimitService;

    public OperationalMetricsService(
            NamedParameterJdbcTemplate jdbcTemplate,
            FraudLimitService fraudLimitService) {

        this.jdbcTemplate = jdbcTemplate;
        this.fraudLimitService = fraudLimitService;
    }

    @Transactional(readOnly = true)
    public OperationalMetricsResponse getMetrics(
            String fromValue,
            String toValue) {

        LocalDateTime from = parseDate(fromValue, "from");
        LocalDateTime to = parseDate(toValue, "to");
        validateWindow(from, to);

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to);

        Counts counts = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE result = 'ALLOWED') AS allowed,
                       COUNT(*) FILTER (
                           WHERE result = 'MANUAL_PROCESSING'
                       ) AS manual_processing,
                       COUNT(*) FILTER (
                           WHERE result = 'PROHIBITED'
                       ) AS prohibited,
                       COUNT(*) FILTER (WHERE feedback = '') AS pending,
                       COUNT(*) FILTER (WHERE feedback <> '') AS reviewed
                FROM transaction_history
                WHERE transaction_date BETWEEN :from AND :to
                """,
                parameters,
                (rs, rowNum) -> new Counts(
                        rs.getLong("total"),
                        rs.getLong("allowed"),
                        rs.getLong("manual_processing"),
                        rs.getLong("prohibited"),
                        rs.getLong("pending"),
                        rs.getLong("reviewed")
                )
        );

        List<RegionMetric> regions = jdbcTemplate.query(
                """
                SELECT region, COUNT(*) AS transaction_count
                FROM transaction_history
                WHERE transaction_date BETWEEN :from AND :to
                GROUP BY region
                ORDER BY transaction_count DESC, region ASC
                """,
                parameters,
                (rs, rowNum) -> new RegionMetric(
                        rs.getString("region"),
                        rs.getLong("transaction_count")
                )
        );

        FraudLimitService.Limits limits =
                fraudLimitService.getCurrentLimits();

        return new OperationalMetricsResponse(
                from.format(DATE_FORMATTER),
                to.format(DATE_FORMATTER),
                new DecisionMetrics(
                        counts.total(),
                        counts.allowed(),
                        counts.manualProcessing(),
                        counts.prohibited()
                ),
                new ReviewMetrics(counts.pending(), counts.reviewed()),
                regions,
                new FraudLimitsMetric(
                        limits.maxAllowed(),
                        limits.maxManual()
                )
        );
    }

    private LocalDateTime parseDate(String value, String parameter) {
        try {
            return LocalDateTime.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    parameter + " must use yyyy-MM-dd'T'HH:mm:ss"
            );
        }
    }

    private void validateWindow(
            LocalDateTime from,
            LocalDateTime to) {

        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "from must be earlier than or equal to to"
            );
        }

        if (Duration.between(from, to).compareTo(
                Duration.ofDays(MAX_WINDOW_DAYS)) > 0) {
            throw new IllegalArgumentException(
                    "Metrics window cannot exceed 366 days"
            );
        }
    }

    private record Counts(
            long total,
            long allowed,
            long manualProcessing,
            long prohibited,
            long pending,
            long reviewed
    ) {
    }
}
