package com.nitor.nauction

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.nitor.nauction.Bid.Companion.toBid
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.*

data class Bid(
    val id: String,
    val auctionItemId: String,
    val bidPrice: BigDecimal,
    val bidderEmail: String,
    val bidTime: LocalDateTime
) {
    companion object {
        fun toBid(rs: ResultSet) = Bid(
            id = rs.getString("id"),
            auctionItemId = rs.getString("fk_auction_item_id"),
            bidPrice = rs.getBigDecimal("bid_price"),
            bidderEmail = rs.getString("bidder_email"),
            bidTime = rs.getTimestamp("bid_time").toLocalDateTime()
        )
    }
}

private val log = KotlinLogging.logger {}

@Repository
class BidDao(val db: JdbcTemplate) {
    companion object {

        private val FIND_BIDS = """
            SELECT 
                id,
                fk_auction_item_id,
                bid_price,
                bidder_email,
                bid_time
            FROM 
                bid
            WHERE 
                fk_auction_item_id = ?
            ORDER BY 
                bid_time ASC         
        """.trimIndent()

        private val CREATE_BID = """
            INSERT INTO bid 
            (id, fk_auction_item_id, bid_price, bidder_email, bid_time) 
            VALUES 
            (?, ?, ?, ?, CURRENT_TIMESTAMP)
        """.trimIndent()
    }

    fun findByAuctionItemId(auctionItemId: String): List<Bid> =
        db.query(FIND_BIDS, auctionItemId) { rs, _ -> toBid(rs) }

    fun addBid(auctionItemId: String, bidderEmail: String, amount: BigDecimal): Either<Exception, Unit> = try {
        val id = UUID.randomUUID()
        db.update(
            CREATE_BID,
            id,
            auctionItemId,
            amount,
            bidderEmail
        )

        log.info { "Created bid ($id): amount: $amount, bidder: $bidderEmail, auctionItem: $auctionItemId" }

        Unit.right()
    } catch (e: DataAccessException) {
        log.error(e) { "Failed to create bid: amount: $amount, bidder: $bidderEmail, auctionItem: $auctionItemId\"" }
        Exception("Failed to add new auction bid").left()
    }
}