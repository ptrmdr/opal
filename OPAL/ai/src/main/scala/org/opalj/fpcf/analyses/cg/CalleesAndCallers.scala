/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg

import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.collection.immutable.LongTrieSet
import org.opalj.collection.immutable.IntTrieSet

import scala.collection.immutable.IntMap
import org.opalj.fpcf.cg.properties.CallersOnlyWithConcreteCallers
import org.opalj.fpcf.EPK
import org.opalj.fpcf.cg.properties.CallersProperty
import org.opalj.fpcf.IntermediateESimpleP
import org.opalj.fpcf.PartialResult
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.ObjectType
import org.opalj.value.KnownTypedValue

private[cg] class CalleesAndCallers(
        private[this] var _callees: IntMap[IntTrieSet] = IntMap.empty
) {
    private[this] var _incompleteCallsites: IntTrieSet = IntTrieSet.empty

    private[this] var _partialResultsForCallers: List[PartialResult[DeclaredMethod, CallersProperty]] =
        List.empty

    private[cg] def callees: IntMap[IntTrieSet] = _callees

    private[cg] def partialResultsForCallers: List[PartialResult[DeclaredMethod, CallersProperty]] = {
        _partialResultsForCallers
    }

    private[cg] def incompleteCallsites: IntTrieSet = _incompleteCallsites

    private[cg] def addIncompleteCallsite(pc: Int): Unit = _incompleteCallsites += pc

    private[cg] def updateWithCall(
        caller: DeclaredMethod, callee: DeclaredMethod, pc: Int
    )(implicit declaredMethods: DeclaredMethods): Unit = {
        val calleeId = callee.id
        if (!_callees.contains(pc) || !_callees(pc).contains(calleeId)) {
            _callees = _callees.updated(pc, _callees.getOrElse(pc, IntTrieSet.empty) + calleeId)
            _partialResultsForCallers ::= createPartialResultForCaller(caller, callee, pc)
        }
    }

    def updateWithCallOrFallback(
        caller:             DeclaredMethod,
        callee:             org.opalj.Result[Method],
        pc:                 Int,
        callerPackage:      String,
        fallbackType:       ObjectType,
        fallbackName:       String,
        fallbackDescriptor: MethodDescriptor
    )(implicit declaredMethods: DeclaredMethods): Unit = {
        if (callee.hasValue) {
            updateWithCall(caller, declaredMethods(callee.value), pc)
        } else {
            val fallbackCallee = declaredMethods(
                fallbackType,
                callerPackage,
                fallbackType,
                fallbackName,
                fallbackDescriptor
            )
            updateWithCall(caller, fallbackCallee, pc)

        }
    }

    private[this] def createPartialResultForCaller(
        caller: DeclaredMethod, callee: DeclaredMethod, pc: Int
    )(implicit declaredMethods: DeclaredMethods): PartialResult[DeclaredMethod, CallersProperty] = {
        PartialResult[DeclaredMethod, CallersProperty](callee, CallersProperty.key, {
            case IntermediateESimpleP(_, ub) ⇒
                val newCallers = ub.updated(caller, pc)
                // here we assert that update returns the identity if there is no change
                if (ub ne newCallers)
                    Some(IntermediateESimpleP(callee, newCallers))
                else
                    None

            case _: EPK[_, _] ⇒
                val set = LongTrieSet(CallersProperty.toLong(caller.id, pc))
                Some(IntermediateESimpleP(
                    callee,
                    new CallersOnlyWithConcreteCallers(set)
                ))

            case r ⇒
                throw new IllegalStateException(s"unexpected previous result $r")
        })
    }
}

private[cg] class IndirectCalleesAndCallers(
        _callees:                      IntMap[IntTrieSet]                                                      = IntMap.empty,
        private[this] var _parameters: IntMap[Map[DeclaredMethod, Seq[Option[(KnownTypedValue, IntTrieSet)]]]] = IntMap.empty
) extends CalleesAndCallers(_callees) {
    private[cg] def parameters: IntMap[Map[DeclaredMethod, Seq[Option[(KnownTypedValue, IntTrieSet)]]]] =
        _parameters

    private[cg] override def updateWithCall(
        caller: DeclaredMethod, callee: DeclaredMethod, pc: Int
    )(implicit declaredMethods: DeclaredMethods): Unit = {
        throw new UnsupportedOperationException("Use updateWithIndirectCall instead!")
    }

    private[cg] def updateWithIndirectCall(
        caller:     DefinedMethod,
        callee:     DeclaredMethod,
        pc:         Int,
        parameters: Seq[Option[(KnownTypedValue, IntTrieSet)]]
    )(implicit declaredMethods: DeclaredMethods): Unit = {
        super.updateWithCall(caller, callee, pc)
        _parameters = _parameters.updated(
            pc,
            _parameters.getOrElse(pc, Map.empty).updated(callee, parameters)
        )
    }

    def updateWithIndirectCallOrFallback(
        caller:             DefinedMethod,
        callee:             org.opalj.Result[Method],
        pc:                 Int,
        callerPackage:      String,
        fallbackType:       ObjectType,
        fallbackName:       String,
        fallbackDescriptor: MethodDescriptor,
        parameters:         Seq[Option[(KnownTypedValue, IntTrieSet)]]
    )(implicit declaredMethods: DeclaredMethods): Unit = {
        if (callee.hasValue) {
            updateWithIndirectCall(caller, declaredMethods(callee.value), pc, parameters)
        } else {
            val fallbackCallee = declaredMethods(
                fallbackType,
                callerPackage,
                fallbackType,
                fallbackName,
                fallbackDescriptor
            )
            updateWithIndirectCall(caller, fallbackCallee, pc, parameters)

        }
    }

}