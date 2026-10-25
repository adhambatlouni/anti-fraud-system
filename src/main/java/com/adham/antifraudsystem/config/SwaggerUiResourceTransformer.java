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

    private static final String DECISION_SHOWCASE_CSS = """

            /* Compact presentation for the decision-inspection showcase. */
            %1$s .opblock-description-wrapper,
            %1$s .parameters-container,
            %1$s .opblock-section-header,
            %1$s .try-out,
            %1$s .execute-wrapper,
            %1$s .responses-wrapper > .opblock-section-header,
            %1$s .responses-inner > h4,
            %1$s .responses-inner > h5,
            %1$s .responses-table thead,
            %1$s .response-col_description__inner > .renderedMarkdown,
            %1$s .response-controls,
            %1$s .model-example > .tab,
            %1$s .example,
            %1$s .response-col_links {
                display: none;
            }

            %1$s .responses-wrapper,
            %1$s .responses-inner,
            %1$s .responses-table,
            %1$s .response-col_description,
            %1$s .model-example,
            %1$s .highlight-code {
                margin: 0;
                padding-top: 0;
            }

            %1$s .responses-table {
                border-collapse: separate;
                border-spacing: 0 12px;
            }

            %1$s .response-col_status {
                color: #1b5e20;
                font-size: 17px;
                font-weight: 700;
                padding: 14px 12px 0 0;
                white-space: nowrap;
            }

            %1$s .response-col_status::after {
                content: " OK";
            }

            %1$s .response-col_description {
                width: 100%%;
            }

            %1$s .highlight-code pre {
                border-radius: 6px;
                max-height: none;
                padding: 18px;
            }
            """.formatted(DECISION_OPERATION);

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
