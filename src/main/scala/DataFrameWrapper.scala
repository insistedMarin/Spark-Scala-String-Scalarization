import org.apache.spark.sql.{DataFrame, functions => F}
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.{PCA, StringIndexer, StringIndexerModel, VectorAssembler}
import org.apache.spark.ml.linalg.DenseVector

import java.nio.file.{Files, Paths}
import org.apache.spark.sql.functions.{col, expr, udf}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.sql.catalyst.dsl.expressions.StringToAttributeConversionHelper

import scala.Double.PositiveInfinity
class DataFrameWrapper(df:DataFrame) {

  private val vectorToFirstElementUDF = udf((vec: DenseVector) => vec.toArray.headOption.getOrElse(0.0))

  private def listToOneHotUDF(numOfValues: Int) = udf((list: Seq[Int]) => {
    val vector = Array.fill(numOfValues)(0.0)
    Option(list).getOrElse(Seq()).foreach { value =>
      if (value >= 0 && value < numOfValues) vector(value) = 1.0
    }
    new DenseVector(vector).toSparse
  })

  private def listToOneHotWithMapUDF(mappingArray: Array[String]) = udf((list: Seq[String]) => {
    val numOfValues = mappingArray.length
    val vector = Array.fill(numOfValues)(0.0)

    // Handle null or empty values
    Option(list).getOrElse(Seq()).foreach { value =>
      val index = mappingArray.indexOf(value)
      if (index >= 0 && index < numOfValues) vector(index) = 1.0
    }

    new DenseVector(vector).toSparse
  })


  def replaceInfinityWithMax(columnName: String): DataFrame = {
    // Step 1: Find the largest value in the column excluding Infinity
    val maxValExcludingInfinity = df.filter(col(columnName) =!= PositiveInfinity)
      .agg(F.max(col(columnName)))
      .head().getDouble(0)

    // Step 2: Replace Infinity with the maximum value found
    df.withColumn(columnName, when(col(columnName) === Double.PositiveInfinity, maxValExcludingInfinity*3+1).otherwise(col(columnName)))
  }
  def encodeAndReduce(columnName: String, numOfValues: Option[Int] = None, mappingArray: Option[Array[String]] = None): DataFrame = {
    if (numOfValues.isDefined && mappingArray.isDefined) {
      throw new IllegalArgumentException("Please provide either numOfValues or mappingArray, but not both.")
    }
    // Pre-processing: Convert the string column to an array of integers and then to a One-Hot encoded vector
    val processedData = (numOfValues, mappingArray) match {
      case (Some(dim), None) =>
        df.withColumn(columnName, split(expr(s"substring(${columnName}, 2, length(${columnName})-2)"), ",").cast("array<int>"))
          .withColumn(columnName, listToOneHotUDF(dim)(col(columnName)))
      case (None, Some(arr)) =>
        df.withColumn(columnName, split(expr(s"substring(${columnName}, 2, length(${columnName})-2)"), ",").cast("array<int>"))
          .withColumn(columnName, listToOneHotWithMapUDF(arr)(col(columnName)))
      case _ => throw new IllegalArgumentException("Please provide either numOfValues or mappingArray.")
    }

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

  def encodeString(name: String): DataFrame = {
    val path = "src/data/model/encode_list_" + name

    if (!Files.exists(Paths.get(path))) {
      try {
        Files.createDirectories(Paths.get(path))
        System.out.println("Directory created successfully!")
      } catch {
        case e: Exception => println(s"Failed to create directory! Reason: ${e.getMessage}")
      }
    }
    else {
      System.out.println("Directory already exist!")
    }

    val indexed = if (Files.exists(Paths.get(path + "/metadata"))) {
      val loadedIndexer = StringIndexerModel.load(path)
      print("load model")
      val result = loadedIndexer.transform(df).drop(name).withColumnRenamed(name + "_index", name)
      result
    }
    else {
      val indexer = new StringIndexer()
        .setInputCol(name)
        .setOutputCol(name + "_index")
        .setHandleInvalid("keep")

      print("create model")
      val indexerModel = indexer.fit(df)
      val result = indexerModel.transform(df).drop(name).withColumnRenamed(name + "_index", name)
      indexerModel.write.overwrite().save(path)
      result
    }
    indexed
  }

  def applyPCA(columns: List[String], k: Int): DataFrame = {
    // Assemble the specified columns into a single vector column
    val assembler = new VectorAssembler()
      .setInputCols(columns.toArray)
      .setOutputCol("features")

    // Apply PCA on the vector column
    val pca = new PCA()
      .setInputCol("features")
      .setOutputCol("pcaFeatures")
      .setK(k)

    // Create a pipeline for the transformations
    val pipeline = new Pipeline().setStages(Array(assembler, pca))

    // Model path
    val modelPath = "src/data/model/pca_main"

    // Check if model exists
    val pipelineModel = if (Files.exists(Paths.get(modelPath))) {
      PipelineModel.load(modelPath)
    } else {
      val trainedModel = pipeline.fit(df)
      trainedModel.write.overwrite().save(modelPath)
      trainedModel
    }

    // Transform the DataFrame
    val resultDF = pipelineModel.transform(df)

    // UDF to extract elements from the PCA vector
    val extract = udf((vector: DenseVector, index: Int) => vector(index))

    // Generate new column names for PCA output
    val pcaColumns = (1 to k).map(i => s"pca_$i").toList

    // Split the PCA output vector into separate columns
    var outputDF = resultDF
    for ((name, idx) <- pcaColumns.zipWithIndex) {
      outputDF = outputDF.withColumn(name, extract(col("pcaFeatures"), lit(idx)))
    }

    // Drop intermediate columns
    outputDF.drop("features","pcaFeatures")
      .drop(columns: _*)
  }
}
