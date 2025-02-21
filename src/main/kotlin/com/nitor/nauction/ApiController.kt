package com.nitor.nauction

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class ErrorResponse(val error: String)

data class NewAuctionItem(
    @field:NotBlank(message = "id is mandatory")
    var id: String,
    @field:NotBlank(message = "description is mandatory")
    @field:Size(max = 2000, message = "description must be less than 500 characters")
    var description: String,
    @field:NotBlank(message = "category is mandatory")
    @field:Size(max = 2000, message = "category must be less than 50 characters")
    var category: String,
    @field:NotNull(message = "purchaseDate is mandatory")
    var purchaseDate: LocalDate,
    @field:NotNull(message = "purchasePrice is mandatory")
    var purchasePrice: BigDecimal,
    @field:NotNull(message = "biddingStartingPrice is mandatory")
    var startingPrice: BigDecimal
)

data class BidRequest(
    @field:NotNull(message = "Bid amount is mandatory")
    @field:Min(value = 1, message = "Bid amount must be at least 1")
    val amount: Int?,

    // Last bid id is empty if there are no earlier bids
    // Used to check that there are no concurrent bids
    @field:ValidUUID(message = "lastBidId is invalid")
    val lastBidId: String = ""
)

private val log = KotlinLogging.logger {}

@RestController
class ApiController(val auctionDao: AuctionDao, val bidService: BidService) {

    @PostMapping("/api/auctionitems")
    fun createAuctionItem(@Valid @RequestBody item: NewAuctionItem): ResponseEntity<Any> {
        return when (val result = auctionDao.addAuctionItem(item)) {
            is Either.Right -> ResponseEntity(Unit, HttpStatus.CREATED)
            is Either.Left -> {
                val errorMessage = result.value.message ?: "Unknown error"
                val status = if (errorMessage.contains("already exists")) {
                    HttpStatus.BAD_REQUEST
                } else {
                    HttpStatus.INTERNAL_SERVER_ERROR
                }
                ResponseEntity(ErrorResponse(errorMessage), status)
            }
        }
    }

    @PostMapping("/api/auctionitems/{auctionItemId}/bids")
    fun bid(@PathVariable @ValidUUID auctionItemId: String, @Valid @RequestBody bid: BidRequest, bindingResult: BindingResult/* TODO: user @AuthenticationPrincipal user: UserDetails */): ResponseEntity<Any> {
        val dummyUser = "bob.the.builder@nitor.com"
        log.info { "New bid $bid on auction item $auctionItemId by user $dummyUser" }

        if (bindingResult.hasErrors()) {
            val errorMessage = bindingResult.allErrors.joinToString { it.defaultMessage.orEmpty() }
            return ResponseEntity(ErrorResponse(errorMessage), HttpStatus.BAD_REQUEST)
        }

        return when (val result = bidService.addBid(auctionItemId, dummyUser, BigDecimal(bid.amount!!), bid.lastBidId)) {
            is Either.Right -> ResponseEntity(Unit, HttpStatus.CREATED)
            is Either.Left -> {
                val errorMessage = result.value.message ?: "Unknown error"
                ResponseEntity(ErrorResponse(errorMessage), HttpStatus.INTERNAL_SERVER_ERROR)
            }
        }
    }

    @GetMapping("/api/auctionitems/{id}")
    fun getAuctionItem(@PathVariable @ValidUUID id: String): ResponseEntity<AuctionItem> {
        return auctionDao.findById(id)?.let {
            ResponseEntity(it, HttpStatus.OK)
        } ?: ResponseEntity(HttpStatus.NOT_FOUND)
    }

    @GetMapping("/api/auctionitems/{id}/latestbid")
    fun getLastBid(@PathVariable @ValidUUID id: String): ResponseEntity<LastBid> {
        return bidService.getLastBid(id)?.let {
            log.info { "Latest bid for auction item $id: $it" }
            ResponseEntity(it, HttpStatus.OK)
        } ?: ResponseEntity(HttpStatus.NOT_FOUND)
    }
}