/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package br
package analyses

import java.io.File
import org.opalj.log.GlobalLogContext

/**
 * Writes out (a subset of) the class hierarchy in the format used by
 * [[org.opalj.br.ClassHierarchy]] to create the pre-initialized
 * class hierarchy.
 *
 * @author Michael Eichberg
 */
object ClassHierarchyExtractor {

    def main(args: Array[String]): Unit = {

        import Console.err

        import org.opalj.br.reader.Java8Framework.ClassFiles

        if (args.length < 3 || !args.drop(2).forall(_.endsWith(".jar"))) {
            println("Usage:     java …ClassHierarchy supertype filterprefix <JAR file>+")
            println("Example:   … java.lang.Enum \"\" .../rt.jar")
            println("           lists all subclasses of java.lang.Enum in rt.jar; \"\" effectively disables the filter.")
            println("Copyright: 2015 Michael Eichberg (eichberg@informatik.tu-darmstadt.de)")
            sys.exit(-1)
        }

        val supertypeName = args(0).replace('.', '/')
        val filterPrefix = args(1).replace('.', '/')

        val classFiles =
            (List.empty[(ClassFile, java.net.URL)] /: args) { (cfs, filename) ⇒
                cfs ++ ClassFiles(new File(filename))
            }
        val classHierarchy = ClassHierarchy(classFiles.view.map(_._1))(GlobalLogContext)
        val supertype = ObjectType(supertypeName)
        if (classHierarchy.isUnknown(supertype)) {
            err.println(s"The type: $supertypeName is not defined in the specified jar(s).")
            sys.exit(-2)
        }

        println(
            "# Class hierarchy for: "+
                supertypeName+
                " limited to subclasses that start with: "+
                "\""+filterPrefix+"\""
        )
        val allRelevantSubtypes =
            classHierarchy.allSubtypes(supertype, true).filter { candidateType ⇒
                candidateType.fqn.startsWith(filterPrefix)
            }
        val specLines = allRelevantSubtypes.map { aType ⇒
            var specLine =
                (
                    if (classHierarchy.isInterface(aType).isYes)
                        "interface "
                    else
                        "class "
                ) + aType.fqn
            val superclassType = classHierarchy.superclassType(aType)
            if (superclassType.isDefined) {
                specLine += " extends "+superclassType.get.fqn
                val superinterfaceTypes = classHierarchy.superinterfaceTypes(aType)
                if (superinterfaceTypes.isDefined && superinterfaceTypes.get.nonEmpty) {
                    specLine += superinterfaceTypes.get.map(_.fqn).mkString(" implements ", ", ", "")
                }
            }
            specLine
        }
        println(specLines.mkString("\n"))
    }
}
