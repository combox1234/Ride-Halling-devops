package com.ridehailing.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTest {

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @Test
    @DisplayName("Should successfully bootstrap Spring Cloud Gateway reactive context")
    void contextLoads() {
    }
}
