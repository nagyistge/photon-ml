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
package com.linkedin.photon.ml.supervised

import scala.reflect.ClassTag

import com.linkedin.photon.ml.data.LabeledPoint
import com.linkedin.photon.ml.supervised.model.GeneralizedLinearModel
import com.linkedin.photon.ml.supervised.regression.Regression
import org.apache.spark.rdd.RDD


/**
 * @author bdrew
 */
class MaximumDifferenceValidator[-R <: GeneralizedLinearModel with Regression with Serializable: ClassTag](
    maximumDifference:Double) extends ModelValidator[R] {

  assert(maximumDifference > 0.0)
  def validateModelPredictions(model:R, data:RDD[LabeledPoint]) = {
    val countTooBig = data.filter( x => { Math.abs(model.predict(x.features) - x.label) > maximumDifference}).count()
    if (countTooBig > 0) {
      throw new IllegalStateException(s"Found [$countTooBig] instances where the magnitude of the prediction error " +
          s"is greater than [$maximumDifference].")
    }
  }
}
