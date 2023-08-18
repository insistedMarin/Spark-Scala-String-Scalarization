# Spark-Scala-String-Scalarization

A data processing pipeline developed using Scala and Spark, aimed at preprocessing strings and other data types for machine learning.

## Table of Contents

1. [Overview](#overview)
2. [Setup and Configuration](#setup-and-configuration)
3. [Functionalities](#functionalities)
4. [Data and Execution](#data-and-execution)
5. [Retraining Models](#retraining-models)

## Overview

This project contains several functions to process and transform string data into a format suitable for machine learning.

## Setup and Configuration

1. Ensure you have Spark and Hadoop properly configured on your machine. If you are using Windows, make sure to add `winutils.exe` to the `bin` directory of your Hadoop installation. 

> **Note for Windows Users**: You can download the necessary `winutils.exe` binaries from [this GitHub repository](https://github.com/steveloughran/winutils). Make sure to select the version that matches your Hadoop installation.
2. Clone this repository. Please note that the data files and main function (`main.scala`) have not been included in this repository. Therefore, the project cannot be directly executed.

## Functionalities

- **Currency Conversion**:
    - Convert a given currency value to USD using real-time fetched exchange rates.
    - Example usage:
      ```scala
      val convertCurrency: UserDefinedFunction = udf((amount: Double, currency: String) => {...}
      ```

- **String Encoding**:
    - Encodes strings based on an indexing system.
    - Indexing models are saved in the `data/model` directory for future consistency.
    - Example usage:
      ```scala
      def encodeString(name: String): DataFrame = {...}
      ```

- **Array Encoding and PCA Reduction**:
    - Handles arrays in string format. There are two variants:
        1. Processes continuous integer arrays starting from zero.
        2. General case handling any array values with a provided mapping array.
    - Converts arrays to one-hot encoded vectors followed by PCA reduction.
    - Example usage:
      ```scala
      def encodeAndReduce(columnName: String, numOfValues: Option[Int] = None, mappingArray: Option[Array[String]] = None): DataFrame = {...}
      ```

- **PCA Reduction for Sparse Attributes**:
    - Reduces dimensionality of specified columns.
    - Combines columns into a single vector and applies PCA.
    - Extracts reduced attributes from PCA vectors.
    - Example usage:
      ```scala
      def applyPCA(columns: List[String], k: Int): DataFrame = {...}
      ```

## Data and Execution

- The main execution function (`main.scala`) is also not present. Users will need to handle execution specifics in their local setup.
- The `DataFrameWrapper` class uses Scala's implicit conversion feature. When using its functionalities on a Spark DataFrame, you simply need to import `DataFrameImplicit._`. This will allow you to access the added functionalities directly on the DataFrame instances.

## Retraining Models

- All transformation models are saved in the `data/model` directory.
- For retraining any model, delete the corresponding model files. The system will retrain and save new models upon the next run.

## Docker Command

'''cmd
docker build -t image-name:tag .
'''

'''cmd
docker run -it -v src\data:/opt/spark/work-dir/src/data --name container-name image-name:tag 
'''
