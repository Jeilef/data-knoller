package de.hpi.isg.dataprep

import java.security.MessageDigest
import java.text.{ParseException, SimpleDateFormat}
import java.util.Date

import de.hpi.isg.dataprep.util.DatePattern.DatePatternEnum
import de.hpi.isg.dataprep.util.{HashAlgorithm, RemoveCharactersMode}
import org.apache.spark.api.java.JavaRDD
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Dataset, Row}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.expressions._
import org.apache.spark.sql.types.{StructField, StructType}

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

/**
  * @author Lan Jiang
  * @since 2018/8/10
  */
@SerialVersionUID(1000L)
object ConversionHelper extends Serializable {

    private val defaultDateFormat = DatePatternEnum.YearMonthDay

    /**
      * Converts the pattern of the date value into the desired pattern.
      *
      * @param value  the value to be converted
      * @param source the origin date pattern specific by metadata
      * @param target the target date pattern.
      * @return the converted [[Date]]
      */
    @throws(classOf[Exception])
    def toDate(value: String, source: DatePatternEnum, target: DatePatternEnum): String = {
        /**
          * Succeeds to convert the date values in the correct origin format.
          * Cannot detect the incorrect order of y,m,d.
          */
        val sourceDatePattern = source.getPattern
        val sourceFormatter = new SimpleDateFormat(sourceDatePattern)
        sourceFormatter.setLenient(false)
        var sourceDateParse = Try{
            sourceFormatter.parse(value)
        }
        sourceDateParse match {
            case Failure(content) => {
                throw content
            }
            case date : Success[content] => {
                val targetDate = new SimpleDateFormat(target.getPattern).format(date.toOption.get)
                targetDate
            }
        }
    }

    def countSubstring( str:String, substr:String ) = substr.r.findAllMatchIn(str).length


    def splitFileBySeparator(separator: String, source : Dataset[Row]) : Dataset[Row] = {
      val dataList = source.collectAsList
      val rowWithMaxSeparators = dataList.toArray
        .map(x => (x, countSubstring(x.toString(), separator)))
        .maxBy(item => item._2)._1 //gibt mir x Element aus dem tupel
      val indexOfSplitLine = dataList.lastIndexOf(rowWithMaxSeparators)
      //TODO: Throw error if index <0
      val resultArray = dataList.subList(0,indexOfSplitLine).toArray
      return source.filter(row => resultArray.contains(row))
    }

  def findUnknownFileSeparator(source: Dataset[Row]) : (String, Float) = {
    val specialCharacters = source.collect.map(row => row.toString().replaceAll("[A-Za-z0-9]",""))
    val charCountList = specialCharacters.map(row => row.groupBy(c => c).mapValues(v => v.size))
    val mergedDictionaries = charCountList.map(a => a.toSeq).reduce((a,b) => a ++ b)
    val groupedDicts = mergedDictionaries.groupBy(_._1)
    val combinedDicts = groupedDicts
      .mapValues( li => li.map(_._2).toList)
      .mapValues( li => li.reduce(_+_).toFloat / (li.size*li.size).toFloat)
    return (combinedDicts.maxBy(_._2)._1.toString, combinedDicts.maxBy(_._2)._2)
  }
    
    def splitFileByType(source : DataFrame) : (DataFrame, DataFrame) = {
      //TODO: split File by different Datatypes in collumns
      (source, source)
    }
    
    def splitFileByCountingHeaders(source: Dataset[Row]) : Dataset[Row] = {
      //TODO: split file by different number of headers
      // minimal: two columns: entweder weniger / mehr
      val dataArray = source.collect()


      // Loop over rows.
        for (row <- dataArray) {
          // Loop over cells in rows.
          var countNullValue = 0
          for (cell <- row) {
            if(row.isNullAt(cell)) {
              countNullValue+=1 //wenn in der Zeile ein null value gefunden wurde, setze den counter eins hoch
              if (countNullValue == 3) {
                  val indexArray = dataArray.slice(0, cell) //aktuell werden Leerstrings mit ausgegeben
              }
            }
          }
        }





      return source
    }

    def getDefaultDate(): String = {
        val defaultDate = new Date(0)
        val defaultFormatter = new SimpleDateFormat(defaultDateFormat.getPattern)
        defaultFormatter.format(defaultDate)
    }

    def getDefaultDateFormat(): String = {
        this.defaultDateFormat.getPattern
    }

    def collapse(value: String): String = {
        val newStr = value.replaceAll("\\s+", " ");
        newStr
    }

    def trim(value: String): String = {
        val newStr = value.trim
        newStr
    }

    def padding(value: String, expectedLength: Int, padder: String): String = {
        val len = value.length
        if (len > expectedLength) {
            throw new IllegalArgumentException(String.format("Value length is already larger than padded length."))
        }
        val paddingBitLen = expectedLength - len
        val padding = padder * paddingBitLen
        val newStr = padding + value
        newStr
    }

    def replaceSubstring(value: String, regex: String, replacement: String, firstSome: Int): String = {
        val processed = firstSome match {
            case count if count > 0 => {
                var newValue = new String(value)
                1 to count foreach {
                    _ => newValue = value.replaceFirst(regex, replacement)
                }
                newValue
            }
            case 0 => {
                value.replaceAll(regex, replacement)
            }
        }
        processed
    }

    def removeCharacters(value: String, mode: RemoveCharactersMode, custom: String) : String = {
        val changed = mode match {
            case RemoveCharactersMode.NUMERIC => {
                value.replaceAll("[0-9]+", "")
            }
            case RemoveCharactersMode.NONALPHANUMERIC => {
                value.replaceAll("([^0-9a-zA-Z])+", "")
            }
            case RemoveCharactersMode.CUSTOM => {
                value.replaceAll(custom, "")
            }
        }
        changed
    }

    def hash(value: String, hashAlgorithm: HashAlgorithm) : String = {
        val bytes = hashAlgorithm match {
            case HashAlgorithm.MD5 => {
                MessageDigest.getInstance("MD5").digest(value.getBytes)
            }
            case HashAlgorithm.SHA => {
                MessageDigest.getInstance("SHA").digest(value.getBytes)
            }
        }
        bytes.map("%02x".format(_)).mkString
    }

    def ngram(value: String, n: Int) : String = {
        ""
    }

    def unicode(value: String) : String = {
        ""
    }
}
