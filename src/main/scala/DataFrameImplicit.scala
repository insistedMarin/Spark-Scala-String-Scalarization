import org.apache.spark.sql.DataFrame

import scala.language.implicitConversions

object DataFrameImplicit {
  implicit def dataFrameToWrapper(df: DataFrame): DataFrameWrapper = new DataFrameWrapper(df)

}
