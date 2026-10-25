package com.adham.antifraudsystem.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI antiFraudOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Anti-Fraud System API")
                        .version("2.1.0")
                        .contact(new Contact()
                                .name("Adham Batlouni")
                                .url("https://github.com/adhambatlouni/anti-fraud-system"))
                        .description("""
                                A rule-based fraud operations API for evaluating transactions,
                                investigating decisions, managing risk signals, and improving amount
                                thresholds through specialist feedback.

                                ### Quick start
                                1. Register an account with **POST `/api/auth/user`**.
                                2. Select **Authorize** and enter the account's HTTP Basic credentials.
                                3. An **ADMINISTRATOR** assigns roles and unlocks new accounts.
                                4. A **MERCHANT** submits transactions for evaluation.
                                5. **SUPPORT** investigates decisions, maintains blacklists, and submits feedback.

                                ### Roles
                                | Role | Main responsibilities |
                                | --- | --- |
                                | `ADMINISTRATOR` | Manage users, roles, access, and view metrics/audit events |
                                | `MERCHANT` | Submit transactions for fraud evaluation |
                                | `SUPPORT` | Review history, manage blacklists, give feedback, and investigate evidence |

                                ### Decisions
                                - `ALLOWED` — no risk rule requires intervention.
                                - `MANUAL_PROCESSING` — a specialist should review the transaction.
                                - `PROHIBITED` — one or more high-risk rules blocked the transaction.

                                > Use only the synthetic card numbers shown in the examples. This demonstration
                                > API is not designed to collect or process real payment-card information.
                                """))
                .tags(List.of(
                        new Tag()
                                .name("Accounts")
                                .description("Register users and manage their roles and access."),
                        new Tag()
                                .name("Transaction evaluation")
                                .description("Submit a transaction and receive its fraud decision."),
                        new Tag()
                                .name("Fraud blacklists")
                                .description("Maintain suspicious IP and synthetic stolen-card signals."),
                        new Tag()
                                .name("Transaction review")
                                .description("Read transaction history and submit specialist feedback."),
                        new Tag()
                                .name("Investigation")
                                .description("Search masked transaction records and inspect captured decision evidence."),
                        new Tag()
                                .name("Audit trail")
                                .description("Review append-only records of sensitive fraud operations."),
                        new Tag()
                                .name("Operational metrics")
                                .description("View aggregated decision and review workload metrics.")))
                .components(new Components().addSecuritySchemes(
                        BASIC_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("Enter the username and password of an enabled application user. "
                                        + "Each operation states its required role.")
                ))
                .externalDocs(new ExternalDocumentation()
                        .description("Architecture and design decisions")
                        .url("https://github.com/adhambatlouni/anti-fraud-system/blob/main/docs/ARCHITECTURE.md"));
    }
}
