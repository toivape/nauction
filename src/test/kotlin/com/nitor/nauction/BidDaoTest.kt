package com.nitor.nauction

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class BidDaoTest (@Autowired val dao: BidDao) {

    @Test
    fun `List bids for a specific auction item`() {
        dao.findByAuctionItemId("b030b21b-73f9-40ff-8518-4a45f2c9b769").apply {
            shouldHaveAtLeastSize(5)
            first().id shouldBe "75467def-b8cf-44dd-89a6-9fc0aa1a010f"
            last().id shouldBe "7f0c311d-2f02-4562-a5e6-254908568f8b"
        }
    }

    @Test
    fun `Auction item has no bids`() {
        dao.findByAuctionItemId("b6579e11-d0ef-4a21-a597-58961ddb801c").shouldBeEmpty()
    }

}