package watch.craft.scrapers

import org.jsoup.nodes.Document
import watch.craft.*
import watch.craft.Scraper.IndexEntry
import watch.craft.Scraper.ScrapedItem
import java.net.URI

class WanderScraper : Scraper {
  override val name = "Wander Beyond"
  override val rootUrls = listOf(URI("https://www.wanderbeyondbrewing.com/shop"))

  override fun scrapeIndex(root: Document) = root
    .selectFrom("product-list-wrapper".hook())  // Only first one, to avoid merch, etc.
    .selectMultipleFrom("product-list-grid-item".hook())
    .map { el ->
      val name = el.textFrom("product-item-name".hook())

      IndexEntry(name, el.hrefFrom("a")) { doc ->
        val desc = doc.selectFrom("description".hook())
        val descText = desc.text()

        val mixed = name.contains("mixed", ignoreCase = true)

        ScrapedItem(
          name = name,
          summary = desc.maybeExtractFrom("p", ".+[ \u00A0]-[ \u00A0](.+)")?.get(1),  // Grotesque heterogeneous space characters
          desc = desc.normaliseParagraphsFrom(),
          mixed = mixed,
          sizeMl = descText.extract("(\\d+)ml")[1].toInt(),
          abv = descText.extract("(\\d+(\\.\\d+)?)%")[1].toDouble(),
          available = true,
          numItems = descText.maybeExtract("(\\d+)x")?.get(1)?.toIntOrNull() ?: 1,
          price = doc.priceFrom("product-price-wrapper".hook()),
          thumbnailUrl = URI(
            el.attrFrom("product-item-images".hook(), "style")
              .extract("background-image:url\\((.*?)\\)")[1]
          )
        )
      }
    }

  private fun String.hook() = "[data-hook=${this}]"
}
