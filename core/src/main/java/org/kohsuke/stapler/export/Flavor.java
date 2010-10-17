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

package org.kohsuke.stapler.export;

import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.io.Writer;

/**
 * Export flavor.
 *
 * @author Kohsuke Kawaguchi
 */
public enum Flavor {
    JSON("application/javascript;charset=UTF-8") {
        public DataWriter createDataWriter(Object bean, StaplerResponse rsp) throws IOException {
            return new JSONDataWriter(rsp);
        }
        public DataWriter createDataWriter(Object bean, Writer w) throws IOException {
            return new JSONDataWriter(w);
        }
    },
    PYTHON("text/x-python;charset=UTF-8") {
        public DataWriter createDataWriter(Object bean, StaplerResponse rsp) throws IOException {
            return new PythonDataWriter(rsp);
        }
        public DataWriter createDataWriter(Object bean, Writer w) throws IOException {
            return new PythonDataWriter(w);
        }
    },
    RUBY("text/x-ruby;charset=UTF-8") {
        public DataWriter createDataWriter(Object bean, StaplerResponse rsp) throws IOException {
            return new RubyDataWriter(rsp);
        }
        public DataWriter createDataWriter(Object bean, Writer w) throws IOException {
            return new RubyDataWriter(w);
        }
    },
    XML("application/xml;charset=UTF-8") {
        public DataWriter createDataWriter(Object bean, StaplerResponse rsp) throws IOException {
            return new XMLDataWriter(bean,rsp);
        }
        public DataWriter createDataWriter(Object bean, Writer w) throws IOException {
            return new XMLDataWriter(bean,w);
        }
    };

    /**
     * Content-type of this flavor, including charset "UTF-8".
     */
    public final String contentType;

    Flavor(String contentType) {
        this.contentType = contentType;
    }

    public abstract DataWriter createDataWriter(Object bean, StaplerResponse rsp) throws IOException;
    public abstract DataWriter createDataWriter(Object bean, Writer w) throws IOException;
}
