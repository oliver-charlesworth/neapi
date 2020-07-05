package watch.craft.scrapers

import org.jsoup.nodes.Document
import watch.craft.*
import watch.craft.Scraper.IndexEntry
import watch.craft.Scraper.ScrapedItem
import java.net.URI

class StewartScraper : Scraper {
  override val brewery = Brewery(
    shortName = "Stewart Brewing",
    name = "Stewart Brewing",
    location = "Loanhead, Midlothian",
    websiteUrl = URI("https://www.stewartbrewing.co.uk/")
  )
  override val rootUrls = listOf(URI("https://www.stewartbrewing.co.uk/browse/c-Build-Your-Own-12-Pack-37?view_all=true"))

  override fun scrapeIndex(root: Document) = root
    .selectMultipleFrom("#browse li .itemWrap")
    .map { el ->
      val a = el.selectFrom("h2 a")

      IndexEntry(a.text(), a.hrefFrom()) { doc ->
        val alco = doc.maybeSelectFrom(".alco")
        val volume = doc.maybeSelectFrom(".volume")

        if (alco == null || volume == null) {
          throw SkipItemException("Couldn't find ABV or volume")
        }

        ScrapedItem(
          thumbnailUrl = el.srcFrom(".imageInnerWrap img"),
          name = removeSizeSuffix(a.text()),
          summary = el.maybeTextFrom(".itemStyle"),
          abv = alco.extractFrom(regex = "(\\d+(\\.\\d+)?)%")[1].toDouble(),
          sizeMl = volume.extractFrom(regex = "(\\d+)ml")[1].toInt(),
          available = true,
          price = doc.extractFrom(".priceNow", "£(\\d+\\.\\d+)")[1].toDouble()
        )
      }
    }

  private fun removeSizeSuffix(str: String) = if (str.endsWith("ml")) {
    str.extract(regex = "^(.+?)( \\d+ml)")[1]
  } else {
    str
  }
}
