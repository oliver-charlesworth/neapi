package watch.craft.scrapers

import watch.craft.Scraper

import watch.craft.Scraper.Node.ScrapedItem
import watch.craft.dsl.*
import watch.craft.shopify.extractShopifyOffers

class OrbitScraper : Scraper {
  override val roots = fromPaginatedRoots(ROOT) { root ->
    root()
      .selectMultipleFrom(".product-card")
      .map { el ->
        val title = el.textFrom(".product-card__title")

        fromHtml(title, el.urlFrom("a")) { doc ->
          val desc = doc().formattedTextFrom(".product-single__description")

          // Remove all the dross
          val name = title
            .cleanse(
              "NEW: ",
              "\\S+%",   // ABV
              "WLS\\d+"  // Some weird code
            ).split("-")[0].trim()

          ScrapedItem(
            name = name,
            summary = null,
            desc = desc,
            mixed = title.containsMatch("mixed"),
            abv = title.maybe { abvFrom() },
            available = ".price--sold-out" !in el,
            offers = doc().orSkip("Can't extract offers, so assume not a beer") {
              extractShopifyOffers(desc.maybe { sizeMlFrom() })
            },
            thumbnailUrl = el.urlFrom("noscript img")
          )
        }
      }
  }

  companion object {
    private val ROOT = root("https://orbitbeers.shop/collections/all")
  }
}
