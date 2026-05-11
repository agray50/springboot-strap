package com.example.demo;

import com.example.demo.config.SecurityConfig;
import com.example.demo.hello.HelloController;
import com.example.demo.hello.HelloResponse;
import com.example.demo.hello.HelloService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HelloController.class)
@Import(SecurityConfig.class)
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HelloService helloService;

    @Test
    void getHello_returns200WithHelloWorld() throws Exception {
        when(helloService.greet()).thenReturn(new HelloResponse("Hello, World!", Instant.now()));

        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, World!"));
    }

    @Test
    void getHelloWithName_returns200WithHelloAlice() throws Exception {
        when(helloService.greet("Alice")).thenReturn(new HelloResponse("Hello, Alice!", Instant.now()));

        mockMvc.perform(get("/api/hello/Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, Alice!"));
    }

    @Test
    void getHelloWithNameTooLong_returns400() throws Exception {
        mockMvc.perform(get("/api/hello/" + "a".repeat(51)))
                .andExpect(status().isBadRequest());
    }
}
