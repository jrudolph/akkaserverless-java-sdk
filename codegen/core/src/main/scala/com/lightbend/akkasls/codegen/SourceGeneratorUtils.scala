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

package com.lightbend.akkasls.codegen

import java.nio.file.Path
import java.nio.file.Paths

import scala.annotation.tailrec
import scala.collection.immutable

import com.lightbend.akkasls.codegen.ModelBuilder.Command
import com.lightbend.akkasls.codegen.ModelBuilder.MessageTypeArgument
import com.lightbend.akkasls.codegen.ModelBuilder.ScalarType
import com.lightbend.akkasls.codegen.ModelBuilder.ScalarTypeArgument
import com.lightbend.akkasls.codegen.ModelBuilder.State
import com.lightbend.akkasls.codegen.ModelBuilder.TypeArgument

object SourceGeneratorUtils {
  val managedComment =
    """// This code is managed by Akka Serverless tooling.
       |// It will be re-generated to reflect any changes to your protobuf definitions.
       |// DO NOT EDIT""".stripMargin

  val unmanagedComment =
    """// This class was initially generated based on the .proto definition by Akka Serverless tooling.
       |//
       |// As long as this file exists it will not be overwritten: you can maintain it yourself,
       |// or delete it so it is regenerated as needed.""".stripMargin

  def mainPackageName(classNames: Iterable[String]): List[String] = {
    val packages = classNames
      .map(
        _.replaceFirst("\\.[^.]*$", "")
          .split("\\.")
          .toList)
      .toSet
    if (packages.isEmpty) throw new IllegalArgumentException("Nothing to generate!")
    longestCommonPrefix(packages.head, packages.tail)
  }

  @tailrec
  def longestCommonPrefix(
      reference: List[String],
      others: Set[List[String]],
      resultSoFar: List[String] = Nil): List[String] = {
    reference match {
      case Nil =>
        resultSoFar
      case head :: tail =>
        if (others.forall(p => p.headOption.contains(head)))
          longestCommonPrefix(tail, others.map(_.tail), resultSoFar :+ head)
        else
          resultSoFar
    }

  }

  def disassembleClassName(fullClassName: String): (String, String) = {
    val className = fullClassName.reverse.takeWhile(_ != '.').reverse
    val packageName = fullClassName.dropRight(className.length + 1)
    packageName -> className
  }

  def qualifiedType(fullyQualifiedName: FullyQualifiedName): String =
    if (fullyQualifiedName.parent.javaMultipleFiles) fullyQualifiedName.name
    else s"${fullyQualifiedName.parent.javaOuterClassname}.${fullyQualifiedName.name}"

  def typeImport(fullyQualifiedName: FullyQualifiedName): String = {
    val name =
      if (fullyQualifiedName.parent.javaMultipleFiles) fullyQualifiedName.name
      else if (fullyQualifiedName.parent.javaOuterClassnameOption.nonEmpty) fullyQualifiedName.parent.javaOuterClassname
      else fullyQualifiedName.name
    s"${fullyQualifiedName.parent.javaPackage}.$name"
  }

  def lowerFirst(text: String): String =
    text.headOption match {
      case Some(c) => c.toLower.toString + text.drop(1)
      case None    => ""
    }

  def packageAsPath(packageName: String): Path =
    Paths.get(packageName.replace(".", "/"))

  def generateImports(
      types: Iterable[FullyQualifiedName],
      packageName: String,
      otherImports: Seq[String],
      packageImports: Seq[String] = Seq.empty): Imports = {
    val messageTypeImports = types
      .filterNot { typ =>
        typ.parent.javaPackage == packageName
      }
      .filterNot { typ =>
        packageImports.contains(typ.parent.javaPackage)
      }
      .map(typeImport)

    new Imports(packageName, (messageTypeImports ++ otherImports ++ packageImports).toSeq.distinct.sorted)
  }

  def generateCommandImports(
      commands: Iterable[Command],
      state: State,
      packageName: String,
      otherImports: Seq[String],
      packageImports: Seq[String] = Seq.empty): Imports = {
    val types = commandTypes(commands) :+ state.fqn
    generateImports(types, packageName, otherImports, packageImports)
  }

  def generateCommandAndTypeArgumentImports(
      commands: Iterable[Command],
      typeArguments: Iterable[TypeArgument],
      packageName: String,
      otherImports: Seq[String],
      packageImports: Seq[String] = Seq.empty): Imports = {

    val types = commandTypes(commands) ++
      typeArguments.collect { case MessageTypeArgument(fqn) => fqn }

    generateImports(types, packageName, otherImports ++ extraTypeImports(typeArguments), packageImports)
  }

