/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.as.controller.persistence;

import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class TestConfigurationPersister extends AbstractConfigurationPersister {

    public TestConfigurationPersister() {
        super(null);
    }

    @Override
    public PersistenceResource store(ModelNode model, Set<PathAddress> affectedAddresses)
            throws ConfigurationPersistenceException {
        return create(model);
    }

    @Override
    public List<ModelNode> load() throws ConfigurationPersistenceException {
        return Collections.emptyList();
    }

    @Override
    public void marshallAsXml(ModelNode model, OutputStream output) throws ConfigurationPersistenceException {
        try {
            output.write(model.asString().getBytes());
        } catch (Exception e) {
            throw new ConfigurationPersistenceException(e);
        }
    }

    abstract PersistenceResource create(ModelNode model) throws ConfigurationPersistenceException;
}
