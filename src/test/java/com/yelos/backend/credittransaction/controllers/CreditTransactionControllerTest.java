package com.yelos.backend.credittransaction.controllers;

import com.yelos.backend.credittransaction.dto.CreditsReponse;
import com.yelos.backend.credittransaction.services.CreditServices;
import com.yelos.backend.security.JwtService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CreditTransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class CreditTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreditServices creditServices;

    @MockitoBean
    private JwtService jwtService;

    @Nested
    class AddCreditsTests {

        @Test
        void addCreditsToUserId_validRequest_returnsCreditsResponse() throws Exception {
            Long userId = 1L;

            CreditsReponse response = new CreditsReponse(
                    userId,
                    BigDecimal.valueOf(150)
            );

            when(creditServices.addCredits(argThat(request ->
                    request.userId().equals(userId)
                            && request.credits().compareTo(BigDecimal.valueOf(50)) == 0
            ))).thenReturn(response);

            mockMvc.perform(post("/api/credittransaction/add")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "userId": 1,
                                      "credits": 50
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(1L))
                    .andExpect(jsonPath("$.credits").value(150));

            verify(creditServices).addCredits(argThat(request ->
                    request.userId().equals(userId)
                            && request.credits().compareTo(BigDecimal.valueOf(50)) == 0
            ));
        }
    }
}