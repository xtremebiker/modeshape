/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.connector.inmemory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.Reference;
import org.jboss.dna.graph.property.UuidFactory;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.property.Path.Segment;
import org.jboss.dna.graph.property.basic.RootPath;
import org.jboss.dna.graph.request.CopyBranchRequest;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.DeleteBranchRequest;
import org.jboss.dna.graph.request.MoveBranchRequest;
import org.jboss.dna.graph.request.ReadAllChildrenRequest;
import org.jboss.dna.graph.request.ReadAllPropertiesRequest;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;
import org.jboss.dna.graph.request.processor.RequestProcessor;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class InMemoryRepository {

    protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final String name;
    private final UUID rootNodeUuid;
    private final Map<UUID, InMemoryNode> nodesByUuid = new HashMap<UUID, InMemoryNode>();

    public InMemoryRepository( String name,
                               UUID rootNodeUUID ) {
        CheckArg.isNotNull(rootNodeUUID, "rootNodeUUID");
        CheckArg.isNotEmpty(name, "name");
        this.name = name;
        this.rootNodeUuid = rootNodeUUID;
        // Create the root node ...
        InMemoryNode root = new InMemoryNode(rootNodeUUID);
        nodesByUuid.put(root.getUuid(), root);
    }

    /**
     * @return lock
     */
    public ReadWriteLock getLock() {
        return lock;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    public InMemoryNode getRoot() {
        return nodesByUuid.get(this.rootNodeUuid);
    }

    public InMemoryNode getNode( UUID uuid ) {
        assert uuid != null;
        return nodesByUuid.get(uuid);
    }

    protected Map<UUID, InMemoryNode> getNodesByUuid() {
        return nodesByUuid;
    }

    public InMemoryNode getNode( ExecutionContext context,
                                 String path ) {
        assert context != null;
        assert path != null;
        return getNode(context.getValueFactories().getPathFactory().create(path));
    }

    /**
     * Find a node with the given path.
     * 
     * @param path the path to the node; may not be null
     * @return the node with the path, or null if the node does not exist
     */
    public InMemoryNode getNode( Path path ) {
        assert path != null;
        InMemoryNode node = getRoot();
        for (Path.Segment segment : path) {
            InMemoryNode desiredChild = null;
            for (InMemoryNode child : node.getChildren()) {
                if (child == null) continue;
                Path.Segment childName = child.getName();
                if (childName == null) continue;
                if (childName.equals(segment)) {
                    desiredChild = child;
                    break;
                }
            }
            if (desiredChild != null) {
                node = desiredChild;
            } else {
                return null;
            }
        }
        return node;
    }

    /**
     * Find the lowest existing node along the path.
     * 
     * @param path the path to the node; may not be null
     * @return the lowest existing node along the path, or the root node if no node exists on the path
     */
    public Path getLowestExistingPath( Path path ) {
        assert path != null;
        InMemoryNode node = getRoot();
        int segmentNumber = 0;
        for (Path.Segment segment : path) {
            InMemoryNode desiredChild = null;
            for (InMemoryNode child : node.getChildren()) {
                if (child == null) continue;
                Path.Segment childName = child.getName();
                if (childName == null) continue;
                if (childName.equals(segment)) {
                    desiredChild = child;
                    break;
                }
            }
            if (desiredChild != null) {
                node = desiredChild;
            } else {
                return path.subpath(0, segmentNumber);
            }
            ++segmentNumber;
        }
        return RootPath.INSTANCE;
    }

    protected UUID generateUuid() {
        return UUID.randomUUID();
    }

    public void removeNode( ExecutionContext context,
                            InMemoryNode node ) {
        assert context != null;
        assert node != null;
        assert getRoot().equals(node) != true;
        InMemoryNode parent = node.getParent();
        assert parent != null;
        parent.getChildren().remove(node);
        correctSameNameSiblingIndexes(context, parent, node.getName().getName());
        removeUuidReference(node);
    }

    protected void removeUuidReference( InMemoryNode node ) {
        nodesByUuid.remove(node.getUuid());
        for (InMemoryNode child : node.getChildren()) {
            removeUuidReference(child);
        }
    }

    /**
     * Create a node at the supplied path. The parent of the new node must already exist.
     * 
     * @param context the environment; may not be null
     * @param pathToNewNode the path to the new node; may not be null
     * @return the new node (or root if the path specified the root)
     */
    public InMemoryNode createNode( ExecutionContext context,
                                    String pathToNewNode ) {
        assert context != null;
        assert pathToNewNode != null;
        Path path = context.getValueFactories().getPathFactory().create(pathToNewNode);
        if (path.isRoot()) return getRoot();
        Path parentPath = path.getParent();
        InMemoryNode parentNode = getNode(parentPath);
        Name name = path.getLastSegment().getName();
        return createNode(context, parentNode, name, null);
    }

    /**
     * Create a new node with the supplied name, as a child of the supplied parent.
     * 
     * @param context the execution context
     * @param parentNode the parent node; may not be null
     * @param name the name; may not be null
     * @param uuid the UUID of the node, or null if the UUID is to be generated
     * @return the new node
     */
    public InMemoryNode createNode( ExecutionContext context,
                                    InMemoryNode parentNode,
                                    Name name,
                                    UUID uuid ) {
        assert context != null;
        assert name != null;
        if (parentNode == null) parentNode = getRoot();
        if (uuid == null) uuid = generateUuid();
        InMemoryNode node = new InMemoryNode(uuid);
        nodesByUuid.put(node.getUuid(), node);
        node.setParent(parentNode);
        Path.Segment newName = context.getValueFactories().getPathFactory().createSegment(name);
        node.setName(newName);
        parentNode.getChildren().add(node);
        correctSameNameSiblingIndexes(context, parentNode, name);
        return node;
    }

    protected void correctSameNameSiblingIndexes( ExecutionContext context,
                                                  InMemoryNode parentNode,
                                                  Name name ) {
        if (parentNode == null) return;
        // Look for the highest existing index ...
        List<InMemoryNode> childrenWithSameNames = new LinkedList<InMemoryNode>();
        for (InMemoryNode child : parentNode.getChildren()) {
            if (child.getName().getName().equals(name)) childrenWithSameNames.add(child);
        }
        if (childrenWithSameNames.size() == 0) return;
        if (childrenWithSameNames.size() == 1) {
            InMemoryNode childWithSameName = childrenWithSameNames.get(0);
            Path.Segment newName = context.getValueFactories().getPathFactory().createSegment(name, Path.NO_INDEX);
            childWithSameName.setName(newName);
            return;
        }
        int index = 1;
        for (InMemoryNode childWithSameName : childrenWithSameNames) {
            Path.Segment segment = childWithSameName.getName();
            if (segment.getIndex() != index) {
                Path.Segment newName = context.getValueFactories().getPathFactory().createSegment(name, index);
                childWithSameName.setName(newName);
            }
            ++index;
        }
    }

    /**
     * Move the supplied node to the new parent. This method automatically removes the node from its existing parent, and also
     * correctly adjusts the {@link Path.Segment#getIndex() index} to be correct in the new parent.
     * 
     * @param context
     * @param node the node to be moved; may not be the {@link #getRoot() root}
     * @param newParent the new parent; may not be the {@link #getRoot() root}
     */
    public void moveNode( ExecutionContext context,
                          InMemoryNode node,
                          InMemoryNode newParent ) {
        assert context != null;
        assert newParent != null;
        assert node != null;
        assert getRoot().equals(newParent) != true;
        assert getRoot().equals(node) != true;
        InMemoryNode oldParent = node.getParent();
        if (oldParent != null) {
            if (oldParent.equals(newParent)) return;
            boolean removed = oldParent.getChildren().remove(node);
            assert removed == true;
            node.setParent(null);
            correctSameNameSiblingIndexes(context, oldParent, node.getName().getName());
        }
        node.setParent(newParent);
        newParent.getChildren().add(node);
        correctSameNameSiblingIndexes(context, newParent, node.getName().getName());
    }

    /**
     * This should copy the subgraph given by the original node and place the new copy under the supplied new parent. Note that
     * internal references between nodes within the original subgraph must be reflected as internal nodes within the new subgraph.
     * 
     * @param context
     * @param original
     * @param newParent
     * @param desiredName
     * @param recursive
     * @param oldToNewUuids the map of UUIDs of nodes in the new subgraph keyed by the UUIDs of nodes in the original; may not be
     *        null
     * @return the new node, which is the top of the new subgraph
     */
    public InMemoryNode copyNode( ExecutionContext context,
                                  InMemoryNode original,
                                  InMemoryNode newParent,
                                  Name desiredName,
                                  boolean recursive,
                                  Map<UUID, UUID> oldToNewUuids ) {
        assert context != null;
        assert original != null;
        assert newParent != null;
        assert oldToNewUuids != null;

        // Get or create the new node ...
        Name childName = desiredName != null ? desiredName : original.getName().getName();
        InMemoryNode copy = createNode(context, newParent, childName, null);
        oldToNewUuids.put(original.getUuid(), copy.getUuid());

        // Copy the properties ...
        copy.getProperties().clear();
        copy.getProperties().putAll(original.getProperties());
        if (recursive) {
            // Loop over each child and call this method ...
            for (InMemoryNode child : original.getChildren()) {
                copyNode(context, child, copy, null, true, oldToNewUuids);
            }
        }

        // Now, adjust any references in the new subgraph to objects in the original subgraph
        // (because they were internal references, and need to be internal to the new subgraph)
        PropertyFactory propertyFactory = context.getPropertyFactory();
        UuidFactory uuidFactory = context.getValueFactories().getUuidFactory();
        ValueFactory<Reference> referenceFactory = context.getValueFactories().getReferenceFactory();
        for (Map.Entry<UUID, UUID> oldToNew : oldToNewUuids.entrySet()) {
            InMemoryNode oldNode = nodesByUuid.get(oldToNew.getKey());
            InMemoryNode newNode = nodesByUuid.get(oldToNew.getValue());
            assert oldNode != null;
            assert newNode != null;
            // Iterate over the properties of the new ...
            for (Map.Entry<Name, Property> entry : newNode.getProperties().entrySet()) {
                Property property = entry.getValue();
                // Now see if any of the property values are references ...
                List<Object> newValues = new ArrayList<Object>();
                boolean foundReference = false;
                for (Iterator<?> iter = property.getValues(); iter.hasNext();) {
                    Object value = iter.next();
                    PropertyType type = PropertyType.discoverType(value);
                    if (type == PropertyType.REFERENCE) {
                        UUID oldReferencedUuid = uuidFactory.create(value);
                        UUID newReferencedUuid = oldToNewUuids.get(oldReferencedUuid);
                        if (newReferencedUuid != null) {
                            newValues.add(referenceFactory.create(newReferencedUuid));
                            foundReference = true;
                        }
                    } else {
                        newValues.add(value);
                    }
                }
                // If we found at least one reference, we have to build a new Property object ...
                if (foundReference) {
                    Property newProperty = propertyFactory.create(property.getName(), newValues);
                    entry.setValue(newProperty);
                }
            }
        }

        return copy;
    }

    /**
     * Get a request processor given the supplied environment and source name.
     * 
     * @param context the environment in which the commands are to be executed
     * @param sourceName the name of the repository source
     * @return the request processor; never null
     */
    /*package*/RequestProcessor getRequestProcessor( ExecutionContext context,
                                                      String sourceName ) {
        return new Processor(context, sourceName);
    }

    protected class Processor extends RequestProcessor {
        private final PathFactory pathFactory;
        private final PropertyFactory propertyFactory;

        protected Processor( ExecutionContext context,
                             String sourceName ) {
            super(sourceName, context);
            pathFactory = context.getValueFactories().getPathFactory();
            propertyFactory = context.getPropertyFactory();
        }

        @Override
        public void process( ReadAllChildrenRequest request ) {
            InMemoryNode node = getTargetNode(request, request.of());
            if (node == null) return;
            Location actualLocation = getActualLocation(request.of().getPath(), node);
            Path path = actualLocation.getPath();
            // Get the names of the children ...
            List<InMemoryNode> children = node.getChildren();
            for (InMemoryNode child : children) {
                Segment childName = child.getName();
                Path childPath = pathFactory.create(path, childName);
                request.addChild(childPath, propertyFactory.create(DnaLexicon.UUID, child.getUuid()));
            }
            request.setActualLocationOfNode(actualLocation);
            setCacheableInfo(request);
        }

        @Override
        public void process( ReadAllPropertiesRequest request ) {
            InMemoryNode node = getTargetNode(request, request.at());
            if (node == null) return;
            // Get the properties of the node ...
            Location actualLocation = getActualLocation(request.at().getPath(), node);
            request.addProperty(propertyFactory.create(DnaLexicon.UUID, node.getUuid()));
            for (Property property : node.getProperties().values()) {
                request.addProperty(property);
            }
            request.setActualLocationOfNode(actualLocation);
            setCacheableInfo(request);
        }

        @Override
        public void process( CopyBranchRequest request ) {
            InMemoryNode node = getTargetNode(request, request.from());
            if (node == null) return;
            // Look up the new parent, which must exist ...
            Path newParentPath = request.into().getPath();
            Name desiredName = request.desiredName();
            InMemoryNode newParent = getNode(newParentPath);
            InMemoryNode newNode = copyNode(getExecutionContext(), node, newParent, desiredName, true, new HashMap<UUID, UUID>());
            Path newPath = getExecutionContext().getValueFactories().getPathFactory().create(newParentPath, newNode.getName());
            Location oldLocation = getActualLocation(request.from().getPath(), node);
            Location newLocation = new Location(newPath, newNode.getUuid());
            request.setActualLocations(oldLocation, newLocation);
        }

        @Override
        public void process( CreateNodeRequest request ) {
            Path parent = request.under().getPath();
            CheckArg.isNotNull(parent, "request.under().getPath()");
            InMemoryNode node = null;
            // Look up the parent node, which must exist ...
            InMemoryNode parentNode = getNode(parent);
            if (parentNode == null) {
                Path lowestExisting = getLowestExistingPath(parent);
                request.setError(new PathNotFoundException(request.under(), lowestExisting,
                                                           GraphI18n.inMemoryNodeDoesNotExist.text(parent)));
            }
            UUID uuid = null;
            for (Property property : request.properties()) {
                if (property.getName().equals(DnaLexicon.UUID)) {
                    uuid = getExecutionContext().getValueFactories().getUuidFactory().create(property.getValues().next());
                    break;
                }
            }
            node = createNode(getExecutionContext(), parentNode, request.named(), uuid);
            assert node != null;
            Path path = getExecutionContext().getValueFactories().getPathFactory().create(parent, node.getName());
            // Now add the properties to the supplied node ...
            for (Property property : request.properties()) {
                Name propName = property.getName();
                if (property.size() == 0) {
                    node.getProperties().remove(propName);
                    continue;
                }
                if (!propName.equals(DnaLexicon.UUID)) {
                    node.getProperties().put(propName, property);
                }
            }
            Location actualLocation = getActualLocation(path, node);
            request.setActualLocationOfNode(actualLocation);
        }

        @Override
        public void process( DeleteBranchRequest request ) {
            InMemoryNode node = getTargetNode(request, request.at());
            if (node == null) return;
            removeNode(getExecutionContext(), node);
            Location actualLocation = getActualLocation(request.at().getPath(), node);
            request.setActualLocationOfNode(actualLocation);
        }

        @Override
        public void process( MoveBranchRequest request ) {
            InMemoryNode node = getTargetNode(request, request.from());
            if (node == null) return;
            // Look up the new parent, which must exist ...
            Path newPath = request.into().getPath();
            Path newParentPath = newPath.getParent();
            InMemoryNode newParent = getNode(newParentPath);
            node.setParent(newParent);
            newPath = getExecutionContext().getValueFactories().getPathFactory().create(newParentPath, node.getName());
            Location oldLocation = getActualLocation(request.from().getPath(), node);
            Location newLocation = new Location(newPath, node.getUuid());
            request.setActualLocations(oldLocation, newLocation);
        }

        @Override
        public void process( UpdatePropertiesRequest request ) {
            InMemoryNode node = getTargetNode(request, request.on());
            if (node == null) return;
            // Now set (or remove) the properties to the supplied node ...
            for (Property property : request.properties()) {
                Name propName = property.getName();
                if (property.size() == 0) {
                    node.getProperties().remove(propName);
                    continue;
                }
                if (!propName.equals(DnaLexicon.UUID)) {
                    node.getProperties().put(propName, property);
                }
            }
            Location actualLocation = getActualLocation(request.on().getPath(), node);
            request.setActualLocationOfNode(actualLocation);
        }

        protected Location getActualLocation( Path path,
                                              InMemoryNode node ) {
            if (path == null) {
                // Find the path on the node ...
                LinkedList<Path.Segment> segments = new LinkedList<Path.Segment>();
                InMemoryNode n = node;
                while (n != null) {
                    if (n.getParent() == null) break;
                    segments.addFirst(n.getName());
                    n = n.getParent();
                }
                path = pathFactory.createAbsolutePath(segments);
            }
            return new Location(path, node.getUuid());
        }

        protected InMemoryNode getTargetNode( Request request,
                                              Location location ) {
            // Check first for the UUID ...
            InMemoryNode node = null;
            UUID uuid = location.getUuid();
            if (uuid != null) {
                node = InMemoryRepository.this.getNode(uuid);
            }
            Path path = null;
            if (node == null) {
                // Look up the node with the supplied path ...
                path = location.getPath();
                if (path != null) {
                    node = InMemoryRepository.this.getNode(path);
                }
            }
            if (node == null) {
                if (path == null) {
                    if (uuid == null) {
                        // Missing both path and UUID ...
                        I18n msg = GraphI18n.inMemoryConnectorRequestsMustHavePathOrUuid;
                        request.setError(new IllegalArgumentException(msg.text()));
                        return null;
                    }
                    // Missing path, and could not find by UUID ...
                    request.setError(new PathNotFoundException(location, pathFactory.createRootPath(),
                                                               GraphI18n.inMemoryNodeDoesNotExist.text(path)));
                    return null;
                }
                // Could not find the node given the supplied path, so find the lowest path that does exist ...
                Path lowestExisting = getLowestExistingPath(path);
                request.setError(new PathNotFoundException(location, lowestExisting,
                                                           GraphI18n.inMemoryNodeDoesNotExist.text(path)));
            }
            return node;
        }
    }
}
