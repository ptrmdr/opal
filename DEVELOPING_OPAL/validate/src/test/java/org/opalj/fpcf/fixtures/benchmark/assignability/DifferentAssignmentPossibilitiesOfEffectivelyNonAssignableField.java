/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * Class encompasses two possible cases of assigning an effectively non assignable field.
 */
//@Immutable
@MutableType("Class is not final")
@TransitivelyImmutableClass("Class has only a transitively immutable field")
public class DifferentAssignmentPossibilitiesOfEffectivelyNonAssignableField {

    //@Immutable
    @TransitivelyImmutableField("Field is effectively non assignable and has a transitively immutable type")
    @EffectivelyNonAssignableField("Field is only once assigned in the constructor via new created object or parameter")
    private Object o;

    public DifferentAssignmentPossibilitiesOfEffectivelyNonAssignableField() {
        this.o = new Integer(5);
    }

    public DifferentAssignmentPossibilitiesOfEffectivelyNonAssignableField(int n) {
        this.o = new Object();
    }

    public Object getO(){
        return this.o;
    }
}
