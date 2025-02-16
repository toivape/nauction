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
class IndexControllerTest(@Autowired private val mvc: MockMvc) {

    @Test
    fun `Show front page`() {
        mvc.get("/")
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content { string(Matchers.containsString("Computer accessories")) }
            }
    }
}