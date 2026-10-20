/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.host.controller;

import static org.junit.Assert.assertEquals;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.extension.ExpressionResolverExtension;
import org.jboss.as.host.controller.util.AbstractControllerTestBase;
import org.jboss.as.server.RuntimeExpressionResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ValueExpression;
import org.junit.Test;

/**
 * Integration test for WFCORE-7087: verifies that RuntimeExpressionResolver can resolve extension
 * expressions without an OperationContext when a CapabilityServiceSupport obtained from a real
 * AbstractControllerService is injected via setCapabilityServiceSupport().
 *
 * Uses the real HC test controller infrastructure (AbstractControllerTestBase /
 * TestModelControllerService) to produce an actual CapabilityServiceSupport — no mocking.
 */
public class ExpressionResolverNoContextTestCase extends AbstractControllerTestBase {

    private static final String EXTENSION_EXPR  = "${EXP::token}";
    private static final String RESOLVED_VALUE  = "resolved-secret";

    public ExpressionResolverNoContextTestCase() {
        super(ProcessType.HOST_CONTROLLER);
    }

    @Override
    protected void initModel(org.jboss.as.controller.ManagementModel managementModel) {
        // No model setup required for this test
    }

    /**
     * Verifies the full wiring used by DomainModelControllerService before startServers():
     *
     *   RuntimeExpressionResolver rte = ...;
     *   rte.setCapabilityServiceSupport(getCapabilityServiceSupport());   // real CSS from controller
     *
     * Then simulates the HC server-boot path:
     *
     *   rte.resolveExpressions(node)   // no OperationContext
     *
     * The extension's resolveExpression(String, CapabilityServiceSupport) is called with the real
     * CSS and returns the expected value.
     */
    @Test
    public void testExtensionExpressionResolvesWithRealCapabilityServiceSupport()
            throws OperationFailedException {

        // Obtain a real CapabilityServiceSupport from the running controller —
        // same call that DomainModelControllerService makes before startServers().
        CapabilityServiceSupport css = getControllerService().getCapabilityServiceSupport();

        RuntimeExpressionResolver resolver = new RuntimeExpressionResolver();
        resolver.setCapabilityServiceSupport(css);
        resolver.addResolverExtension(new TestExtension());

        ModelNode result = resolver.resolveExpressions(new ModelNode(new ValueExpression(EXTENSION_EXPR)));

        assertEquals("Extension expression must be resolved via the CSS path even without an OC",
                RESOLVED_VALUE, result.asString());
    }

    /**
     * Extension that resolves {@value #EXTENSION_EXPR} using the CSS path.
     * Does not require a real capability lookup — demonstrates that the CSS object
     * is passed correctly through the resolution chain.
     */
    private static class TestExtension implements ExpressionResolverExtension {

        @Override
        public void initialize(OperationContext context) throws OperationFailedException {
            // no-op
        }

        @Override
        public String resolveExpression(String expression, OperationContext context) {
            return null;
        }

        @Override
        public String resolveExpression(String expression, CapabilityServiceSupport serviceSupport) {
            if (EXTENSION_EXPR.equals(expression) && serviceSupport != null) {
                return RESOLVED_VALUE;
            }
            return null;
        }
    }
}
