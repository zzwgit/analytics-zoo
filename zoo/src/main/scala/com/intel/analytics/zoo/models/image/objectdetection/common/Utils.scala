/*
 * Copyright 2018 Analytics Zoo Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.analytics.zoo.models.image.objectdetection.common

import java.io.File

import com.intel.analytics.bigdl.nn.Graph._
import com.intel.analytics.bigdl.nn.{ReLU, SpatialConvolution, Xavier, Zeros}
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.transform.vision.image.ImageFeature
import com.intel.analytics.zoo.feature.image.{ImageSet, LocalImageSet}
import com.intel.analytics.zoo.models.image.objectdetection.common.dataset.roiimage.ByteRecord
import org.apache.hadoop.io.Text
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag


object IOUtils {
  def loadSeqFiles(nPartition: Int, seqFloder: String, sc: SparkContext): RDD[ByteRecord] = {
    sc.sequenceFile(seqFloder, classOf[Text], classOf[Text],
      nPartition).map(x => ByteRecord(x._2.copyBytes(), x._1.toString))
  }

  def localImagePaths(folder: String): LocalImageSet = {
    val arr = new File(folder).listFiles().map(x => {
      val imf = ImageFeature()
      imf(ImageFeature.uri) = x.getAbsolutePath
      imf
    })
    ImageSet.array(arr)
  }
}

object OBUtils {
  def addConvRelu[@specialized(Float, Double) T: ClassTag](prevNodes: ModuleNode[T],
    p: (Int, Int, Int, Int, Int), name: String, prefix: String = "conv", nGroup: Int = 1,
    propogateBack: Boolean = true)(implicit ev: TensorNumeric[T]): ModuleNode[T] = {
    val conv = SpatialConvolution[T](p._1, p._2, p._3, p._3, p._4, p._4,
      p._5, p._5, nGroup = nGroup, propagateBack = propogateBack)
      .setInitMethod(weightInitMethod = Xavier, biasInitMethod = Zeros)
      .setName(s"$prefix$name").inputs(prevNodes)
    ReLU[T](true).setName(s"relu$name").inputs(conv)
  }
}
