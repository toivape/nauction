package com.nitor.nauction

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
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
            value.message.shouldBe("This is no longer the first bid")
        }
    }

    @Test
    fun `should return error when last bid id is not the last bid id`() {
        val auctionItemId = "b030b21b-73f9-40ff-8518-4a45f2c9b769"
        val lastBidId = "75467def-b8cf-44dd-89a6-9fc0aa1a010f"
        bidService.addBid(auctionItemId, "test@example.com", BigDecimal.TEN, lastBidId).apply {
            shouldBeLeft()
            value.message.shouldBe("Other user has made a simultaneous bid")
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

    @Test
    fun `should return null when auction item not found`() {
        bidService.getLastBid("non-existent-id").shouldBeNull()
    }

    @Test
    fun `should return last bid details when bids exist`() {
        val auctionItemId = "b030b21b-73f9-40ff-8518-4a45f2c9b769"
        bidService.getLastBid(auctionItemId).apply {
            shouldNotBeNull()
            auctionItemId shouldBe auctionItemId
            lastBidId shouldBe "7f0c311d-2f02-4562-a5e6-254908568f8b"
            lastBidAmount shouldBe BigDecimal("5.00")
            itemDescription shouldBe "Apple iPhone 15 Pro Max 512 Gt -puhelin, sinititaani (MU7F3)"
            currentPrice shouldBe BigDecimal("175.00")
        }
    }

    @Test
    fun `should return only auction item details if there are no bids`() {
        val auctionItemId = "76bce495-219d-4632-a0bb-3e2977b7ae83"
        bidService.getLastBid(auctionItemId).apply {
            shouldNotBeNull()
            auctionItemId shouldBe auctionItemId
            lastBidId shouldBe ""
            lastBidAmount shouldBe null
            itemDescription shouldBe "Apple 96 W USB-C-virtalähde (MX0J2)"
            currentPrice shouldBe BigDecimal("7.00")
        }
    }
}