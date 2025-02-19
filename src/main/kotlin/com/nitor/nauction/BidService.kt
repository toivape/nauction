package com.nitor.nauction

import arrow.core.Either
import arrow.core.left
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class BidService(private val bidDao: BidDao, private val auctionDao: AuctionDao) {
    fun findByAuctionItemId(auctionItemId: String) = bidDao.findByAuctionItemId(auctionItemId)

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
}