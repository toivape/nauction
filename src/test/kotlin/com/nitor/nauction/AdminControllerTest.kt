package com.nitor.nauction

import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminControllerTest (@Autowired private val mvc: MockMvc) {

    @Test
    fun `Health check is successful`() {
        mvc.get("/actuator/health")
            .andDo { print() }
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("UP") }
            }
    }

    @Test
    fun `Show admin page with auction items`() {
        mvc.get("/admin")
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content { string(Matchers.containsString("Satechi USB-C Multi-Port Adapter 4K Gigabit Ethernet V2")) }
            }
    }
}