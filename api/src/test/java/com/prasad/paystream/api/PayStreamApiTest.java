package com.prasad.paystream.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end HTTP test of the whole platform: accounts, a deposit out of the
 * seeded SYSTEM_CASH account, an idempotent transfer, and a reconciliation run.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PayStreamApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long createAccount(String name, String type) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"type\":\"" + type
                                + "\",\"currency\":\"USD\",\"allowNegative\":false}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long systemCashId() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode accounts = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode account : accounts) {
            if ("SYSTEM_CASH".equals(account.get("name").asText())) {
                return account.get("id").asLong();
            }
        }
        throw new IllegalStateException("SYSTEM_CASH seed account missing");
    }

    private void transfer(String key, long from, long to, long amount, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"" + key + "\",\"fromAccountId\":" + from
                                + ",\"toAccountId\":" + to + ",\"amountMinor\":" + amount
                                + ",\"currency\":\"USD\"}"))
                .andExpect(status().is(expectedStatus));
    }

    private long balanceOf(long accountId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/accounts/" + accountId))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("balanceMinor").asLong();
    }

    @Test
    void fullPaymentFlowOverHttp() throws Exception {
        long systemCash = systemCashId();
        long alice = createAccount("Alice", "LIABILITY");
        long bob = createAccount("Bob", "LIABILITY");

        // Deposit $100.00 into Alice's account out of SYSTEM_CASH.
        transfer("dep-1", systemCash, alice, 10_000, 201);
        // Alice pays Bob $25.00.
        transfer("pay-1", alice, bob, 2_500, 201);
        // Retry of the same payment (client timeout, double click, queue redelivery):
        // same key -> 200, original transfer, money does NOT move again.
        transfer("pay-1", alice, bob, 2_500, 200);

        assertThat(balanceOf(alice)).isEqualTo(7_500);
        assertThat(balanceOf(bob)).isEqualTo(2_500);
        assertThat(balanceOf(systemCash)).isEqualTo(-10_000);

        mockMvc.perform(post("/api/reconciliation/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanced").value(true))
                .andExpect(jsonPath("$.discrepancyCount").value(0));

        mockMvc.perform(get("/api/reconciliation/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanced").value(true));
    }

    @Test
    void overdraftAttemptIsRejectedWith422() throws Exception {
        long alice = createAccount("Carol", "LIABILITY");
        long bob = createAccount("Dave", "LIABILITY");

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"pay-broke\",\"fromAccountId\":" + alice
                                + ",\"toAccountId\":" + bob + ",\"amountMinor\":100,\"currency\":\"USD\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unknownAccountIs404() throws Exception {
        mockMvc.perform(get("/api/accounts/999999"))
                .andExpect(status().isNotFound());
    }
}
