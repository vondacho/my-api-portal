package io.obya.api.onboarding.adapter.in.web.config;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.springmvc.OpenApiValidationFilter;
import com.atlassian.oai.validator.springmvc.OpenApiValidationInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures OpenAPI contract validation for all HTTP requests/responses.
 * Validates incoming requests and outgoing responses against the OpenAPI specContent.
 */
@Configuration
public class OpenApiValidationConfig implements WebMvcConfigurer {

    @Bean
    public OpenApiValidationInterceptor openApiValidationInterceptor() {
        return new OpenApiValidationInterceptor(OpenApiInteractionValidator
                .createForSpecificationUrl("openapi/registration_v1.yaml").build());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(openApiValidationInterceptor())
                .addPathPatterns("/registrations/**");
    }

    @Bean
    public OpenApiValidationFilter openApiValidationFilter() {
        return new OpenApiValidationFilter(true, false);
    }

}
