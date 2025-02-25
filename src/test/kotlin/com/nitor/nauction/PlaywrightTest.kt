package com.nitor.nauction

import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.WaitForSelectorState
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort

@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlaywrightTest(@Autowired val bidService: BidService) {

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

    @Test
    fun `Playwright can open front page`() {
        playwright!!.chromium().launch().use { browser ->
            val page = browser.newPage()
            page.navigate("$baseUrl/")
            //page.screenshot(Page.ScreenshotOptions().setPath(Paths.get("example.png")))
            page.title() shouldBe "Nitor Auction"
        }
    }

    @Test
    fun `Open modal window to make a bid`() {
        val auctionItemId = "b030b21b-73f9-40ff-8518-4a45f2c9b769"
        playwright!!.chromium().launch().use { browser ->
            val page = browser.newPage()
            page.navigate("$baseUrl/")

            val button = page.locator("[data-bs-itemid='$auctionItemId']")
            button.click()

            // Wait for the modal to be visible
            page.waitForSelector(".modal.show")

            // Verify that auctionItemId and latestBidId are set correctly
            val auctionItemIdTag = page.locator("#item-id")
            auctionItemIdTag.getAttribute("value").shouldBe(auctionItemId)

            val latestBid = bidService.getLatestBid(auctionItemId)!!
            val latestBidIdTag = page.locator("#last-bid-id")
            latestBidIdTag.getAttribute("value").shouldBe(latestBid.lastBidId)
        }
    }

    @Test
    fun `Make a bid`() {
        playwright!!.chromium().launch().use { browser ->
            val page = browser.newPage()
            page.navigate("$baseUrl/")

            // Locate and click the button to open the modal
            val button = page.locator("[data-bs-itemid='b030b21b-73f9-40ff-8518-4a45f2c9b769']")
            button.click()

            // Wait for the modal to be visible
            page.waitForSelector("#myBidModal.show")

            // Set the bid amount
            val bidAmountInput = page.locator("#bid-amount")
            bidAmountInput.fill("42")

            // Click the "Bid" button
            val submitBidButton = page.locator("#submit-bid")
            submitBidButton.click()

            // Wait for the modal to be hidden
            page.waitForSelector("#myBidModal", Page.WaitForSelectorOptions().setState(WaitForSelectorState.HIDDEN))

            // Check that bid is found in database
            val bid = bidService.getLatestBid("b030b21b-73f9-40ff-8518-4a45f2c9b769")!!
            bid.lastBidAmount shouldBe "42.00".toBigDecimal()
        }

        @Test
        fun `Attempt to make bid without bid amount shows error to user`(){

        }

        @Test
        fun `User makes simultaneous bid and gets error`(){

        }

        @Test
        fun `After successful bid auction item is updated with current price and latest bid`(){

        }
    }

}