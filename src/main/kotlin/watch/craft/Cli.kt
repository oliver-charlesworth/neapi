package watch.craft

import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import watch.craft.storage.CachingGetter
import watch.craft.storage.HttpGetter
import watch.craft.scrapers.*
import watch.craft.storage.GcsObjectStore
import watch.craft.storage.LocalObjectStore
import watch.craft.storage.StoreStructure
import watch.craft.storage.WriteThroughObjectStore
import java.time.Instant

class Cli : CliktCommand(name = "scraper") {
  private val listScrapers by option("--list-scrapers", "-l").flag()
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
    val store = WriteThroughObjectStore(
      firstLevel = LocalObjectStore(CACHE_DIR),
      secondLevel = GcsObjectStore(GCS_BUCKET)
    )
    val structure = StoreStructure(store, Instant.now())
    val getter = CachingGetter(structure.cache, HttpGetter())
    val executor = Executor(getter)

    val inventory = executor.scrape(*scrapers.ifEmpty { SCRAPERS.values }.toTypedArray())
    structure.results.write("inventory.json", mapper.writeValueAsBytes(inventory))
    INVENTORY_JSON_FILE.outputStream().use { mapper.writeValue(it, inventory) }
    // TODO - log
  }

  companion object {
    private val SCRAPERS = listOf(
      BoxcarScraper(),
      CanopyScraper(),
      FivePointsScraper(),
      FourpureScraper(),
      GipsyHillScraper(),
      HowlingHopsScraper(),
      MarbleScraper(),
      PillarsScraper(),
      PressureDropScraper(),
      RedchurchScraper(),
      StewartScraper(),
      ThornbridgeScraper(),
      VillagesScraper()
    ).associateBy { it.name.toSafeName() }

    private fun String.toSafeName() = toLowerCase().replace("[^0-9a-z]".toRegex(), "-")
  }
}

fun main(args: Array<String>) = Cli().main(args)

