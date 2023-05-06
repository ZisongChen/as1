import java.io.{BufferedReader, FileReader, FileWriter}
import java.nio.file.{Files, Paths}
import scala.util.Try

import com.github.tototoshi.csv._
import better.files.{File, FileMonitor}
import scala.concurrent.ExecutionContext.Implicits.global

object project extends App {

  val solarPowerPath = "D:\\作业\\论文\\dmm2\\sunday1.CSV"
  val hydroPowerPath = "D:\\作业\\论文\\dmm2\\hyday1.csv"
  val windPowerPath = "D:\\作业\\论文\\dmm2\\windday1.csv"
  val outputPath = "D:\\作业\\论文\\dmm2\\output.csv"
  val consumePath = "D:\\作业\\论文\\dmm2\\consume.csv"
  val paths = List((solarPowerPath, "SolarPower"), (hydroPowerPath, "HydroPower"), (windPowerPath, "WindPower"))
  val storageCapacity = 50000
  var currentEnergy = getTotalElectricity(outputPath)
  val storageThreshold = 0.1
  case class PowerData(date: String, powerType: String, electricity: Double)

  def getTotalElectricity(filePath: String): Double = {
    if (Files.exists(Paths.get(filePath))) {
      val reader = CSVReader.open(new BufferedReader(new FileReader(filePath)))
      val rows = reader.allWithHeaders()
      reader.close()
      if (rows.isEmpty) {
        return 0.0
      }

      val totalElectricity = rows.flatMap(row => Try(row("Electricity").toDouble).toOption).sum
      val writer = CSVWriter.open(new FileWriter(filePath))
      writer.writeRow(List("Date", "Type", "Electricity", "TotalElectricity"))
      writer.writeRow(List("", "", "", totalElectricity.toString))
      writer.close()

      totalElectricity

    } else {
      0.0
    }
  }

  def writeDataToOutputFile(data: PowerData, filePath: String, totalElectricity: Double): Unit = {
    val exists = Files.exists(Paths.get(filePath))
    val writer = CSVWriter.open(new FileWriter(filePath, true))

    if (!exists) {
      writer.writeRow(List("Date", "Type", "Electricity", "TotalElectricity"))
    }
    writer.writeRow(List(data.date, data.powerType, data.electricity.toString, totalElectricity.toString))
    writer.close()
  }

  def processData(powerData: List[PowerData], consumeData: List[PowerData]): Unit = {
    val sortedPowerData = powerData.sortBy(_.date)
    val sortedConsumeData = consumeData.sortBy(_.date)
    val combinedData = sortedPowerData.zip(sortedConsumeData)

    combinedData.foreach { case (powerData, consumeData) =>
      if (currentEnergy <= storageCapacity * 0.1) {
        currentEnergy += powerData.electricity
        writeDataToOutputFile(powerData, outputPath, currentEnergy)
      } else if (currentEnergy >= storageCapacity * 0.9) {
        currentEnergy -= consumeData.electricity
        writeDataToOutputFile(consumeData, outputPath, currentEnergy)
      } else {
        currentEnergy += powerData.electricity
        writeDataToOutputFile(powerData, outputPath, currentEnergy)
        currentEnergy -= consumeData.electricity
        writeDataToOutputFile(consumeData, outputPath, currentEnergy)
      }
    }
  }
  def readCsvFile(filePath: String, powerType: String, afterDate: Option[String]): List[PowerData] = {
    val reader = CSVReader.open(new BufferedReader(new FileReader(filePath)))
    val rows = reader.allWithHeaders()
    reader.close()

    rows
      .map(row => row.map { case (k, v) => (k.toLowerCase, v) }) // Convert column names to lowercase
      .filter(row => row.contains("date") && row.contains("electricity"))
      .filter(row => afterDate.forall(row("date") > _))
      .flatMap(row => {
        Try(PowerData(row("date"), powerType, row("electricity").toDouble)).toOption
      })
      .toList
  }

  def initialUpdate(): Unit = {
    val powerDataList = paths.flatMap { case (path, powerType) => readCsvFile(path, powerType, None) }
    val consumeData = readCsvFile(consumePath, "consume", None)
    processData(powerDataList, consumeData)
  }

  class CsvMonitor(file: better.files.File) extends FileMonitor(file, recursive = false) {
    override def onCreate(file: File, count: Int): Unit = initialUpdate()

    override def onModify(file: File, count: Int): Unit = initialUpdate()
  }

  // Perform initial update
  initialUpdate()

  val fileMonitors = paths.map { case (path, _) => new CsvMonitor(File(path)) } :+ new CsvMonitor(File(consumePath))

  fileMonitors.foreach(_.start())

  // Keep the application running
  while (true) {
    Thread.sleep(1000)
  }
}


