package watch.craft.scrapers

import watch.craft.Offer
import watch.craft.Scraper

import watch.craft.Scraper.Node.ScrapedItem
import watch.craft.dsl.*
import java.net.URI

class WanderScraper : Scraper {
  override val roots = fromHtmlRoots(ROOT) { root ->
    root()
      .selectFrom("product-list-wrapper".hook())  // Only first one, to avoid merch, etc.
      .selectMultipleFrom("product-list-grid-item".hook())
      .map { el ->
        val name = el.textFrom("product-item-name".hook())

        fromHtml(name, el.urlFrom("a")) { doc ->
          val desc = doc().selectFrom("description".hook())
          val descText = desc.text()

          val mixed = name.containsMatch("mixed")

          ScrapedItem(
            name = name,
            summary = desc.maybe {
              extractFrom(
                "p",
                ".+[ \u00A0]-[ \u00A0](.+)"
              )[1]
            },  // Grotesque heterogeneous space characters
            desc = desc.formattedTextFrom(),
            mixed = mixed,
            abv = descText.abvFrom(),
            available = true,
            offers = setOf(
              Offer(
                quantity = descText.maybe { quantityFrom() } ?: 1,
                totalPrice = doc().priceFrom("formatted-primary-price".hook()),
                sizeMl = descText.sizeMlFrom()
              )
            ),
            thumbnailUrl = URI(
              el.attrFrom("product-item-images".hook(), "style")
                .extract("background-image:url\\((.*?)\\)")[1]
            )
          )
        }
      }
  }

  private fun String.hook() = "[data-hook=${this}]"

  companion object {
    private val ROOT = root("https://www.wanderbeyondbrewing.com/shop")
  }
}
