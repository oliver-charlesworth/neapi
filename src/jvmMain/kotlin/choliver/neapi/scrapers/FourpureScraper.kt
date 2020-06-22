package choliver.neapi.scrapers

import choliver.neapi.ParsedItem
import choliver.neapi.Scraper
import choliver.neapi.Scraper.Context
import org.jsoup.nodes.Element
import java.net.URI

class FourpureScraper : Scraper {
  override val name = "Fourpure"

  override fun Context.scrape() = request(ROOT_URL)
    .select(".itemsBrowse li")
    .filterNot { el -> el.title().contains("pack", ignoreCase = true) }  // Can't figure out how to extract price-per-can from packs, so ignore
    .map { el ->
      val a = el.selectFirst("a")
      val thumbnailUrl = ROOT_URL.resolve(a.srcFrom("img"))
      val url = URI(a.hrefFrom())
      val itemDoc = request(url)
      val abv = itemDoc.extractFrom(".brewSheet", "Alcohol By Volume: (\\d+\\.\\d+)")!![1].toDouble()
      val unitPrice = (el.selectFirst(".priceNow") ?: el.selectFirst(".priceStandard")).priceFrom(".GBP")

      val title = el.title()
      if (title.contains("minikeg", ignoreCase = true)) {
        println(title)
        val parts = title.extract("([^\\d]+) (\\d+)L.*")!!
        ParsedItem(
          thumbnailUrl = thumbnailUrl,
          url = url,
          name = parts[1],
          abv = abv,
          summary = "Minikeg",
          sizeMl = parts[2].toInt() * 1000,
          available = true,
          unitPrice = unitPrice
        )
      } else {
        ParsedItem(
          thumbnailUrl = ROOT_URL.resolve(a.srcFrom("img")),
          url = url,
          name = el.title().extract("([^\\d]+)( \\d+ml)?")!![1],  // Strip size embedded in name
          abv = abv,
          summary = null,
          sizeMl = itemDoc.extractFrom(".quickBuy", "(\\d+)ml")!![1].toInt(),
          available = true,
          unitPrice = unitPrice
        )
      }
    }

  private fun Element.title() = textFrom("h3")

  companion object {
    private val ROOT_URL = URI("https://www.fourpure.com/browse/c-Our-Beers-5/")
  }
}