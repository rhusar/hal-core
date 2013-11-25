package org.useware.kernel.model;

import org.useware.kernel.model.scopes.InterfaceStructureShadow;
import org.useware.kernel.model.scopes.Scope;
import org.useware.kernel.model.scopes.ScopeAssignment;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.builder.InteractionUnitVisitor;

/**
 * A dialog contains a set of hierarchically structured abstract interaction objects,
 * which enable the execution of an interactive task.
 *
 * @author Heiko Braun
 * @date 1/16/13
 */
public class Dialog {
    private QName id;
    private InteractionUnit root;
    private InterfaceStructureShadow<Scope> scopeModel;

    public Dialog(QName id, InteractionUnit root) {
        this.id = id;
        this.root = root;

         // create scope model
        ScopeAssignment scopeAssignment = new ScopeAssignment();
        root.accept(scopeAssignment);
        this.scopeModel = scopeAssignment.getScopeModel();
    }

    public Dialog(QName id) {
        this(id, null);
    }

    public QName getId() {
        return id;
    }

    public InteractionUnit getInterfaceModel() {
        return root;
    }

    public InteractionUnit findUnit(final QName id) {

        final Result result = new Result();

        InteractionUnitVisitor findById = new InteractionUnitVisitor() {

            @Override
            public void startVisit(Container container) {
                if (container.getId().equalsIgnoreSuffix(id))
                    result.setUnit(container);
            }

            @Override
            public void visit(InteractionUnit interactionUnit) {
                if (interactionUnit.getId().equalsIgnoreSuffix(id))
                    result.setUnit(interactionUnit);
            }

            @Override
            public void endVisit(Container container) {

            }
        };

        root.accept(findById);

        if(null==result.getUnit())
            System.out.println("No interaction unit with id "+ id);

        return result.getUnit();
    }

    public InterfaceStructureShadow<Scope> getScopeModel() {
        assert this.scopeModel !=null : "Scope model not set";
        return this.scopeModel;
    }

    class Result {
        InteractionUnit unit;

        public InteractionUnit getUnit() {
            return unit;
        }

        public void setUnit(InteractionUnit unit) {
            this.unit = unit;
        }
    }

}
