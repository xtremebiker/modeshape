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
package org.modeshape.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import java.util.Collection;
import java.util.List;
import org.modeshape.web.shared.JcrNode;

/**
 *
 * @author kulikov
 */
public class NodeBrowser extends VLayout{
    
    private Console console;
    
//    private JcrURL jcrURL = new JcrURL();
//    private HtmlHistory htmlHistory = new HtmlHistory();
    private final ListGrid grid = new ListGrid();
    
    public NodeBrowser(Console console) {
        super();
        this.console = console;
        
        
        addMember(grid);

        ListGridField iconField = new ListGridField("icon", " ");
        iconField.setType(ListGridFieldType.IMAGE);
        iconField.setImageURLPrefix("icons/bullet_");
        iconField.setImageURLSuffix(".png");
        iconField.setWidth(20);

        ListGridField nameField = new ListGridField("name", "Name");
        nameField.setCanEdit(false);
        nameField.setShowHover(true);

        ListGridField primaryTypeField = new ListGridField("primaryType", "Primary Type");
        primaryTypeField.setCanEdit(false);
        primaryTypeField.setShowHover(true);

        ListGridField pathField = new ListGridField("path", "Path");
        pathField.setCanEdit(false);
        pathField.setShowHover(true);
        pathField.addRecordClickHandler(new RecordClickHandler() {

            @Override
            public void onRecordClick(RecordClickEvent event) {
                final String path = grid.getSelectedRecord().getAttribute("path");
                NodeBrowser.this.console.jcrService.childNodes(path, new AsyncCallback<List<JcrNode>> () {

                    @Override
                    public void onFailure(Throwable caught) {
                        SC.say(caught.getMessage());
                    }

                    @Override
                    public void onSuccess(List<JcrNode> result) {
                        updateAddressBar(path);
                        displayChildren(result, path);
                    }
                });
            }
        });
        grid.setFields(iconField, nameField, primaryTypeField, pathField);
/*        grid.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final String path = grid.getSelectedRecord().getAttribute("path");
                NodeBrowser.this.console.jcrService.childNodes(path, new AsyncCallback<List<JcrNode>> () {

                    @Override
                    public void onFailure(Throwable caught) {
                        SC.say(caught.getMessage());
                    }

                    @Override
                    public void onSuccess(List<JcrNode> result) {
                        SC.say("Displaying " + result.size() + " nodes");
                        updateAddressBar(path);
                        displayChildren(result, path);
                    }
                });
            }
        });
        */
    }
    
    public void init(final String path) {
        NodeBrowser.this.console.jcrService.childNodes(path, new AsyncCallback<List<JcrNode>>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(List<JcrNode> result) {
                updateAddressBar(path);
                displayChildren(result, "/");
            }
        });
    }
    
    /**
     * Displays given.
     * 
     * The path of the node is reflected in the address bar while child nodes
     * are listed in the table.
     * 
     * @param node the node to display.
     */
    public void display(JcrNode node) {
        updateAddressBar(node.getPath());
        displayChildren(node.children(), "/");
    }
    
    /**
     * Modifies address bar.
     * 
     * @param path 
     */
    private void updateAddressBar(String path) {
//        console.pathNavigator.setPath(path);
        console.jcrURL.setPath(path);
        console.htmlHistory.newItem(console.jcrURL.toString(), true);
    }
    
    /**
     * List childrens in the table.
     * 
     * @param children 
     */
    private void displayChildren(Collection<JcrNode> children, String path) {
//        if (children.isEmpty()) {
//            displayEmptySet();
//            return;
//        }
        
        ListGridRecord[] data = new ListGridRecord[children.size() + 1];
        displayEmptySet(data, path);
        int i = 1;
        for (JcrNode child : children) {
            data[i] = new ListGridRecord();
            data[i].setAttribute("icon", "blue");
            data[i].setAttribute("name", child.getName());
            data[i].setAttribute("path", child.getPath());
            data[i].setAttribute("primaryType", child.getPrimaryType());
            i++;
        }
        grid.setData(data);
    }
    
    private void displayEmptySet(ListGridRecord[] data, String path) {
        data[0] = new ListGridRecord();
        data[0].setAttribute("icon", "blue");
        data[0].setAttribute("name", "../");
        data[0].setAttribute("path", parent(path));
        data[0].setAttribute("primaryType", "");
    }
    
    private String parent( String path ) {
        if (path == null) {
            return "/";
        }
        
        path = path.substring(0, path.lastIndexOf('/'));
        if (path.length() == 0) {
            return "/";
        }
        
        return path;
    }
    
}
