package choliver.neapi

import choliver.neapi.getters.HttpGetter
import choliver.neapi.getters.NewCachingGetter
import choliver.neapi.scrapers.*
import choliver.neapi.storage.GcsBacker
import choliver.neapi.storage.StorageThinger
import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import java.time.Instant

class Cli : CliktCommand(name = "scraper") {
  private val listScrapers by option("--list-scrapers", "-l").flag()
  private val withoutCache by option("--without-cache", "-w").flag()
  private val scrapers by argument().choice(SCRAPERS).multiple()

  private val mapper = jacksonObjectMapper().enable(INDENT_OUTPUT)

  override fun run() {
    when {
      listScrapers -> executeListScrapers()
      else -> executeScrape()
    }
  }

  private fun executeListScrapers() {
    SCRAPERS.keys.sorted().forEach(::println)
  }

  private fun executeScrape() {
//    val backer = LocalBacker(STORAGE_DIR)
    val backer = GcsBacker(GCS_BUCKET)
    val storage = StorageThinger(backer, Instant.now())
    val getter = NewCachingGetter(storage, HttpGetter())
    val executor = Executor(getter)

    val inventory = executor.scrape(*scrapers.ifEmpty { SCRAPERS.values }.toTypedArray())
    storage.writeResults(
      "inventory.json",
      mapper.writeValueAsBytes(inventory)
    )
  }

  companion object {
    private val SCRAPERS = listOf(
      BoxcarScraper(),
      CanopyScraper(),
      FivePointsScraper(),
      FourpureScraper(),
      GipsyHillScraper(),
      HowlingHopsScraper(),
      PillarsScraper(),
      PressureDropScraper(),
      StewartScraper(),
      VillagesScraper()
    ).associateBy { it.name.toSafeName() }

    private fun String.toSafeName() = toLowerCase().replace("[^0-9a-z]".toRegex(), "-")
  }
}

fun main(args: Array<String>) = Cli().main(args)
