/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.io;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.xnio.Option;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class OptionsResourceDefinition extends PersistentResourceDefinition {

    private final List<OptionAttributeDefinition> attributes;


    private OptionsResourceDefinition(PathElement pathElement, ResourceDescriptionResolver resolver, List<OptionAttributeDefinition> options) {
        super(pathElement, resolver);
        attributes = options;
    }


    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        if (resourceRegistration.getOperationEntry(PathAddress.EMPTY_ADDRESS, ModelDescriptionConstants.ADD) == null) {
            registerAddOperation(resourceRegistration, new AbstractAddStepHandler(getAttributes()), OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        }
        if (resourceRegistration.getOperationEntry(PathAddress.EMPTY_ADDRESS, ModelDescriptionConstants.REMOVE) == null) {
            registerRemoveOperation(resourceRegistration, ReloadRequiredRemoveStepHandler.INSTANCE, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return (Collection) attributes;
    }

    public static Builder builder(PathElement pathElement, ResourceDescriptionResolver resolver) {
        return new Builder(pathElement, resolver);
    }

    public static class Builder {
        private final PathElement pathElement;
        private final ResourceDescriptionResolver resolver;
        private List<OptionAttributeDefinition> attributes = new LinkedList<>();

        private Builder(PathElement pathElement, ResourceDescriptionResolver resolver) {
            this.pathElement = pathElement;
            this.resolver = resolver;
        }

        public Builder addOption(Option<?> option, String name) {
            attributes.add(OptionAttributeDefinition.builder(name, option)
                    .build()
            );
            return this;
        }

        public Builder addOption(Option<?> option, String name, ModelNode defaultValue) {
            attributes.add(OptionAttributeDefinition.builder(name, option)
                    .setDefaultValue(defaultValue)
                    .build()
            );
            return this;
        }

        public OptionsResourceDefinition build() {
            return new OptionsResourceDefinition(pathElement, resolver, attributes);
        }

    }
}
