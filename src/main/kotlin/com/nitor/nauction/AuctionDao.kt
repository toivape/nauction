package com.nitor.nauction

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.nitor.nauction.AdminItem.Companion.toAdminItem
import com.nitor.nauction.AuctionItem.Companion.toAuctionItem
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.dao.DataAccessException
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.sql.ResultSet
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun String.toUUID(): UUID = UUID.fromString(this)

data class AuctionItem(
    val id: UUID = UUID.randomUUID(),
    val externalId: String,
    val description: String,
    val category: String,
    val purchaseDate: LocalDate,
    val purchasePrice: BigDecimal,
    val biddingEndDate: LocalDate,
    val startingPrice: Int,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val totalBids: Int,
    val currentPrice: Int = startingPrice + totalBids
) {

    companion object {
        fun toAuctionItem(rs: ResultSet) = AuctionItem(
            id = UUID.fromString(rs.getString("id")),
            externalId = rs.getString("external_id"),
            description = rs.getString("description"),
            category = rs.getString("category"),
            purchaseDate = rs.getDate("purchase_date").toLocalDate(),
            purchasePrice = rs.getBigDecimal("purchase_price"),
            biddingEndDate = rs.getDate("bidding_end_date").toLocalDate(),
            startingPrice = rs.getInt("starting_price"),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
            updatedAt = rs.getTimestamp("updated_at").toLocalDateTime(),
            totalBids = rs.getInt("total_bids"),
        )
    }

    fun isExpired() = biddingEndDate.isBefore(LocalDate.now())
}

data class AdminItem(
    val description: String,
    val biddingEndDate: LocalDate,
    val timesRenewed: Int,
    val isTransferred: Boolean,
    val currentPrice: Int,
    val numberOfBids: Int
){
    companion object {
        fun toAdminItem(rs: ResultSet) = AdminItem(
            description = rs.getString("description"),
            biddingEndDate = rs.getDate("bidding_end_date").toLocalDate(),
            timesRenewed = rs.getInt("times_renewed"),
            isTransferred = rs.getBoolean("is_transferred"),
            currentPrice = rs.getInt("current_price"),
            numberOfBids = rs.getInt("number_of_bids")
        )
    }
}

private val log = KotlinLogging.logger {}

@Repository
class AuctionDao(val db: JdbcTemplate) {

    companion object {
        private val FIND_ALL_ADMIN = """
            SELECT            
                ai.description,
                ai.bidding_end_date,
                ai.times_renewed,
                ai.is_transferred,
                COALESCE(SUM(b.bid_price), 0) + ai.starting_price AS current_price,
                COUNT(b.id) AS number_of_bids
            FROM 
                auction_item ai
            LEFT JOIN 
                bid b ON ai.id = b.fk_auction_item_id
            GROUP BY 
                ai.description, ai.bidding_end_date, ai.times_renewed, ai.is_transferred
            ORDER BY 
                ai.bidding_end_date ASC
        """.trimIndent()

        private val FIND_ALL_ACTIVE = """
            SELECT 
                ai.id,
                ai.external_id,
                ai.description,
                ai.category,
                ai.purchase_date,
                ai.purchase_price,
                ai.bidding_end_date,
                ai.starting_price,
                ai.created_at,
                ai.updated_at,
                COALESCE(SUM(b.bid_price), 0) AS total_bids
            FROM 
                auction_item ai
            LEFT JOIN 
                bid b ON ai.id = b.fk_auction_item_id
            WHERE 
                ai.bidding_end_date > CURRENT_DATE
            GROUP BY 
                ai.id, ai.external_id, ai.description, ai.category, ai.purchase_date, ai.purchase_price, ai.bidding_end_date, ai.created_at, ai.updated_at
            ORDER BY 
                ai.bidding_end_date ASC         
        """.trimIndent()


        private val INSERT_AUCTION = """
           INSERT INTO auction_item 
               (id, external_id, description, category, purchase_date, purchase_price, bidding_end_date, starting_price, created_at, updated_at) 
           VALUES 
               (?, ?, ?, ?, ?, ?, NOW() + interval '3' month, ?, NOW(), NOW()) 
        """.trimIndent()

        private val GET_ITEM = """
            SELECT 
                ai.id,
                ai.external_id,
                ai.description,
                ai.category,
                ai.purchase_date,
                ai.purchase_price,
                ai.bidding_end_date,
                ai.starting_price,
                ai.created_at,
                ai.updated_at,
                COALESCE(SUM(b.bid_price), 0) AS total_bids
            FROM 
                auction_item ai
            LEFT JOIN 
                bid b ON ai.id = b.fk_auction_item_id
            WHERE 
                ai.id = ?
            GROUP BY 
                ai.id, ai.external_id, ai.description, ai.category, ai.purchase_date, ai.purchase_price, ai.bidding_end_date, ai.created_at, ai.updated_at
        """.trimIndent()

    }

    fun findAllOpen() = db.query(FIND_ALL_ACTIVE) { rs, _ -> toAuctionItem(rs) }

    fun findAllAdmin() = db.query(FIND_ALL_ADMIN) { rs, _ -> toAdminItem(rs) }

    fun findById(id: String): AuctionItem? = runCatching {
        db.queryForObject(GET_ITEM, { rs, _ -> toAuctionItem(rs) }, id.toUUID())
    }.getOrNull()

    fun addAuctionItem(item: NewAuctionItem): Either<Exception, Unit> = try {
        val id = UUID.randomUUID()
        db.update(
            INSERT_AUCTION,
            id,
            item.id,
            item.description,
            item.category,
            item.purchaseDate,
            item.purchasePrice,
            item.startingPrice
        )

        log.info { "Created auction item ($id): $item" }

        Unit.right()
    } catch (e: DuplicateKeyException) {
        val error = "Can't create auction item. Item already exists with external id ${item.id}."
        log.error(e) { error }
        Exception(error).left()
    } catch (e: DataAccessException) {
        log.error(e) { "Failed to create auction item: $item" }
        Exception("Failed to add new auction item $item").left()
    }

}