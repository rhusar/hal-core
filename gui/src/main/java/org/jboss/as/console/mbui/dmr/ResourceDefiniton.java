package org.jboss.as.console.mbui.dmr;

import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @since 29/08/14
 */
public class ResourceDefiniton extends ModelNode {
    public ResourceDefiniton(ModelNode resourceDescription) {
        this.set(resourceDescription);
    }
}
