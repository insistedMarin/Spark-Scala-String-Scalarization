import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.udf

import scala.jdk.CollectionConverters.asScalaSetConverter


class ProcessingData() {

  def nullValuePercentages(df:DataFrame): Map[String, Double] = {
    val totalRows = df.count()

    val nullCounts = df.columns.map { colName =>
      colName -> df.filter(df(colName).isNull || df(colName) === "").count()
    }.toMap

    nullCounts.map { case (colName, nullCount) =>
      colName -> (nullCount.toDouble / totalRows * 100)
    }
  }

  val convertCurrency: UserDefinedFunction = udf((amount: Double, currency: String) => {
    if (amount!= null && currency != null){
    val rateFetcher = ExchangeRateFetcher.rates.getJsonObject("quotes")
    val javaMap = rateFetcher.entrySet().asScala.map(entry => entry.getKey.replace("USD", "") -> entry.getValue.toString.toDouble).toMap
    val rateMap: Map[String, Double] = javaMap
    rateMap.get(currency) match {
      case Some(rate) => amount / rate
      case None => 0.0
    }}
    else
      0.0
  })

  val convertResponseValueUDF: UserDefinedFunction = udf((s: String) =>s match {
    case "interested" => 1
    case "not_interested" => 0
  })

}
