package com.nitor.nauction

import arrow.core.Either
import arrow.core.left
import arrow.core.right
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

data class AuctionItem(
    val id: UUID = UUID.randomUUID(),
    val description: String,
    val category: String,
    val purchaseDate: LocalDate,
    val purchasePrice: BigDecimal,
    val biddingEndDate: LocalDate,
    val currentPrice: BigDecimal,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {

    companion object {
        fun toAuctionItem(rs: ResultSet) = AuctionItem(
            id = UUID.fromString(rs.getString("id")),
            description = rs.getString("description"),
            category = rs.getString("category"),
            purchaseDate = rs.getDate("purchase_date").toLocalDate(),
            purchasePrice = rs.getBigDecimal("purchase_price"),
            biddingEndDate = rs.getDate("bidding_end_date").toLocalDate(),
            currentPrice = rs.getBigDecimal("current_price"),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
            updatedAt = rs.getTimestamp("updated_at").toLocalDateTime()
        )
    }
}

private val log = KotlinLogging.logger {}

@Repository
class AuctionItemDao(val db: JdbcTemplate) {

    companion object {
        private val FIND_ALL_ACTIVE = """
            SELECT id, description, category, purchase_date, purchase_price, bidding_end_date, current_price, created_at, updated_at
            FROM auction_item
            WHERE bidding_end_date > CURRENT_DATE
            ORDER BY bidding_end_date DESC         
        """.trimIndent()

        private val FIND_BY_EXT_ID = """
            SELECT id, description, category, purchase_date, purchase_price, bidding_end_date, current_price, created_at, updated_at
            FROM auction_item
            WHERE external_id = ?
        """.trimIndent()

        private val INSERT_AUCTION = """
           INSERT INTO auction_item 
           (id, external_id, description, category, purchase_date, purchase_price, bidding_end_date, current_price, created_at, updated_at) 
           VALUES 
           (?, ?, ?, ?, ?, ?, NOW() + interval '3' month, ?, NOW(), NOW()) 
        """.trimIndent()
    }

    fun findAllActive() = db.query(FIND_ALL_ACTIVE) { rs, _ -> toAuctionItem(rs) }

    fun findByExternalId(id: String): AuctionItem? = runCatching {
        // Throws exception if no result is found
        db.queryForObject(FIND_BY_EXT_ID, { rs, _ -> toAuctionItem(rs) }, id)
    }.getOrNull()

    fun create(item: NewAuctionItem):Either<Exception, Unit> = try {
       db.update(
            INSERT_AUCTION,
            UUID.randomUUID(),
            item.id,
            item.description,
            item.category,
            item.purchaseDate,
            item.purchasePrice,
            item.biddingStartingPrice
        )
        Unit.right()
    } catch (e: DuplicateKeyException){
        val error = "Can't create auction item. Item already exists with external id ${item.id}."
        log.error(e) { error }
        Exception(error).left()
    }catch (e: DataAccessException){
        log.error(e) { "Failed to create auction item: $item" }
        Exception("Failed to add new auction item $item").left()
    }
}