package com.nitor.nauction

import arrow.core.Either
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

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


@RestController
class ApiController(val dao: AuctionItemDao) {

    @PostMapping("/api/auctionitems")
    @ResponseStatus(HttpStatus.CREATED)
    fun createAuctionItem(@Valid @RequestBody item: NewAuctionItem): ResponseEntity<Any> {
        return when (val result = dao.create(item)) {
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
}