package com.API_Gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

@Configuration
public class RouteConfig {

        @Bean
        public RouteLocator routes(RouteLocatorBuilder builder) {
            return builder.routes().route("auth-service", r -> r.path("/api/auth/**").uri("lb://auth-service")).route("doctor-service", r -> r.path("/api/doctors/**").uri("lb://doctor-service")).build();

        }
    }

    //This configuration class defines routing rules for the API Gateway using Spring Cloud Gateway.
// The RouteLocatorBuilder is used to create routes.
// Each route maps a request path to a specific microservice registered in Eureka.
// When a request matches a defined path,
// the gateway forwards it to the corresponding service using load balancing.

//config:This tells Spring that this class contains configuration settings for the application.
//Bean:This tells Spring to create and manage this method's return object
// as a Bean inside the Spring container.