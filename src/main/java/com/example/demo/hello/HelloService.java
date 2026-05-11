package com.example.demo.hello;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Service
public class HelloService {

    public HelloResponse greet() {
        return greet("World");
    }

    public HelloResponse greet(String name) {
        log.debug("greet called with name={}", name);
        return new HelloResponse("Hello, " + name + "!", Instant.now());
    }
}
