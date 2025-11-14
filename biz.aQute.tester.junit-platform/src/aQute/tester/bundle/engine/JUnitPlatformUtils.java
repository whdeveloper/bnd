package aQute.tester.bundle.engine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * Do not use or refer to any classes from {@code org.junit.platform.launcher} package in here.
 * This utility performs reflective lookups lazily and caches results.
 */
public class JUnitPlatformUtils {

	private JUnitPlatformUtils() {
	}

	private static final String[] CREATE_PARAMETER_TYPES5 = new String[]{
		"org.junit.platform.engine.TestDescriptor",
		"org.junit.platform.engine.EngineExecutionListener",
		"org.junit.platform.engine.ConfigurationParameters"
	};
	private static final String[] CREATE_PARAMETER_TYPES513 = new String[]{
		"org.junit.platform.engine.OutputDirectoryProvider",
		"org.junit.platform.engine.support.store.NamespacedHierarchicalStore"
	};
	private static final String[] CREATE_PARAMETER_TYPES514 = new String[]{
		"org.junit.platform.engine.OutputDirectoryCreator",
		"org.junit.platform.engine.support.store.NamespacedHierarchicalStore"
	};

	private static final Class<?> EXECUTION_REQUEST_CLASS = forName("org.junit.platform.engine.ExecutionRequest");
	private static final Method GET_OUTPUT_DIRECTORY_PROVIDER = findInstanceMethod(EXECUTION_REQUEST_CLASS, "getOutputDirectoryProvider");
	private static final Method GET_STORE = findInstanceMethod(EXECUTION_REQUEST_CLASS, "getStore");
	private static final Method EXECUTION_REQUEST_CREATE = createExecutionRequest();

	private static Class<?> forName(String className) {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	private static Method findMethod(Class<?> cls, Predicate<Method> predicate) {
		if (cls == null) {
			return null;
		}
		for (Method m : cls.getMethods()) {
			if (predicate.test(m)) {
				try {
					m.setAccessible(true);
				} catch (SecurityException ignored) {
				}
				return m;
			}
		}
		return null;
	}

	private static Method findInstanceMethod(Class<?> cls, String methodName) {
		return findMethod(cls, m -> methodName.equals(m.getName()) && m.getParameterCount() == 0);
	}

	private static String[] concat(String[] a, String[] b) {
		String[] r = new String[a.length + b.length];
		System.arraycopy(a, 0, r, 0, a.length);
		System.arraycopy(b, 0, r, a.length, b.length);
		return r;
	}

	private static Method createExecutionRequest() {
		Method execCreate = null;
		if (EXECUTION_REQUEST_CLASS != null) {
			if (GET_STORE != null && GET_OUTPUT_DIRECTORY_PROVIDER != null) {
				execCreate = findMethod(
					EXECUTION_REQUEST_CLASS,
					meth -> "create".equals(meth.getName())
						&& meth.getParameterCount() == 5
						&& Arrays.equals(
							Arrays.stream(meth.getParameterTypes()).map(Class::getName).toArray(),
							concat(CREATE_PARAMETER_TYPES5, CREATE_PARAMETER_TYPES514)));
				if (execCreate == null) {
					execCreate = findMethod(
						EXECUTION_REQUEST_CLASS,
						meth -> "create".equals(meth.getName())
							&& meth.getParameterCount() == 5
							&& Arrays.equals(
								Arrays.stream(meth.getParameterTypes()).map(Class::getName).toArray(),
								concat(CREATE_PARAMETER_TYPES5, CREATE_PARAMETER_TYPES513)));
				}
			} else {
				execCreate = findMethod(
					EXECUTION_REQUEST_CLASS,
					meth -> "create".equals(meth.getName())
						&& meth.getParameterCount() == 3
						&& Arrays.equals(
							Arrays.stream(meth.getParameterTypes()).map(Class::getName).toArray(),
							CREATE_PARAMETER_TYPES5));
			}
		}
		return execCreate;
	}

	/**
	 * Create an ExecutionRequest in a JUnit-version-agnostic way. All parameters are typed as Object to
	 * avoid a compile-time dependency on a specific JUnit Platform version.
	 *
	 * @param request a JUnit ExecutionRequest instance (as Object)
	 * @param listener EngineExecutionListener instance (as Object)
	 * @param descriptor TestDescriptor instance (as Object)
	 * @param params ConfigurationParameters instance (as Object)
	 * @return a new ExecutionRequest instance (as Object) or null if creation isn't possible
	 */
	public static Object createExecutionRequest(Object request, Object listener, Object descriptor, Object params) {
		if (EXECUTION_REQUEST_CREATE == null) {
			return null;
		}

		try {
			if (GET_STORE != null && GET_OUTPUT_DIRECTORY_PROVIDER != null) {
				Object provider = GET_OUTPUT_DIRECTORY_PROVIDER.invoke(request);
				Object store = GET_STORE.invoke(request);
				return EXECUTION_REQUEST_CREATE.invoke(null, descriptor, listener, params, provider, store);
			} else {
				return EXECUTION_REQUEST_CREATE.invoke(null, descriptor, listener, params);
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (InvocationTargetException ite) {
			throw new RuntimeException(ite.getCause() != null ? ite.getCause() : ite);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}
}
