import org.apache.spark.sql.{DataFrame, SparkSession}

class ETLProcessSpec extends UnitSpec {

  "ETLProcess" should "produce output without null or non-numeric values" in {
    // Create a SparkSession for testing
    val spark: SparkSession = SparkSession.builder()
      .appName("Test")
      .master("local[*]")
      .getOrCreate()

    // Prepare the paths
    val inputPath = "src/data/test_data.csv"
    val outputPath = "src/data/output/test_"

    // Create an instance of ETLProcess
    val etlProcess = new ETLProcess(spark)

    // Run the ETL process
    val outputDataFrame: DataFrame= etlProcess.run(inputPath, outputPath)

    val columnNames = outputDataFrame.columns
    outputDataFrame.collect().foreach { row =>
      row.toSeq.zip(columnNames).foreach { case (cell, colName) =>
        assert(cell != null, s"Null value found in column $colName")
        cell shouldBe a[Number]
      }
    }
  }


}