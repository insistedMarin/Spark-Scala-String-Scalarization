import org.apache.spark.sql.SparkSession



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

  def main(args: Array[String]): Unit = {
    parser.parse(args, Config()) match {
      case Some(config) =>

        val job = new ETLProcess(spark)

        job.run(config.inputPath,config.outputPath)

        spark.close()

      case None =>
        print("Les arguments n'étaient pas valides")
    }

  }
}
