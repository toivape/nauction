package com.nitor.nauction

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

data class LatestBid(
    val auctionItemId: String,
    val lastBidId: String,
    val lastBidAmount: Int?,
    val lastBidder: String,
    val itemDescription: String,
    val currentPrice: Int,
)

class ConcurrentBidException(message: String) : Exception(message)

class ExpiredException(message: String) : Exception(message)

private val log = KotlinLogging.logger {}

@Service
class BidService(private val bidDao: BidDao, private val auctionDao: AuctionDao) {

    @Transactional
    fun addBid(
        auctionItemId: String,
        bidderEmail: String,
        amount: Int,
        lastBidId: String
    ): Either<Exception, LatestBid> {
        // Auction item must exist
        val auctionItem = auctionDao.findById(auctionItemId)
        if (auctionItem == null) {
            return Exception("Auction item not found").left()
        }

        // Auction item must be open (not expired)
        if (auctionItem.isExpired()) {
            log.warn { "User $bidderEmail tried to place bid for expired auction item $auctionItemId" }
            return ExpiredException("Auction item is expired").left()
        }

        // If this is first bid then price is starting price of auction item
        val existingBids = bidDao.findByAuctionItemId(auctionItemId)
        val bidAmount = if (existingBids.isEmpty()) {
            auctionItem.startingPrice
        } else {
            amount
        }

        // If last bid is empty then existing bid list must be empty
        if (existingBids.isNotEmpty() && lastBidId.isEmpty()) {
            return ConcurrentBidException("This is no longer the first bid").left()
        }

        // Check that last bid id is still the last bid id
        if (existingBids.isNotEmpty() && lastBidId != existingBids.last().id) {
            return ConcurrentBidException("Other user has made a simultaneous bid").left()
        }

        bidDao.addBid(auctionItemId, bidderEmail, bidAmount)

        val latestBid = getLatestBid(auctionItemId)
        return latestBid?.right() ?: Exception("Failed to get latest bid").left()
    }

    fun getLatestBid(auctionItemId: String): LatestBid? {
        val auctionItem = auctionDao.findById(auctionItemId) ?: return null
        val lastBid = bidDao.findByAuctionItemId(auctionItemId).lastOrNull()
        return LatestBid(
            auctionItemId,
            lastBid?.id ?: "",
            lastBid?.bidPrice,
            lastBid?.bidderEmail ?: "",
            auctionItem.description,
            auctionItem.currentPrice
        )
    }

    fun renewExpiredAuctions() {
        val numUpdated = bidDao.renewExpiredAuctions()
        log.info { "Number of renewed auctions: $numUpdated" }
    }

    fun exportFinishedAuctions() {
        log.info { "Exporting finished auctions" }
        // TODO Find auctions which are expired and have bids, export them to originating system, and mark them exported by settings is_exported = true
        // TODO Export should go to a separate class which is responsible for exporting
    }
}