  def extraTypeImports(typeArguments: Iterable[TypeArgument]): Seq[String] =
    typeArguments.collect { case ScalarTypeArgument(ScalarType.Bytes) =>
      "com.google.protobuf.ByteString"
    }.toSeq

  def commandTypes(commands: Iterable[Command]): Seq[FullyQualifiedName] =
    commands.flatMap(command => Seq(command.inputType, command.outputType)).toSeq

  def typeName(fqn: FullyQualifiedName)(implicit imports: Imports): String = {
    if (fqn.fullQualifiedName == "com.google.protobuf.any.Any") "ScalaPbAny"
    else if (imports.contains(fqn.fullQualifiedName)) fqn.name
    else if (fqn.parent.javaPackage == imports.currentPackage) fqn.name
    else if (imports.contains(fqn.parent.javaPackage))
      fqn.parent.javaPackage.split("\\.").last + "." + fqn.name
    else fqn.fullQualifiedName
  }

  def writeImports(imports: Imports, isScala: Boolean): String = {
    val suffix = if (isScala) "" else ";"
    imports.imports
      .map { imported =>
        if (imported == "com.google.protobuf.any.Any") {
          s"import com.google.protobuf.any.{ Any => ScalaPbAny }${suffix}"
        } else
          s"import $imported${suffix}"
      }
      .mkString("\n")
  }

  def collectRelevantTypes(
      fullQualifiedNames: Iterable[FullyQualifiedName],
      service: FullyQualifiedName): immutable.Seq[FullyQualifiedName] = {
    fullQualifiedNames.filterNot { desc =>
      desc.parent == service.parent
    }.toList
  }

  def collectRelevantTypeDescriptors(
      fullQualifiedNames: Iterable[FullyQualifiedName],
      service: FullyQualifiedName): String = {
    collectRelevantTypes(fullQualifiedNames, service)
      .map(desc => s"${desc.parent.javaOuterClassname}.getDescriptor()")
      .distinct
      .sorted
      .mkString(",\n")
  }

  def extraReplicatedImports(replicatedData: ModelBuilder.ReplicatedData): Seq[String] = {
    replicatedData match {
      // special case ReplicatedMap as heterogeneous with ReplicatedData values
      case _: ModelBuilder.ReplicatedMap => Seq("com.akkaserverless.replicatedentity.ReplicatedData")
      case _                             => Seq.empty
    }
  }

  def dataType(typeArgument: ModelBuilder.TypeArgument, isScala: Boolean)(implicit imports: Imports): String =
    typeArgument match {
      case ModelBuilder.MessageTypeArgument(fqn) =>
        // FIXME: there is a bug here the full name is EntityOuterClass.SomeValue
        //  and the fullQualifiedName is com.example.service.domain.SomeValue (missing EntityOuterClass)
        if (isScala) typeName(fqn)
        else fqn.fullName
      case ModelBuilder.ScalarTypeArgument(scalar) =>
        scalar match {
          case ModelBuilder.ScalarType.Int32 | ModelBuilder.ScalarType.UInt32 | ModelBuilder.ScalarType.SInt32 |
              ModelBuilder.ScalarType.Fixed32 | ModelBuilder.ScalarType.SFixed32 =>
            if (isScala) "Int" else "Integer"
          case ModelBuilder.ScalarType.Int64 | ModelBuilder.ScalarType.UInt64 | ModelBuilder.ScalarType.SInt64 |
              ModelBuilder.ScalarType.Fixed64 | ModelBuilder.ScalarType.SFixed64 =>
            "Long"
          case ModelBuilder.ScalarType.Double => "Double"
          case ModelBuilder.ScalarType.Float  => "Float"
          case ModelBuilder.ScalarType.Bool   => "Boolean"
          case ModelBuilder.ScalarType.String => "String"
          case ModelBuilder.ScalarType.Bytes  => "ByteString"
          case _                              => if (isScala) "_" else "?"
        }
    }

  def parameterizeDataType(replicatedData: ModelBuilder.ReplicatedData, isScala: Boolean)(implicit
      imports: Imports): String = {
    val typeArguments =
      replicatedData match {
        // special case ReplicatedMap as heterogeneous with ReplicatedData values
        case ModelBuilder.ReplicatedMap(key) => Seq(dataType(key, isScala), "ReplicatedData")
        case data                            => data.typeArguments.map(typ => dataType(typ, isScala))
      }
    parameterizeTypes(typeArguments, isScala)
  }

  def parameterizeTypes(types: Iterable[String], isScala: Boolean): String =
    if (types.isEmpty) ""
    else if (isScala) types.mkString("[", ", ", "]")
    else types.mkString("<", ", ", ">")

}
