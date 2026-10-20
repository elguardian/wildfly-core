/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.server;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jboss.as.controller.extension.ExpressionResolverExtension;
import org.jboss.as.controller.ExpressionResolverImpl;
import org.jboss.as.controller.OperationClientException;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.extension.ResolverExtensionRegistry;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RuntimeExpressionResolver extends ExpressionResolverImpl implements ResolverExtensionRegistry {

    private static final Logger log = Logger.getLogger(RuntimeExpressionResolver.class);

    // guarded by this
    private final Set<ExpressionResolverExtension> extensions = new HashSet<>();
    private volatile CapabilityServiceSupport capabilityServiceSupport;

    public void setCapabilityServiceSupport(CapabilityServiceSupport capabilityServiceSupport) {
        this.capabilityServiceSupport = capabilityServiceSupport;
    }

    @Override
    public synchronized void addResolverExtension(ExpressionResolverExtension extension) {
        extensions.add(extension);
    }

    @Override
    public synchronized void removeResolverExtension(ExpressionResolverExtension extension) {
        extensions.remove(extension);
    }

    @Override
    protected void resolvePluggableExpression(ModelNode node, OperationContext context) throws OperationFailedException {
        String expression = node.asString();
        if (expression.length() <= 3) {
            return;
        }

        if (context != null) {
            resolveWithExtensions(node, expression, context, null);
        } else if (ExpressionResolverExtension.EXTENSION_EXPRESSION_PATTERN.matcher(expression).matches()) {
            // No OC (e.g. domain server boot preparation). Use the injected CSS if available.
            CapabilityServiceSupport css = capabilityServiceSupport;
            if (css != null) {
                resolveWithExtensions(node, expression, null, css);
            }
        }
    }

    /**
     * Cycles through all registered extensions attempting to resolve {@code expression}.
     * Exactly one of {@code context} or {@code serviceSupport} must be non-null:
     * <ul>
     *   <li>When {@code context} is non-null the OC path is used: {@code initialize(context)} is called
     *       followed by {@code resolveExpression(expression, context)}.</li>
     *   <li>When {@code serviceSupport} is non-null the CSS path is used:
     *       {@code resolveExpression(expression, serviceSupport)} is called directly.
     *       Extensions that do not support this mode throw {@link UnsupportedOperationException}
     *       and are skipped.</li>
     * </ul>
     */
    private void resolveWithExtensions(ModelNode node, String expression,
                                       OperationContext context, CapabilityServiceSupport serviceSupport)
            throws OperationFailedException {

        String result = null;
        OperationFailedException ofe = null;
        RuntimeException operationClientException = null;
        RuntimeException otherRe = null;

        synchronized (extensions) {
            Iterator<ExpressionResolverExtension> iter = extensions.iterator();
            while (result == null && iter.hasNext()) {
                ExpressionResolverExtension ext = iter.next();
                try {
                    if (context != null) {
                        ext.initialize(context);
                        result = ext.resolveExpression(expression, context);
                    } else {
                        result = ext.resolveExpression(expression, serviceSupport);
                    }
                } catch (UnsupportedOperationException uoe) {
                    log.debugf(uoe, "Extension %s does not support CSS-based expression resolution; skipping.", ext);
                } catch (OperationFailedException oe) {
                    if (ofe == null) {
                        ofe = oe;
                    }
                } catch (RuntimeException re) {
                    if (re instanceof OperationClientException) {
                        if (operationClientException == null) {
                            operationClientException = re;
                        }
                    } else if (otherRe == null) {
                        otherRe = re;
                    }
                }
            }
        }

        // If we have a result use it, otherwise throw an exception caught,
        // preferring OFEs, then other OperationClientExceptions, then server faults.
        // If no exceptions and no results, the caller will just carry on.
        if (result != null) {
            node.set(result);
        } else if (ofe != null) {
            throw ofe;
        } else if (operationClientException != null) {
            throw operationClientException;
        } else if (otherRe != null) {
            throw otherRe;
        }
    }
}
