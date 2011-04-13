package org.kohsuke.stapler;

import com.google.common.collect.MapMaker;

import java.net.URL;
import java.util.Map;

/**
 * Convenient base class for caching loaded scripts.
 * @author Kohsuke Kawaguchi
 */
public abstract class CachingScriptLoader<S, E extends Exception> {
    /**
     * Compiled scripts of this class.
     * Access needs to be synchronized.
     *
     * <p>
     * Jelly leaks memory (because Scripts hold on to Tag)
     * which usually holds on to JellyContext that was last used to run it,
     * which often holds on to some big/heavy objects.)
     *
     * So it's important to allow Scripts to be garbage collected.
     * This is not an ideal fix, but it works.
     *
     * {@link Optional} is used as Google Collection doesn't allow null values in a map.
     */
    private final Map<String,Optional<S>> scripts = new MapMaker().softValues().makeComputingMap(new com.google.common.base.Function<String, Optional<S>>() {
        public Optional<S> apply(String from) {
            try {
                return Optional.create(loadScript(from));
            } catch (RuntimeException e) {
                throw e;    // pass through
            } catch (Exception e) {
                throw new ScriptLoadException(e);
            }
        }
    });

    /**
     * Locates the view script of the given name.
     *
     * @param name
     *      if this is a relative path, such as "foo.jelly" or "foo/bar.groovy",
     *      then it is assumed to be relative to this class, so
     *      "org/acme/MyClass/foo.jelly" or "org/acme/MyClass/foo/bar.groovy"
     *      will be searched.
     *      <p>
     *      If this starts with "/", then it is assumed to be absolute,
     *      and that name is searched from the classloader. This is useful
     *      to do mix-in.
     */
    public S findScript(String name) throws E {
        if (MetaClass.NO_CACHE)
            return loadScript(name);
        else
            return scripts.get(name).get();
    }

    protected abstract S loadScript(String name) throws E;

    /**
     * Discards the cached script.
     */
    public synchronized void clearScripts() {
        scripts.clear();
    }

    /**
     * Wraps the {@link #getResource(String, ClassLoader)} and make it support debug loader.
     */
    protected final URL findResource(String name, ClassLoader cl) {
        URL res = null;
        if (MetaClassLoader.debugLoader != null)
            res = getResource(name, MetaClassLoader.debugLoader.loader);
        if (res == null)
            res = getResource(name, cl);
        return res;
    }

    protected abstract URL getResource(String name, ClassLoader cl);
}
