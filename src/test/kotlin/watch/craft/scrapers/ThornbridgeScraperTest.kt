package watch.craft.scrapers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import watch.craft.Offer
import watch.craft.Scraper.Node.ScrapedItem
import watch.craft.byName
import watch.craft.executeScraper
import watch.craft.noDesc
import java.net.URI

class ThornbridgeScraperTest {
  companion object {
    private val ITEMS = executeScraper(ThornbridgeScraper())
  }

  @Test
  fun `finds all the beers`() {
    assertEquals(14, ITEMS.size)
  }

  @Test
  fun `extracts beer details`() {
    assertEquals(
      ScrapedItem(
        name = "Jaipur",    // No "can" or "bottle" in title
        summary = "IPA",
        abv = 5.9,
        offers = setOf(
          Offer(totalPrice = 2.00, sizeMl = 330)
        ),
        available = true,
        thumbnailUrl = URI("https://cdn.shopify.com/s/files/1/0075/9939/0831/products/Jaipur_can_mockup_coloured_background_200x.jpg")
      ),
      ITEMS.first { it.name == "Jaipur" && it.thumbnailUrl.toString().contains("can") }.noDesc()
    )
  }

  @Test
  fun `extracts description`() {
    assertNotNull(ITEMS.byName("Jaipur").desc)
  }

  @Test
  fun `ignores things that aren't beers`() {
    assertFalse(ITEMS.any { it.name.contains("glass", ignoreCase = true) })
  }
}

