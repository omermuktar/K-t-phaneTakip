package com.omermuktar.library.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "Kütüphane Yönetim API'si",
            version = "1.0.0",
            description = "Kitap, üye ve ödünç süreçlerini yöneten REST API",
            contact = @Contact(name = "Ömer Şamil Muktar", url = "https://github.com/omermuktar"),
            license = @License(name = "MIT")))
public class OpenApiConfig {}
