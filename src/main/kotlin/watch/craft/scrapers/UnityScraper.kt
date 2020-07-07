package watch.craft.scrapers

import watch.craft.*
import watch.craft.Scraper.Job.Leaf
import watch.craft.Scraper.ScrapedItem
import java.net.URI

class UnityScraper : Scraper {
  override val brewery = Brewery(
    shortName = "Unity",
    name = "Unity Brewing Co",
    location = "Southampton",
    websiteUrl = URI("https://unitybrewingco.com/")
  )

  override val jobs = forRootUrls(ROOT_URL) { root ->
    root
      .shopifyItems()
      .map { details ->

        Leaf(details.title, details.url) { doc ->
          val desc = doc.textFrom(".product-single__description")

          ScrapedItem(
            name = details.title,
            summary = null,
            desc = desc,
            mixed = false,
            sizeMl = desc.sizeMlFrom(),
            abv = desc.abvFrom(),
            available = details.available,
            numItems = 1,
            price = details.price,
            thumbnailUrl = details.thumbnailUrl
          )
        }
      }
  }

  companion object {
    private val ROOT_URL = URI("https://unitybrewingco.com/collections/unity-beer")
  }
}
