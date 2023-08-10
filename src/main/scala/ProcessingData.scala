import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.{PCA, StringIndexer, StringIndexerModel, VectorAssembler}
import org.apache.spark.sql.DataFrame
import org.apache.spark.ml.linalg.DenseVector

import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.{col, expr, udf}
import org.apache.spark.ml.{Pipeline, PipelineModel}

class ProcessingData {

  def nullValuePercentages(df: DataFrame): Map[String, Double] = {
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
    val rateFetcher = ExchangeRateFetcher.getRates.getJsonObject("quotes")
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

  private val vectorToFirstElementUDF = udf((vec: DenseVector) => vec.toArray.headOption.getOrElse(0.0))

  private def listToOneHotUDF(numOfValues: Int) = udf((list: Seq[Int]) => {
    val vector = Array.fill(numOfValues)(0.0)
    Option(list).getOrElse(Seq()).foreach { value =>
      if (value >= 0 && value < numOfValues) vector(value) = 1.0
    }
    new DenseVector(vector).toSparse
  })

  def encodeAndReduce(data: DataFrame, columnName: String, numOfValues: Int): DataFrame = {
    // Pre-processing: Convert the string column to an array of integers and then to a One-Hot encoded vector
    val processedData = data.withColumn(columnName, split(expr(s"substring(${columnName}, 2, length(${columnName})-2)"), ",").cast("array<int>"))
      .withColumn(columnName, listToOneHotUDF(numOfValues)(col(columnName)))

    // Create a dense vector for PCA
    val assembler = new VectorAssembler()
      .setInputCols(Array(columnName))
      .setOutputCol("features")

    // Apply PCA
    val pca = new PCA()
      .setInputCol("features")
      .setOutputCol(s"${columnName}_pca")
      .setK(1)

    // Create a pipeline to manage transformations
    val pipeline = new Pipeline()
      .setStages(Array(assembler, pca))

    // Model path based on column name
    val modelPath = s"src/data/model/pca_$columnName"

    // Check if model exists
    val pipelineModel = if (Files.exists(Paths.get(modelPath))) {
      PipelineModel.load(modelPath)
    } else {
      val trainedModel = pipeline.fit(processedData)
      trainedModel.write.overwrite().save(modelPath)
      trainedModel
    }

    // Transform data
    val transformedData = pipelineModel.transform(processedData)
    // Drop the intermediate columns and rename the PCA output to the original column name
    transformedData.drop(columnName, "features")
      .withColumnRenamed(s"${columnName}_pca", columnName)
      .withColumn(columnName, vectorToFirstElementUDF(col(columnName)))
  }

  def encodingString(name:String,data:DataFrame):DataFrame={
    val path = "src/data/model/encode_list_"+name

    if(!Files.exists(Paths.get(path))){
      try {
        Files.createDirectories(Paths.get(path))
        System.out.println("Directory created successfully!")
      } catch {
        case e:Exception=> println(s"Failed to create directory! Reason: ${e.getMessage}")
      }
    }
    else{
      System.out.println("Directory already exist!")
    }

    val indexed = if (Files.exists(Paths.get(path + "/metadata"))) {
      val loadedIndexer = StringIndexerModel.load(path)
      print("load model")
      val result =  loadedIndexer.transform(data).drop(name).withColumnRenamed(name+"_index",name)
      result
    }
    else {
      val indexer = new StringIndexer()
        .setInputCol(name)
        .setOutputCol(name+"_index")
        .setHandleInvalid("keep")

      print("create model")
      val indexerModel = indexer.fit(data)
      val result = indexerModel.transform(data).drop(name).withColumnRenamed(name+"_index",name)
      indexerModel.write.overwrite().save(path)
      result
    }
    indexed
  }
}
