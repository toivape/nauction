package com.nitor.nauction

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal

@SpringBootTest
@ActiveProfiles("test")
class BidServiceTest {

    @Autowired
    private lateinit var bidService: BidService

    @Autowired
    private lateinit var bidDao: BidDao

    @Autowired
    private lateinit var auctionDao: AuctionDao

    @Test
    fun `should return error when auction item not found`() {
        bidService.addBid("non-existent-id", "test@example.com", BigDecimal.TEN, "").apply {
            shouldBeLeft()
            value.message.shouldBe("Auction item not found")
        }
    }

    @Test
    fun `should add first bid with starting price`() {
        val auctionItemId = "76bce495-219d-4632-a0bb-3e2977b7ae83"
        val auctionItem = auctionDao.findById(auctionItemId)
        bidService.addBid(auctionItemId, "test@example.com", BigDecimal.TEN, "").shouldBeRight()
        bidDao.findByAuctionItemId(auctionItemId).apply {
            shouldHaveAtLeastSize(1)
            first().bidPrice.shouldBe(auctionItem!!.startingPrice)
        }
    }

    @Test
    fun `should return error when last bid id is empty but bids exist`() {
        val auctionItemId = "b030b21b-73f9-40ff-8518-4a45f2c9b769" // Ensure this ID exists in your test data
        bidService.addBid(auctionItemId, "test@example.com", BigDecimal.TEN, "").apply {
            shouldBeLeft()
            value.message.shouldBe("Last bid id is not the last bid id")
        }
    }

    @Test
    fun `should return error when last bid id is not the last bid id`() {
        val auctionItemId = "b030b21b-73f9-40ff-8518-4a45f2c9b769"
        val lastBidId = "75467def-b8cf-44dd-89a6-9fc0aa1a010f"
        bidService.addBid(auctionItemId, "test@example.com", BigDecimal.TEN, lastBidId).apply {
            shouldBeLeft()
            value.message.shouldBe("Last bid id is not the last bid id")
        }
    }

    @Test
    fun `should add bid successfully`() {
        val auctionItemId = "b030b21b-73f9-40ff-8518-4a45f2c9b769"
        val lastBidId = "7f0c311d-2f02-4562-a5e6-254908568f8b"
        bidService.addBid(auctionItemId, "test@example.com", BigDecimal.TEN, lastBidId).shouldBeRight()
        bidDao.findByAuctionItemId(auctionItemId).apply {
            shouldHaveAtLeastSize(6)
            last().bidPrice.shouldBe(BigDecimal("10.00"))
        }
    }
}