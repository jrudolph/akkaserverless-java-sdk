/*
 * Copyright 2021 Lightbend Inc.
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

package com.lightbend.akkasls.codegen.java

import com.lightbend.akkasls.codegen.{ DescriptorSet, ModelBuilder }
import com.lightbend.akkasls.codegen.tests.BuildInfo

import java.io.{ File, FileFilter }
import sys.process._

class ExampleSuite extends munit.FunSuite {
  import ExampleSuite._

  implicit val codegenLog = new com.lightbend.akkasls.codegen.Log {
    override def debug(message: String): Unit = println(s"[DEBUG] $message")
    override def info(message: String): Unit = println(s"[INFO] $message")
  }
  implicit val fqnExtractor = FullyQualifiedNameExtractor

  val testsDir = new File(getClass.getClassLoader.getResource("tests").toURI)
  val tests = testsDir.listFiles().filter(d => d.isDirectory).toVector

  tests.foreach { testDir =>
    test(testDir.getName) {
      val protoDir = testDir / "proto"
      val protos = protoDir.byName(_.endsWith(".proto"))
      val tmpDesc = File.createTempFile("user", ".desc")
      tmpDesc.deleteOnExit()

      s"""${BuildInfo.protocExecutable.getAbsolutePath} --include_imports --proto_path=${protoDir.getAbsolutePath} --proto_path=${BuildInfo.protocExternalSourcePath} --proto_path=${BuildInfo.protocExternalIncludePath} --descriptor_set_out=${tmpDesc.getAbsolutePath} ${protos
        .map(_.getAbsolutePath)
        .mkString(" ")}""".!!

      val fileDescs = DescriptorSet.fileDescriptors(tmpDesc).right.get.right.get
      val model = ModelBuilder.introspectProtobufClasses(fileDescs)
      println(model)

    }
  }

}
object ExampleSuite {
  implicit class FileTools(val f: File) extends AnyVal {
    def /(path: String): File = new File(f, path)
    def byName(filter: String => Boolean): Seq[File] = f.listFiles(f => filter(f.getName)).toVector
  }
}
