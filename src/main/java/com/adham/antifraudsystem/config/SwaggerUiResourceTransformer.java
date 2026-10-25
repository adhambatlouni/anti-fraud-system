package com.adham.antifraudsystem.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SwaggerUiResourceTransformer
        extends SwaggerIndexPageTransformer {

    private static final String SWAGGER_CSS = "swagger-ui.css";
    private static final String DECISION_OPERATION =
            "#operations-Investigation-inspectOriginalFraudDecision";
    private static final String TRANSACTION_SEARCH_OPERATION =
            "#operations-Investigation-search";

    private static final String DECISION_SHOWCASE_CSS = """

            /* Focused presentation for the decision-inspection showcase. */
            %2$s {
                display: none;
            }

            %1$s .try-out,
            %1$s .execute-wrapper,
            %1$s .responses-inner > h4,
            %1$s .responses-inner > h5,
            %1$s .curl-command,
            %1$s .request-url,
            %1$s .response-headers,
            %1$s .response-headers > h4,
            %1$s .response-controls,
            %1$s .model-example > .tab,
            %1$s .example,
            %1$s .response-col_links {
                display: none;
            }

            %1$s .opblock-description-wrapper,
            %1$s .parameters-container,
            %1$s .responses-wrapper {
                margin-left: 20px;
                margin-right: 20px;
            }

            %1$s .responses-table {
                margin-bottom: 20px;
            }

            %1$s .responses-wrapper .opblock-section-header h4 {
                font-size: 14px;
            }

            %1$s .responses-table thead th {
                font-size: 13px;
            }

            %1$s .response-col_status {
                color: #3b4151;
                font-size: 14px;
                font-weight: 600;
                white-space: nowrap;
            }

            %1$s .response-col_description {
                width: 100%%;
            }

            %1$s .response-col_description__inner > .renderedMarkdown p {
                font-size: 16px;
                font-weight: 600;
                margin: 0 0 10px;
            }

            %1$s .model-example,
            %1$s .highlight-code {
                margin: 0;
            }

            %1$s .highlight-code pre {
                border-radius: 6px;
                font-size: 15px;
                line-height: 1.5;
                max-height: none;
                padding: 18px;
            }
            """.formatted(
                    DECISION_OPERATION,
                    TRANSACTION_SEARCH_OPERATION
            );

    public SwaggerUiResourceTransformer(
            SwaggerUiConfigProperties swaggerUiConfig,
            SwaggerUiOAuthProperties swaggerUiOAuth,
            SwaggerWelcomeCommon swaggerWelcome,
            ObjectMapperProvider objectMapperProvider) {

        super(
                swaggerUiConfig,
                swaggerUiOAuth,
                swaggerWelcome,
                objectMapperProvider
        );
    }

    @Override
    public Resource transform(
            HttpServletRequest request,
            Resource resource,
            ResourceTransformerChain transformerChain) throws IOException {

        Resource transformed = super.transform(
                request,
                resource,
                transformerChain
        );

        if (!SWAGGER_CSS.equals(resource.getFilename())) {
            return transformed;
        }

        String css = new String(
                transformed.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        return new TransformedResource(
                transformed,
                (css + DECISION_SHOWCASE_CSS).getBytes(StandardCharsets.UTF_8)
        );
    }
}
