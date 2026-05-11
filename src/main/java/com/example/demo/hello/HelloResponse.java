package com.example.demo.hello;

import java.time.Instant;

public record HelloResponse(String message, Instant timestamp) {}
