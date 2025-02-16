package com.nitor.nauction

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ApiControllerTest (
    @Autowired private val mvc: MockMvc,
    @Autowired private val dao: AuctionItemDao,
    @Autowired private val objectMapper: ObjectMapper) {

    private val item = NewAuctionItem(
        id = "c22c60e8-2268-4c78-ac2f-4110ca5f169c",
        description = "OnePlus 13 5G puhelin, 512/16 Gt",
        category = "Phone",
        purchaseDate = LocalDate.of(2023, 2, 13),
        purchasePrice = BigDecimal("1149.00"),
        biddingStartingPrice = BigDecimal("100.00")
    )

    private fun postItem(item: NewAuctionItem) =
        mvc.post("/api/auctionitems") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(item)
        }

    @Test
    fun `New auction item is added to database`(){
        val json = objectMapper.writeValueAsString(item)
        print(json)
        postItem(item)
        .andExpect {
            status { isCreated() }
        }

        val dbItem = dao.findByExternalId(item.id)
        withClue("Auction item with external id ${item.id} should exist in database") {
            dbItem.shouldNotBeNull()
        }
    }

    @Test
    fun `Adding duplicate auction item causes exception`(){
        val newItem1 = item.copy(id = "c22c60e8-2268-4c78-ac2f-4110ca5f169d")
        postItem(newItem1).andExpect { status { isCreated() } }
        postItem(newItem1).andDo { print() }.andExpect { status { isBadRequest() } }
    }

}