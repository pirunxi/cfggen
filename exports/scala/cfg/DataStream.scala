package perfect.cfg

import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.List

object DataStream {
  private val magicStringForNewLine: String = ".g9~/"

  def create(dataFile: String, inputEncoding: String): DataStream = {
    try {
      return new DataStream(Files.readAllLines(new File(dataFile).toPath, Charset.forName(inputEncoding)))
    }
    catch {
      case e: Exception => {
        e.printStackTrace()
        throw new RuntimeException("data file:" + dataFile + " loads fail!")
      }
    }
  }
}

final class DataStream(val lines: List[String]) {
  private var index: Int = 0

  private def getNext: String = {
    if (index < lines.size) lines.get({
      index += 1; index - 1
    })
    else null
  }

  private def error(err: String) {
    throw new RuntimeException(s"$index $err")
  }

  private def getNextAndCheckNotEmpty : String = {
    val s = getNext
    if (s == null) error("read not enough")
    s
  }

  def getBool() : Boolean = {
    val s: String = getNextAndCheckNotEmpty.toLowerCase
    s match {
      case "true" => true
      case "false" => false
      case _ =>
        error(s + " isn't bool")
        false
    }
  }

  def getInt(): Int = {
    val s: String = getNextAndCheckNotEmpty
    s.toInt
  }

  def getLong() : Long = {
    val s: String = getNextAndCheckNotEmpty
    java.lang.Long.parseLong(s)
  }

  def getFloat() : Float = {
    val s: String = getNextAndCheckNotEmpty
    java.lang.Float.parseFloat(s)
  }

  def getString() : String = {
    getNextAndCheckNotEmpty.replace(DataStream.magicStringForNewLine, "\n")
  }

  def getObject(name: String) : CfgObject = {
    try {
      Class.forName(name).getConstructor(classOf[DataStream]).newInstance(this).asInstanceOf[CfgObject]
    }
    catch {
      case e: Exception => throw new RuntimeException(e)
    }
  }
}