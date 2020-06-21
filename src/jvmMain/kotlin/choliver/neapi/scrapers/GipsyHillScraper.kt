package choliver.neapi.scrapers

import choliver.neapi.ParsedItem
import choliver.neapi.Scraper
import choliver.neapi.Scraper.Context
import java.net.URI

class GipsyHillScraper : Scraper {
  override val name = "Gipsy Hill"

  override fun Context.scrape() = request(ROOT_URL)
    .select(".product")
    .map { el ->
      val a = el.selectFirst(".woocommerce-LoopProduct-link")
      val url = URI(a.attr("href").trim())
      val rawSummary = request(url).selectFirst(".summary").text()

      val parts = rawSummary.extract("Sold as: ((\\d+) x )?(\\d+)ml")
      val numCans = parts?.get(2)?.toIntOrNull() ?: 1

      ParsedItem(
        thumbnailUrl = URI(a.selectFirst(".attachment-woocommerce_thumbnail").attr("src").trim()),
        url = url,
        name = a.selectFirst(".woocommerce-loop-product__title").text().trim(),
        summary = rawSummary.extract("Style: (.*) ABV")?.get(1)?.trim(),
        available = true, // TODO
        abv = rawSummary.extract("ABV: (.*?)%")?.get(1)?.toBigDecimal(),
        sizeMl = parts?.get(3)?.toInt(),
        pricePerCan = el.selectFirst(".woocommerce-Price-amount").ownText().toBigDecimal() / numCans.toBigDecimal()
      )
    }
    .distinctBy { it.name }

  companion object {
    private val ROOT_URL = URI("https://gipsyhillbrew.com")
  }
}
