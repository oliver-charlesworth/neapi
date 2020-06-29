package watch.craft.scrapers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import watch.craft.Item
import watch.craft.byName
import watch.craft.executeScraper
import watch.craft.noDesc
import java.net.URI

class BoxcarScraperTest {
  companion object {
    private val ITEMS = executeScraper(BoxcarScraper())
  }

  @Test
  fun `finds all the beers`() {
    assertEquals(8, ITEMS.size)
  }

  @Test
  fun `extracts available beers`() {
    assertEquals(
      Item(
        brewery = "Boxcar",
        name = "Dreamful",
        summary = "IPA",
        sizeMl = 440,
        abv = 6.5,
        perItemPrice = 4.95,
        available = true,
        url = "https://shop.boxcarbrewery.co.uk/collections/beer/products/dreamful-6-5-ipa-440ml",
        thumbnailUrl = "https://cdn.shopify.com/s/files/1/0358/6742/6953/products/IMG-20200604-WA0003_345x345.jpg"
      ),
      ITEMS.byName("Dreamful").noDesc()
    )
  }

  @Test
  fun `extracts unavailable beers`() {
    assertEquals(
      Item(
        brewery = "Boxcar",
        name = "Dark Mild",
        sizeMl = 440,
        abv = 3.6,
        perItemPrice = 3.75,
        available = false,
        url = "https://shop.boxcarbrewery.co.uk/collections/beer/products/dark-mild",
        thumbnailUrl = "https://cdn.shopify.com/s/files/1/0358/6742/6953/products/20200429_183043_345x345.jpg"
      ),
      ITEMS.byName("Dark Mild").noDesc()
    )
  }

  @Test
  fun `extracts description`() {
    assertNotNull(ITEMS.byName("Dreamful").desc)
  }
}

