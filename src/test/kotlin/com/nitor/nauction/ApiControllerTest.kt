package com.nitor.nauction

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveAtLeastSize
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
class ApiControllerTest(
    @Autowired private val mvc: MockMvc,
    @Autowired private val auctionDao: AuctionDao,
    @Autowired private val bidDao: BidDao,
    @Autowired private val bidService: BidService,
    @Autowired private val objectMapper: ObjectMapper
) {

    private val item = NewAuctionItem(
        id = "c22c60e8-2268-4c78-ac2f-4110ca5f169c",
        description = "OnePlus 13 5G puhelin, 512/16 Gt",
        category = "Phone",
        purchaseDate = LocalDate.of(2023, 2, 13),
        purchasePrice = BigDecimal("1149.00"),
        startingPrice = 100
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
    fun `New auction item has invalid data`() {
        val newItem = item.copy(id = "", description = "")
        postItem(newItem).andDo { print() }.andExpect {
            status { isBadRequest() }
            content { string(Matchers.containsString("description is mandatory")) }
            content { string(Matchers.containsString("id is mandatory")) }
        }
    }

    @Test
    fun `Adding duplicate auction item causes exception`() {
        val newItem1 = item.copy(id = "c22c60e8-2268-4c78-ac2f-4110ca5f169d")
        postItem(newItem1).andExpect { status { isCreated() } }
        postItem(newItem1).andDo { print() }.andExpect { status { isBadRequest() } }
    }

    @Sql(statements = ["INSERT INTO auction_item (id, external_id, description, category, purchase_date, purchase_price, bidding_end_date, starting_price) VALUES ('01951f4a-48ac-7c5f-8db1-1ef9efc5e10d','01951f4a-48ac-7ece-acac-f7c443787795','Ubiquiti UniFi 7 Pro -WiFi-tukiasema', 'Network', '2023-10-06', '249.99',  NOW() + interval '3' month, 42)"])
    @Test
    fun `User makes a bid as a first bidder`() {
        val auctionItemId = "01951f4a-48ac-7c5f-8db1-1ef9efc5e10d"
        val bidRequest = BidRequest(amount = 1, lastBidId = "")
        mvc.post("/api/auctionitems/$auctionItemId/bids") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(bidRequest)
        }
            .andDo { print() }
            .andExpect {
                status { isCreated() }
            }

        // Make sure the bid is in the database
        bidDao.findByAuctionItemId(auctionItemId).apply {
            withClue("There should be one bid for auction item $auctionItemId") {
                size shouldBe 1
                first().bidPrice shouldBe 42
            }
        }
    }

    @Test
    fun `User places a bid when there are existing bids`() {
        val auctionItemId = "b2ce636c-9d81-4ba4-bab8-f2ffaa91293c"
        val latestBid = bidService.getLatestBid(auctionItemId)!!
        val bidRequest = BidRequest(amount = 9, lastBidId = latestBid.lastBidId)
        val expectedPrice = latestBid.currentPrice + bidRequest.amount!!

        mvc.post("/api/auctionitems/$auctionItemId/bids") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(bidRequest)
        }
            .andDo { print() }
            .andExpect {
                status { isCreated() }
                content { string(Matchers.containsString(",\"currentPrice\":$expectedPrice")) }
            }

        // Make sure the bid is in the database
        bidDao.findByAuctionItemId(auctionItemId).apply {
            shouldHaveAtLeastSize(4)
            last().bidPrice shouldBe bidRequest.amount
        }
    }

    @Test
    fun `User is not able to bid on an expired auction item`() {
        val auctionItemId = "271aebdf-b53d-4748-8dce-a67f6ece3399"
        val bidRequest = BidRequest(amount = 5, lastBidId = "f40d0d08-8f37-4e60-bb65-54207c98e015")
        mvc.post("/api/auctionitems/$auctionItemId/bids") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(bidRequest)
        }
            .andDo { print() }
            .andExpect {
                status { isBadRequest() }
                content { string(Matchers.containsString("Auction item is expired")) }
            }
    }

    @Test
    fun `New bid request has invalid auction item id`() {
        val auctionItemId = "01951f4a-48ac-7c5f-8db1-1ef9efc5e10d-bad"
        val bidRequest = BidRequest(amount = 1, lastBidId = "")
        mvc.post("/api/auctionitems/$auctionItemId/bids") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(bidRequest)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `Users makes a bid with invalid data`() {
        val auctionItemId = "01951f4a-48ac-7c5f-8db1-1ef9efc5e10d"
        val bidRequest = BidRequest(amount = null, lastBidId = "invalid-uuid")
        println("PID REQUEST: " + objectMapper.writeValueAsString(bidRequest))
        mvc.post("/api/auctionitems/$auctionItemId/bids") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(bidRequest)
        }
            .andDo { print() }
            .andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `Get auction item`() {
        mvc.get("/api/auctionitems/d1d018fe-cc1b-4f9c-9d53-bc8f5dd9b515")
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content { string(Matchers.containsString(",\"currentPrice\":275")) }
            }
    }

    @Test
    fun `Auction item not found`() {
        mvc.get("/api/auctionitems/01951e70-fd92-78b3-92ab-d2f92e0586ba")
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `Get auction item with invalid id`() {
        mvc.get("/api/auctionitems/bad-uuid")
            .andDo { print() }
            .andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `Get the latest bid of auction item`() {
        mvc.get("/api/auctionitems/d1d018fe-cc1b-4f9c-9d53-bc8f5dd9b515/latestbid")
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content { string(Matchers.containsString(",\"currentPrice\":275")) }
                content { string(Matchers.containsString("\"lastBidId\":\"cf9e1c37-3647-4ad4-9539-23f592a32597\"")) }
            }
    }

    @Sql(statements = ["INSERT INTO auction_item (id, external_id, description, category, purchase_date, purchase_price, bidding_end_date, starting_price) VALUES ('8e61bd74-b109-4bac-8ad3-552e3d3451df','8c031a7a-6c3f-411c-85b6-35a97a61da6b','Apple MagSafe -laturi 25 W (1 m) (MX6X3)', 'Phone accessories', '2024-06-06', '49.99',  NOW() + interval '3' month, 12)"])
    @Test
    fun `Get the latest bid of auction item when there are no bids`() {
        mvc.get("/api/auctionitems/8e61bd74-b109-4bac-8ad3-552e3d3451df/latestbid")
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content { string(Matchers.containsString(",\"currentPrice\":12")) }
                content { string(Matchers.containsString("\"lastBidId\":\"\"")) }
            }
    }

    @Test
    fun `Get the latest bid for non-existing auction item`() {
        mvc.get("/api/auctionitems/01951e70-fd92-78b3-92ab-d2f92e0586ba/latestbid")
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `Get the latest bid with invalid auction item`() {
        mvc.get("/api/auctionitems/bad-uuid/latestbid")
            .andDo { print() }
            .andExpect {
                status { isBadRequest() }
            }
    }
}