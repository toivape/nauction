package com.nitor.nauction

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ApiControllerTest (
    @Autowired private val mvc: MockMvc,
    @Autowired private val auctionDao: AuctionDao,
    @Autowired private val bidDao: BidDao,
    @Autowired private val objectMapper: ObjectMapper) {

    private val item = NewAuctionItem(
        id = "c22c60e8-2268-4c78-ac2f-4110ca5f169c",
        description = "OnePlus 13 5G puhelin, 512/16 Gt",
        category = "Phone",
        purchaseDate = LocalDate.of(2023, 2, 13),
        purchasePrice = BigDecimal("1149.00"),
        startingPrice = BigDecimal("100.00")
    )

    private fun postItem(item: NewAuctionItem) =
        mvc.post("/api/auctionitems") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(item)
        }

    @Test
    fun `New auction item is added to database`() {
        postItem(item).andExpect { status { isCreated() } }

        val dbItem = auctionDao.findAllOpen().first { it.externalId == item.id }
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

    @Sql(statements = ["INSERT INTO auction_item (id, external_id, description, category, purchase_date, purchase_price, bidding_end_date, starting_price) VALUES ('01951f4a-48ac-7c5f-8db1-1ef9efc5e10d','01951f4a-48ac-7ece-acac-f7c443787795','Ubiquiti UniFi 7 Pro -WiFi-tukiasema', 'Network', '2023-10-06', '249.99',  NOW() + interval '3' month, '42.00')"])
    @Test
    fun `User makes a bid as a first bidder`() {
        val auctionItemId = "01951f4a-48ac-7c5f-8db1-1ef9efc5e10d"
        val bidRequest = BidRequest(amount = 1, lastBidId = "")
        mvc.post("/api/auctionitems/$auctionItemId/bid") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(bidRequest)
        }.andExpect {
            status { isCreated() }
        }

        // Make sure the bid is in the database
        bidDao.findByAuctionItemId(auctionItemId).apply {
            withClue("There should be one bid for auction item $auctionItemId") {
                size shouldBe 1
                first().bidPrice shouldBe BigDecimal("42.00")
            }
        }
    }

    @Test
    fun `Get auction item`(){
        mvc.get("/api/auctionitems/d1d018fe-cc1b-4f9c-9d53-bc8f5dd9b515")
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content { string(Matchers.containsString(",\"currentPrice\":275.00")) }
            }
    }

    @Test
    fun `Auction item not found`(){
        mvc.get("/api/auctionitems/01951e70-fd92-78b3-92ab-d2f92e0586ba")
            .andExpect {
                status { isNotFound() }
            }
    }
}