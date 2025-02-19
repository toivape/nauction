package com.nitor.nauction

import arrow.core.Either
import arrow.core.left
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

data class LastBid (
    val auctionItemId: String,
    val lastBidId: String,
    val lastBidAmount: BigDecimal,
    val lastBidder: String,
    val itemDescription: String,
    val currentPrice: BigDecimal,
)

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
            return Exception("Last bid id is not the last bid id").left()
        }

        // Check that last bid id is still the last bid id
        if (existingBids.isNotEmpty() && lastBidId != existingBids.last().id){
            return Exception("Last bid id is not the last bid id").left()
        }

        return bidDao.addBid(auctionItemId, bidderEmail, bidAmount)
    }

    fun getLastBid(auctionItemId: String): LastBid? {
        val auctionItem = auctionDao.findById(auctionItemId) ?: return null
        val bids = bidDao.findByAuctionItemId(auctionItemId)
        val lastBid = bids.lastOrNull()
        if (lastBid == null){
            return LastBid(auctionItemId, "", auctionItem.startingPrice, "", auctionItem.description, auctionItem.currentPrice)
        }
        return LastBid(auctionItemId, lastBid.id, lastBid.bidPrice, lastBid.bidderEmail, auctionItem.description, auctionItem.currentPrice)
    }
}