package com.nitor.nauction

import arrow.core.Either
import arrow.core.left
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

data class LastBid (
    val auctionItemId: String,
    val lastBidId: String,
    val lastBidAmount: BigDecimal?,
    val lastBidder: String,
    val itemDescription: String,
    val currentPrice: BigDecimal,
)

class ConcurrentBidException(message: String) : Exception(message)

@Service
class BidService(private val bidDao: BidDao, private val auctionDao: AuctionDao) {

    @Transactional
    fun addBid(auctionItemId: String, bidderEmail: String, amount: BigDecimal, lastBidId: String): Either<Exception, Unit> {
        // Auction item must exist
        val auctionItem = auctionDao.findById(auctionItemId)
        if (auctionItem == null){
            return Exception("Auction item not found").left()
        }

        // If this is first bid then price is starting price of auction item
        val existingBids = bidDao.findByAuctionItemId(auctionItemId)
        val bidAmount = if (existingBids.isEmpty()){
            auctionItem.startingPrice
        } else {
            amount
        }

        // If last bid is empty then existing bid list must be empty
        if (existingBids.isNotEmpty() && lastBidId.isEmpty()){
            return ConcurrentBidException("This is no longer the first bid").left()
        }

        // Check that last bid id is still the last bid id
        if (existingBids.isNotEmpty() && lastBidId != existingBids.last().id){
            return ConcurrentBidException("Other user has made a simultaneous bid").left()
        }

        return bidDao.addBid(auctionItemId, bidderEmail, bidAmount)
    }

    fun getLastBid(auctionItemId: String): LastBid? {
        val auctionItem = auctionDao.findById(auctionItemId) ?: return null
        val lastBid = bidDao.findByAuctionItemId(auctionItemId).lastOrNull()
        return LastBid(
            auctionItemId,
            lastBid?.id ?: "",
            lastBid?.bidPrice, // ?: auctionItem.startingPrice,
            lastBid?.bidderEmail ?: "",
            auctionItem.description,
            auctionItem.currentPrice
        )
    }
}