package org.jahia.modules.elasticsearchconnector.http;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * A class loader that always looks inside its URLs before delegating to the parent classloader.
 */
public class ChildFirstClassLoader extends ClassLoader {

    private ChildFirstURLClassLoader childFirstURLClassLoader;

    private static class ChildFirstURLClassLoader extends URLClassLoader {
        private ClassLoader parentClassLoader;

        public ChildFirstURLClassLoader(URL[] urls, ClassLoader parentClassLoader) {
            super(urls, null);

            this.parentClassLoader = parentClassLoader;
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                return super.findClass(name);
            } catch (ClassNotFoundException e) {
                return parentClassLoader.loadClass(name);
            }
        }
    }

    public ChildFirstClassLoader(ClassLoader parent, URL[] urls) {
        super(parent);
        childFirstURLClassLoader = new ChildFirstURLClassLoader(urls, parent);
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        try {
            return childFirstURLClassLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            return super.loadClass(name, resolve);
        }
    }

}