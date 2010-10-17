/*
 * Copyright (c) 2004-2010, Kohsuke Kawaguchi
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of
 *       conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.kohsuke.stapler.bind;

import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 *
 * TODO: think about some kind of eviction strategy, beyond the session eviction.
 * Maybe it's not necessary, I don't know.
 *
 * @author Kohsuke Kawaguchi
 */
public class BoundObjectTable {
    public Object getDynamic(String id) {
        Table t = resolve(false);
        if (t == null) return null;
        return t.resolve(id);
    }

    private Bound bind(Ref ref) {
        return resolve(true).add(ref);
    }

    /**
     * Binds an object temporarily and returns its URL.
     */
    public Bound bind(Object o) {
        return bind(strongRef(o));
    }

    /**
     * Binds an object temporarily and returns its URL.
     */
    public Bound bindWeak(Object o) {
        return bind(new WeakRef(o));
    }

    /**
     * Called from within the request handling of a bound object, to release the object explicitly.
     */
    public void releaseMe() {
        Ancestor eot = Stapler.getCurrentRequest().findAncestor(BoundObjectTable.class);
        if (eot==null)
            throw new IllegalStateException("The thread is not handling a request to a abound object");
        String id = eot.getNextToken(0);

        resolve(false).release(id); // resolve(false) can't fail because we are processing this request now.
    }

    private Table resolve(boolean createIfNotExist) {
        HttpSession session = Stapler.getCurrentRequest().getSession(createIfNotExist);
        if (session==null) return null;

        Table t = (Table) session.getAttribute(Table.class.getName());
        if (t==null) {
            if (createIfNotExist)
                session.setAttribute(Table.class.getName(), t=new Table());
            else
                return null;
        }
        return t;
    }

    /**
     * Per-session table that remembers all the bound instances.
     */
    private static class Table {
        private final Map<String,Ref> entries = new HashMap<String,Ref>();

        private synchronized Bound add(Ref ref) {
            final Object target = ref.get();
            if (target instanceof WithWellKnownURL) {
                WithWellKnownURL w = (WithWellKnownURL) target;
                return new WellKnownObjectHandle(w.getWellKnownUrl(),w);
            }

            final String id = UUID.randomUUID().toString();
            entries.put(id,ref);

            return new Bound() {
                public void release() {
                   Table.this.release(id);
                }

                public String getURL() {
                    return Stapler.getCurrentRequest().getContextPath()+PREFIX+id;
                }

                public Object getTarget() {
                    return target;
                }

                public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
                    rsp.sendRedirect2(getURL());
                }
            };
        }

        private synchronized Ref release(String id) {
            return entries.remove(id);
        }

        private synchronized Object resolve(String id) {
            Ref e = entries.get(id);
            if (e==null)    return null;
            Object v = e.get();
            if (v==null)
                entries.remove(id); // reference is already garbage collected.
            return v;
        }
    }

    private static final class WellKnownObjectHandle extends Bound {
        private final String url;
        private final Object target;

        public WellKnownObjectHandle(String url, Object target) {
            this.url = url;
            this.target = target;
        }

        /**
         * Objects with well-known URLs cannot be released, as their URL bindings are controlled
         * implicitly by the application.
         */
        public void release() {
        }

        public String getURL() {
            return Stapler.getCurrentRequest().getContextPath()+url;
        }

        @Override
        public Object getTarget() {
            return target;
        }

        public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
            rsp.sendRedirect2(getURL());
        }
    }


    /**
     * Reference that resolves to an object.
     */
    interface Ref {
        Object get();
    }

    private static Ref strongRef(final Object o) {
        return new Ref() {
            public Object get() {
                return o;
            }
        };
    }
    
    private static class WeakRef extends WeakReference implements Ref {
        private WeakRef(Object referent) {
            super(referent);
        }
    }

    public static final String PREFIX = "/$stapler/bound/";
}
