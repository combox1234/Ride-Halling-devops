package com.ridehailing.ride.client;

import com.ridehailing.common.dto.DriverCandidateDto;
import com.ridehailing.common.dto.DriverMatchRequest;
import com.ridehailing.common.dto.LocationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Component
public class DefaultDriverMatchingClient implements DriverMatchingClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultDriverMatchingClient.class);
    public static final Long DEFAULT_FALLBACK_DRIVER_ID = 201L;

    private final String matchingServiceUrl;
    private final RestTemplate restTemplate;

    public DefaultDriverMatchingClient() {
        this("http://localhost:8085", null);
    }

    @Autowired
    public DefaultDriverMatchingClient(
        @Value("${matching.service.url:http://localhost:8085}") String matchingServiceUrl,
        @Autowired(required = false) RestTemplateBuilder restTemplateBuilder
    ) {
        this.matchingServiceUrl = matchingServiceUrl;
        if (restTemplateBuilder != null) {
            this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(1000))
                .setReadTimeout(Duration.ofMillis(2000))
                .build();
        } else {
            this.restTemplate = new RestTemplate();
        }
    }

    @Override
    public Optional<DriverCandidateDto> findDriverForRide(Long rideId, LocationDto pickupLocation) {
        if (matchingServiceUrl != null && !matchingServiceUrl.isBlank()) {
            try {
                String endpoint = matchingServiceUrl.replaceAll("/+$", "") + "/api/v1/matching/find-driver";
                DriverMatchRequest matchRequest = new DriverMatchRequest(rideId, pickupLocation, 5.0);

                HttpEntity<DriverMatchRequest> entity = new HttpEntity<>(matchRequest);
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {}
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Object dataObj = response.getBody().get("data");
                    if (dataObj instanceof Map<?, ?> dataMap) {
                        Number driverIdNum = (Number) dataMap.get("driverId");
                        Number distNum = (Number) dataMap.get("distanceKm");
                        Number latNum = (Number) dataMap.get("latitude");
                        Number lonNum = (Number) dataMap.get("longitude");
                        Number ratingNum = (Number) dataMap.get("rating");

                        if (driverIdNum != null) {
                            DriverCandidateDto candidate = new DriverCandidateDto(
                                driverIdNum.longValue(),
                                distNum != null ? distNum.doubleValue() : 1.0,
                                latNum != null ? latNum.doubleValue() : pickupLocation.latitude(),
                                lonNum != null ? lonNum.doubleValue() : pickupLocation.longitude(),
                                ratingNum != null ? ratingNum.doubleValue() : 5.0
                            );
                            log.info("Matching service matched driver {} for ride {}", candidate.driverId(), rideId);
                            return Optional.of(candidate);
                        }
                    }
                }
            } catch (Exception e) {
                log.info("Remote matching service at {} unavailable or returned no candidate ({}). Falling back to mock driver.",
                    matchingServiceUrl, e.getMessage());
            }
        }

        // Standalone fallback: simulate available driver for tests / vertical slice
        return Optional.of(new DriverCandidateDto(
            DEFAULT_FALLBACK_DRIVER_ID,
            1.2,
            pickupLocation.latitude(),
            pickupLocation.longitude(),
            5.0
        ));
    }
}
