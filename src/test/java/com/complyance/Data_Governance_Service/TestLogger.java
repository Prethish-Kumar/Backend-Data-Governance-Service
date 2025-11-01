package com.complyance.Data_Governance_Service;

import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

public class TestLogger implements TestWatcher, BeforeTestExecutionCallback {

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        System.out.println("🧪 Running Test: " + context.getDisplayName());
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        System.out.println("✅ Test Passed: " + context.getDisplayName() + "\n");
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        System.out.println("❌ Test Failed: " + context.getDisplayName() + " — " + cause.getMessage() + "\n");
    }
}
