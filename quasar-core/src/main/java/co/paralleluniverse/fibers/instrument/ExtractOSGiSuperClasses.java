package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.common.resource.ClassLoaderUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ExtractOSGiSuperClasses implements PrivilegedExceptionAction<Map<String, String>> {
    private static final String BUNDLE_WIRING_CLASS_NAME = "org.osgi.framework.wiring.BundleWiring";
    private static final String BUNDLE_WIRE_CLASS_NAME = "org.osgi.framework.wiring.BundleWire";
    private static final String BUNDLE_CAPABILITY_CLASS_NAME = "org.osgi.framework.wiring.BundleCapability";
    private static final String BUNDLE_CLASS_NAME = "org.osgi.framework.Bundle";
    private static final String PACKAGE_WIRING = "osgi.wiring.package";
    private static final String JAVA_OBJECT = "java/lang/Object";

    private final String className;
    private final ClassLoader cl;

    ExtractOSGiSuperClasses(String className, ClassLoader cl) {
        this.className = className;
        this.cl = cl;
    }

    @Override
    public Map<String, String> run() throws Exception {
        final Extractor extractor;
        try {
            extractor = new Extractor(cl);
        } catch (Exception e) {
            // These exceptions mean we cannot examine the OSGi framework.
            // Most likely, because there is no OSGi framework to examine!
            return null;
        }
        return extractor.extractFor(className);
    }

    private static final class Extractor {
        private final Method getRequiredWires;
        private final Method getProviderWiring;
        private final Method getClassLoader;
        private final Method getCapability;
        private final Method getAttributes;
        private final Object initialWiring;

        Extractor(ClassLoader cl) throws ReflectiveOperationException {
            final Class<?> bundleClass = Class.forName(BUNDLE_CLASS_NAME, false, cl);
            final Method getBundle = cl.getClass().getMethod("getBundle");
            checkReturnType(getBundle, bundleClass);

            final Class<?> bundleWiringClass = Class.forName(BUNDLE_WIRING_CLASS_NAME, false, bundleClass.getClassLoader());
            getRequiredWires = bundleWiringClass.getMethod("getRequiredWires", String.class);
            checkReturnType(getRequiredWires, List.class);
            getClassLoader = bundleWiringClass.getMethod("getClassLoader");
            checkReturnType(getClassLoader, ClassLoader.class);

            final Class<?> bundleWireClass = Class.forName(BUNDLE_WIRE_CLASS_NAME, false, bundleClass.getClassLoader());
            getProviderWiring = bundleWireClass.getMethod("getProviderWiring");
            checkReturnType(getProviderWiring, bundleWiringClass);

            final Class<?> bundleCapabilityClass = Class.forName(BUNDLE_CAPABILITY_CLASS_NAME, false, bundleClass.getClassLoader());
            getCapability = bundleWireClass.getMethod("getCapability");
            checkReturnType(getCapability, bundleCapabilityClass);
            getAttributes = bundleCapabilityClass.getMethod("getAttributes");
            checkReturnType(getAttributes, Map.class);

            final Method adapt = bundleClass.getMethod("adapt", Class.class);
            Object bundle = getBundle.invoke(cl);
            initialWiring = adapt.invoke(bundle, bundleWiringClass);
        }

        Map<String, String> extractFor(String className) throws Exception {
            final Map<String, String> superClasses = new HashMap<>();
            Object bundleWiring = initialWiring;
            String packageName = "";
            while (!JAVA_OBJECT.equals(className)) {
                final String nextPackageName = getPackageName(className);
                if (!packageName.equals(nextPackageName)) {
                    // Either the package has changed, or we don't know that it
                    // hasn't changed. Check whether this bundle wiring supports
                    // our package or must be switched for one that does.
                    packageName = nextPackageName;
                    bundleWiring = getBundleWiringFor(bundleWiring, packageName);
                }

                final String superClassName = extractSuperClass(bundleWiring, className);
                if (superClassName == null) {
                    // Only java.lang.Object should have a null super class.
                    break;
                }
                superClasses.put(className, superClassName);
                className = superClassName;
            }
            return superClasses;
        }

        private String extractSuperClass(Object bundleWiring, String className) throws Exception {
            final ClassLoader cl = bundleWiring == null ? null : (ClassLoader) getClassLoader.invoke(bundleWiring);
            try (InputStream is = ClassLoaderUtil.getResourceAsStream(cl, className + ".class")) {
                return ExtractSuperClass.extractFrom(is);
            } catch (IOException e) {
                throw new FileNotFoundException(className);
            }
        }

        private Object getBundleWiringFor(Object bundleWiring, String packageName) throws ReflectiveOperationException {
            List<?> requiredWires = (List<?>) getRequiredWires.invoke(bundleWiring, PACKAGE_WIRING);
            for (Object requiredWire : requiredWires) {
                Object capability = getCapability.invoke(requiredWire);
                if (capability != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> attributes = (Map<String, String>) getAttributes.invoke(capability);
                    String wirePackage = attributes.get(PACKAGE_WIRING);
                    if (packageName.equals(wirePackage)) {
                        return getProviderWiring.invoke(requiredWire);
                    }
                }
            }
            // We haven't gone anywhere, so keep this wiring.
            return bundleWiring;
        }

        private static String getPackageName(String className) {
            int idx = className.lastIndexOf('/');
            if (idx < 0) {
                throw new IllegalArgumentException("Invalid class: " + className);
            }
            return className.substring(0, idx).replace('/', '.');
        }

        private static void checkReturnType(Method method, Class<?> expectedType) throws NoSuchMethodException {
            if (!expectedType.isAssignableFrom(method.getReturnType())) {
                throw new NoSuchMethodException("Expected " + method + " to have return type " + expectedType.getName());
            }
        }
    }
}
