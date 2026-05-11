package com.example.demo.hello;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hello")
@RequiredArgsConstructor
@Validated
public class HelloController {

    private final HelloService helloService;

    @GetMapping
    public ResponseEntity<HelloResponse> hello() {
        return ResponseEntity.ok(helloService.greet());
    }

    @GetMapping("/{name}")
    public ResponseEntity<HelloResponse> helloName(
            @PathVariable @NotBlank @Size(max = 50) String name) {
        return ResponseEntity.ok(helloService.greet(name));
    }
}
