package com.paro

import java.time.{LocalDateTime, ZoneId}

import com.spotify.scio.ContextAndArgs
import com.spotify.scio.parquet.avro._
import org.apache.beam.sdk.io._
import org.apache.beam.sdk.transforms.windowing.{FixedWindows, Window}
import org.joda.time.Duration

import scala.util.Random

object PubSubToAvro {

  /**
    * Run with:
    * run --project=sky-italia-bigdata --runner=DirectRunner --gcpTempLocation=gs://sky-ita-data-analytics-dev/dataflow/temp --stagingLocation=gs://sky-ita-data-analytics-dev/dataflow/staging --tempLocation=gs://sky-ita-data-analytics-dev/dataflow/temp --region=europe-west1
    *
    * Message Example: 1,user1,user1_email,test_file
    */
  def main(cargs: Array[String]): Unit = {

    val (sc, args) = ContextAndArgs(cargs)

    sc.pubsubSubscription[String]("projects/sky-italia-bigdata/subscriptions/poc-pubsub-to-avro")
      .withFixedWindows(Duration.standardSeconds(10))
      .map { s =>
        TestType(
          id = s.toInt,
          name = s"user $s",
          email = s"$s@gmail.com",
          timestamp=LocalDateTime.of(2019, 1, Random.nextInt(20)+1, 0,0,0)
            .atZone(ZoneId.of("Europe/Rome")).toInstant.toEpochMilli,
          destination = s //(s % 5).toString
        )
      }
      .applyTransform(Window
        .into[TestType](FixedWindows.of(Duration.standardSeconds(1)))
      ).map(x => {
        println("before saving")
        x
      })
      .saveAsParquetAvroFile("avro-parquet-test.parquet")

    sc.close().waitUntilFinish()
  }

}

