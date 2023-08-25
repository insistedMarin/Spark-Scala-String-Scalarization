import java.io._
import java.net.{HttpURLConnection, URL}
import javax.json._
import java.time._
import scala.io.Source

object ExchangeRateFetcher {

  private val API_URL = "http://apilayer.net/api/live?access_key=71cb6d1a396d611c1f5296e77ef6c60b&currencies=MUR,XPF,EUR&source=USD&format=1"
  private val JSON_FILE_PATH = "src/data/rates.json"
  lazy val rates: JsonObject = getRates()
  private def getRates(): JsonObject = {
    val jsonFile = new File(JSON_FILE_PATH)
    var rates: JsonObject = null

    if (jsonFile.exists()) {
      val reader = new FileReader(jsonFile)
      val jsonReader = Json.createReader(reader)
      rates = jsonReader.readObject()
      jsonReader.close()

      val timestamp = rates.getJsonNumber("timestamp").longValue()
      val past = Instant.ofEpochSecond(timestamp)
      val duration = Duration.between(past, Instant.now())

      if (duration.toDays() >= 1) {
        rates = updateRates()
        saveRates(rates)
      }
    } else {
      rates = updateRates()
      saveRates(rates)
    }

    rates
  }

  private def saveRates(rates: JsonObject): Unit = {
    val writer = new FileWriter(JSON_FILE_PATH)
    val jsonWriter = Json.createWriter(writer)
    jsonWriter.writeObject(rates)
    jsonWriter.close()
  }

  private def updateRates(): JsonObject = {
    val url = new URL(API_URL)
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")

    val reader = Source.fromInputStream(connection.getInputStream).bufferedReader()
    val jsonReader = Json.createReader(reader)
    val rates = jsonReader.readObject()
    jsonReader.close()

    val writer = new FileWriter(JSON_FILE_PATH)
    val jsonWriter = Json.createWriter(writer)
    jsonWriter.writeObject(rates)
    jsonWriter.close()

    rates
  }
}

