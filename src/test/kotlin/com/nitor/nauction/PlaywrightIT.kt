package com.nitor.nauction

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.Page.WaitForSelectorOptions
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.options.WaitForSelectorState
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class PlaywrightIT(@Autowired val bidService: BidService) {

    companion object {
        private const val AUCTION_ITEM_ID = "b030b21b-73f9-40ff-8518-4a45f2c9b769"
    }

    @LocalServerPort
    private var port: Int = 0

    private val baseUrl: String
        get() = "http://localhost:$port"

    private var playwright: Playwright? = null

    @BeforeAll
    fun setUp() {
        playwright = Playwright.create()
    }

    @AfterAll
    fun tearDown() {
        playwright?.close()
    }

    fun Browser.frontpage(): Page {
        val context = this.newContext()
        context.setDefaultTimeout(1000.0) // Set default timeout to 1 second
        val page = context.newPage()
        page.navigate("$baseUrl/")
        return page
    }

    fun Browser.openAuctionItemModal(): Page {
        val page = frontpage()

        // Click Bid button to open auction item in modal
        page.locator("[data-bs-itemid='$AUCTION_ITEM_ID']").click()
        page.waitForSelector(".modal.show")
        return page
    }

    fun Page.placeBid(amount: String) {
        locator("#bid-amount").fill(amount)
        locator("#submit-bid").click()
    }

    @Test
    fun `Playwright can open front page`() {
        playwright!!.chromium().launch().use { browser ->
            val page = browser.frontpage()
            //page.screenshot(Page.ScreenshotOptions().setPath(Paths.get("example.png")))
            page.title() shouldBe "Nitor Auction"
        }
    }

    @Test
    fun `Open auction item window with latest bid data`() {
        playwright!!.chromium().launch().use { browser ->
            val page = browser.openAuctionItemModal()

            // Verify that auctionItemId and latestBidId are set correctly
            val auctionItemIdTag = page.locator("#item-id")
            auctionItemIdTag.getAttribute("value").shouldBe(AUCTION_ITEM_ID)

            val latestBid = bidService.getLatestBid(AUCTION_ITEM_ID)!!
            val latestBidIdTag = page.locator("#last-bid-id")
            latestBidIdTag.getAttribute("value").shouldBe(latestBid.lastBidId)
        }
    }

    @Test
    fun `Make a bid`() {
        playwright!!.chromium().launch().use { browser ->
            val page = browser.openAuctionItemModal()
            val bidAmount = 42
            page.placeBid(bidAmount.toString())

            // Wait for the modal to be hidden
            page.waitForSelector("#myBidModal", WaitForSelectorOptions().setState(WaitForSelectorState.HIDDEN))

            // Check that bid is found in database
            val bid = bidService.getLatestBid(AUCTION_ITEM_ID)!!
            bid.lastBidAmount shouldBe bidAmount
        }
    }

    @Test
    fun `User makes simultaneous bid and gets error`() {
        playwright!!.chromium().launch().use { browser ->
            val page = browser.openAuctionItemModal()

            // Make a simultaneous bid
            val latestBid = bidService.getLatestBid(AUCTION_ITEM_ID)
            bidService.addBid(AUCTION_ITEM_ID, "test.bidder.bob@nitor.com", 10, latestBid!!.lastBidId).apply {
                shouldBeRight()
            }

            // Make bid with outdated lastBidId
            page.placeBid("42")

            // Check if the alert box contains the correct error message
            page.waitForSelector("#error-alert:visible")
            val alertText = page.locator("#error-alert").textContent()
            alertText shouldBe "Other user has placed a bid"
        }
    }

    @Test
    fun `Attempt to make bid without bid amount shows error to user`() {
        playwright!!.chromium().launch().use { browser ->
            val page = browser.openAuctionItemModal()

            // Make bid with empty amount
            page.placeBid("")

            //Check if the invalid-feedback contains the correct error message
            page.waitForSelector(".invalid-feedback:visible")
            val errorMessage = page.locator(".invalid-feedback").textContent()
            errorMessage shouldContain "Please enter a valid bid amount"
        }
    }

    @Test
    fun `After successful bid auction item is updated with current price and latest bid`() {
        playwright!!.chromium().launch().use { browser ->
            val page = browser.openAuctionItemModal()

            val latestBid = bidService.getLatestBid(AUCTION_ITEM_ID)!!
            val bidAmount = 11
            val expectedPrice = latestBid.currentPrice + bidAmount

            page.placeBid(bidAmount.toString())

            // Wait for the modal to be hidden
            page.waitForSelector("#myBidModal", WaitForSelectorOptions().setState(WaitForSelectorState.HIDDEN))

            // Wait for the page to reload and finish loading
            page.waitForLoadState(LoadState.LOAD)

            // Check that Current Price is updated to expectedPrice
            val currentPriceText = page.locator("#item_${AUCTION_ITEM_ID} .card-text").textContent()
            currentPriceText shouldContain "$expectedPrice€"
        }
    }

}