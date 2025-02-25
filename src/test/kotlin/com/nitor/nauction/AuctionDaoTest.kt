package com.nitor.nauction

import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
class AuctionDaoTest(@Autowired val dao: AuctionDao) {

    @Test
    fun `Open auction items list does not contain expired auction item`() {
        val expiredAuctionId = UUID.fromString("4c36b5ec-eebc-4881-8e18-edc9c84a0b49")
        dao.findAllOpen().apply {
            shouldHaveAtLeastSize(6)
            none { it.id == expiredAuctionId } shouldBe true
        }
    }

    @Test
    fun `Auctions ending soonest are shown first`() {
        dao.findAllOpen().apply {
            first().id shouldBe UUID.fromString("b030b21b-73f9-40ff-8518-4a45f2c9b769")
            last().id shouldBe UUID.fromString("ac63acfa-35bc-4ea4-aa2a-47470515596c")
        }
    }

    @Test
    fun `Auction item current price is starting price plus bids`(){
        dao.findById("b030b21b-73f9-40ff-8518-4a45f2c9b769").apply {
            shouldNotBeNull()
            currentPrice shouldBe 175
        }
    }

    @Test
    fun `Auction item current price is starting price when there are no bids`(){
       dao.findById("76bce495-219d-4632-a0bb-3e2977b7ae83").apply {
            shouldNotBeNull()
            currentPrice shouldBe startingPrice
        }
    }
}