import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{BooleanType, NumericType}
import DataFrameImplicit._
import scala.jdk.CollectionConverters._
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}


object Main {
  private case class Config(inputPath: String = "", outputPath: String = "")

  private val parser = new scopt.OptionParser[Config]("DataPipeline") {
    head("DataPipeline", "1.0")

    opt[String]('i', "input").required().valueName("<path>").
      action((x, c) => c.copy(inputPath = x)).
      text("inputPath is the path to the input data")

    opt[String]('o', "output").required().valueName("<path>").
      action((x, c) => c.copy(outputPath = x)).
      text("outputPath is the path to save the output data")

    help("help").text("prints this usage text")
  }
  val spark: SparkSession = SparkSession.builder()
    .appName("Data Processing")
    .master("local[*]")
    .config("spark.hadoop.validateOutputSpecs", "false")
    .getOrCreate()

  def load(path: String): Array[String] = {
    try {
      val content = new String(Files.readAllBytes(Paths.get(path)))
      content.split(",")
    } catch {
      case e: Exception =>
        e.printStackTrace()
        Array.empty[String]
    }
  }

  def main(args: Array[String]): Unit = {
    parser.parse(args, Config()) match {
      case Some(config) =>
        // get raw data
        val dataProcessor = new ProcessingData()
        val equipmentMapping = load("src/data/map/equipment_type_array.csv")

        val lines = Files.readAllLines(Paths.get("src/data/map/value_sparse_attribute.txt"), StandardCharsets.UTF_8)
        val restoredIterable: Iterable[String] = lines.asScala
        val df = spark.read
          .option("header", "true")
          .option("inferSchema", "true")
          .csv(config.inputPath)
          .dropDuplicates().withColumn("id", monotonically_increasing_id())
          .withColumn("financial_prix_net_vendeur", col("financial_prix_net_vendeur").cast("double"))
          .withColumn("financial_prix_net_vendeur", dataProcessor.convertCurrency(col("financial_prix_net_vendeur"), col("id_ISO_4217_FinancialPrixNetVendeur")))
          .drop("id_ISO_4217_FinancialPrixNetVendeur")
          .withColumn("financial_realtor_fees", col("financial_realtor_fees").cast("double"))
          .withColumn("financial_realtor_fees", dataProcessor.convertCurrency(col("financial_realtor_fees"), col("id_ISO_4217_FinancialRealtorFees")))
          .drop("id_ISO_4217_FinancialRealtorFees")
          .withColumn("provided_total_price", col("provided_total_price").cast("double"))
          .withColumn("provided_total_price", dataProcessor.convertCurrency(col("provided_total_price"), col("id_ISO_4217_provided_total_price")))
          .drop("id_ISO_4217_provided_total_price")
          .withColumn("financial_seller_realtor_fees", col("financial_seller_realtor_fees").cast("double"))
          .withColumn("financial_seller_realtor_fees", dataProcessor.convertCurrency(col("financial_seller_realtor_fees"), col("id_ISO_4217_FinancialSellerRealtorFees")))
          .drop("id_ISO_4217_FinancialSellerRealtorFees")
          .withColumn("financial_buyer_realtor_fees", col("financial_buyer_realtor_fees").cast("double"))
          .withColumn("financial_buyer_realtor_fees", dataProcessor.convertCurrency(col("financial_buyer_realtor_fees"), col("id_ISO_4217_FinancialBuyerRealtorFees")))
          .drop("id_ISO_4217_FinancialBuyerRealtorFees")
          .withColumn("selling_price_excl", col("selling_price_excl").cast("double"))
          .withColumn("selling_price_excl", dataProcessor.convertCurrency(col("selling_price_excl"), col("id_ISO_4217_SellingPriceExcl")))
          .drop("id_ISO_4217_SellingPriceExcl")
          .withColumn("financial_key_money", col("financial_key_money").cast("double"))
          .withColumn("financial_key_money", dataProcessor.convertCurrency(col("financial_key_money"), col("id_ISO_4217_FinancialKeyMoney")))
          .drop("id_ISO_4217_FinancialKeyMoney")
          .withColumn("rent_range_high", col("rent_range_high").cast("double"))
          .withColumn("rent_range_high", dataProcessor.convertCurrency(col("rent_range_high"), col("id_ISO_4217_rent_range_high")))
          .drop("id_ISO_4217_rent_range_high")
          .processColumnIfPresent("response_value")
//          .withColumn("response", dataProcessor.convertResponseValueUDF(col("response_value")))
          .drop("response_value")
          .withColumn("total_price_min", when(col("total_price_min").equalTo(Double.NegativeInfinity), -1.0).otherwise(col("total_price_min")))
          .encodeString("id_ISO_3166_1")
          .encodeString("mandate_origin")
          .encodeAndReduce("bien_list_lois", Option(20))
          .encodeAndReduce("list_security", Option(12))
          .encodeAndReduce("list_proximities", Option(79))
          .encodeAndReduce("list_views", Option(15))
          .encodeAndReduce("list_equipment", None, Option(equipmentMapping))
          .replaceInfinityWithMax("surf_max")
          .replaceInfinityWithMax("surf_terrain_max")
          .replaceInfinityWithMax("right_to_the_lease_plus_key_money_max")
          .replaceInfinityWithMax("surf_rdc_max")
          .replaceInfinityWithMax("surf_vente_max")
          .withColumnRenamed("list_rooms", "room_list")
          .encodeAndReduce("room_list", Option(80))





        val dfNotNull = df.columns.foldLeft(df) {
          (curr, c) =>
            curr.schema(c).dataType match {
              case _: NumericType => curr.withColumn(c, when(col(c).isNull, -1).otherwise(col(c)))
              case _: BooleanType => curr.withColumn(c, when(col(c).isNull, -1).otherwise(col(c).cast("integer")))
              case _ => curr
            }
        }.applyPCA(restoredIterable.toList,5)


        dfNotNull.write.mode("overwrite").save(config.outputPath+"data")
      case None =>
        print("Les arguments n'étaient pas valides")
    }

  }
}
