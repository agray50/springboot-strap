package com.example.demo;

import com.example.demo.hello.HelloService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HelloServiceTest {

    private final HelloService service = new HelloService();

    @Test
    void greet_returnsHelloWorld() {
        var response = service.greet();
        assertThat(response.message()).isEqualTo("Hello, World!");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void greet_withName_returnsPersonalisedMessage() {
        var response = service.greet("Alice");
        assertThat(response.message()).isEqualTo("Hello, Alice!");
    }
}
