/*
 * Copyright 2016 LinkedIn Corp. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.linkedin.photon.ml

import breeze.linalg.DenseVector
import com.linkedin.photon.ml.data.LabeledPoint
import com.linkedin.photon.ml.supervised.model.CoefficientSummary
import com.linkedin.photon.ml.supervised.regression.LinearRegressionModel
import com.linkedin.photon.ml.test.SparkTestUtils
import org.apache.spark.rdd.RDD
import org.testng.annotations.Test

/**
 * Integration tests for bootstrapping. Most of the heavy lifting has already been done in the unit tests
 */
class BootstrapTrainingIntegTest extends SparkTestUtils {

  import org.testng.Assert._

  val lambdas: List[Double] = List(0.01, 0.1, 1.0)
  val numWorkers: Int = Math.max(1, Runtime.getRuntime.availableProcessors / 2)
  val samplePct = 0.01
  val seed = 0L
  val numSamples = 100

  def regressionModelFitFunction(coefficient: Double, lambdas: Seq[Double])
  : (RDD[LabeledPoint], Map[Double, LinearRegressionModel]) => List[(Double, LinearRegressionModel)] = {
    (x: RDD[LabeledPoint], y: Map[Double, LinearRegressionModel]) => {
      lambdas.map(l => (l, new LinearRegressionModel(DenseVector.ones[Double](BootstrapTrainingTest.NUM_DIMENSIONS) *
          coefficient, None))).toList
    }
  }

  /**
   * Sanity check that the bootstrapping mechanics appear to work before we attempt to do integration tests with
   * "real" aggregation operations and data sets
   */
  @Test
  def checkBootstrapHappyPathRegressionDummyAggregates(): Unit = sparkTest("checkBootstrapHappyPathDummyAggregates") {
    val identity = (x: Seq[(LinearRegressionModel, Map[String, Double])]) => {
      x
    }
    val identityKey: String = "identity"
    val aggregations: Map[String, Seq[(LinearRegressionModel, Map[String, Double])] => Any] = Map(identityKey -> identity)

    // Generate an empty RDD (model fitting is mocked out but we need a "real" instance for the sampling to work)
    val data: RDD[LabeledPoint] = sc.parallelize(drawSampleFromNumericallyBenignDenseFeaturesForLinearRegressionLocal(seed.toInt, numSamples, BootstrapTrainingTest.NUM_DIMENSIONS).toSeq).map(x => new LabeledPoint(x._1, x._2))

    val result: Map[Double, Map[String, Any]] = BootstrapTraining.bootstrap[LinearRegressionModel](
      BootstrapTrainingTest.NUM_SAMPLES,
      samplePct,
      Map[Double, LinearRegressionModel](),
      regressionModelFitFunction(0, lambdas),
      aggregations,
      data)

    // Verify that we got the expected results
    assertEquals(result.size, lambdas.size, "Result has expected number of keys")
    lambdas.foreach(x => {
      result.get(x) match {
        case Some(aggregates) =>
          aggregates.get(identityKey) match {
            case Some(models) =>
              models match {
                case m: TraversableOnce[(LinearRegressionModel, Map[String, Double])] => assertEquals(m.size, BootstrapTrainingTest.NUM_SAMPLES, "Number of bootstrapped models matches expected")
                case _ => fail(f"Found aggregate for lambda=[$x%.04f] and name [$identityKey] with unexpected type")
              }
            case None =>
              fail(f"Aggregate [$identityKey] appears to be missing")
          }

        case None =>
          fail(f"Result is missing aggregates for lambda = [$x%.04f]")

        case _ =>
          fail(f"Result has aggregates for lambda = [$x%.04f] with unexpected type")
      }
    })
  }
}
