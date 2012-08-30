/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr.value.binary;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.modeshape.jcr.JcrI18n;

/**
 *
 * @author kulikov
 */
public abstract class CustomBinaryStore extends AbstractBinaryStore {

    /**
     * Instantiates custom binary store
     *
     * @param conf configuration parameters of the store
     * @return Binary store instance.
     * @throws Exception if problem with underlying resource occurs
     */
    public static CustomBinaryStore newInstance(Map conf) throws Exception {
        String className = (String) conf.get("classname");
        if (className == null) {
            throw new BinaryStoreException(JcrI18n.missingVariableValue.text("classname"));
        }
        
        Class cls = CustomBinaryStore.class.getClassLoader().loadClass(className);
        CustomBinaryStore store = (CustomBinaryStore) cls.newInstance();
        store.configure(toProperties(conf));
        return store;
    }

    private static Properties toProperties(Map<String, String> map) {
        Properties props = new Properties();
        props.putAll(map);
//        Set<String> keys = map.keySet();
//        for (String key : keys) {
//            props.setProperty(key, map.get(key));
//        }
        return props;
    }

    /**
     * Default constructor
     */
    public CustomBinaryStore() {
    }

    /**
     * Configures binary store.
     *
     * @param conf configuration parameters
     */
    public abstract void configure(Properties conf);

}
