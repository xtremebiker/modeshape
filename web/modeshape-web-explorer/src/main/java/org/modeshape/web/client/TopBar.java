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

import com.smartgwt.client.types.LayoutPolicy;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.toolbar.ToolStripButton;
import com.smartgwt.client.widgets.toolbar.ToolStripMenuButton;

/**
 *
 * @author kulikov
 */
public class TopBar extends HLayout {
    
    public TopBar() {
        super();        

        setHeight(50);
        
        Img logo = new Img();
        logo.setSrc("icons/logo.png");
        
        logo.setHeight(30);
        logo.setWidth(190);
        logo.setValign(VerticalAlignment.CENTER);
        
        
        ToolStrip strip = new ToolStrip();
        strip.setHeight(50);
        strip.setWidth100();
        
        strip.addSpacer(120);
        strip.addMember(logo);
        strip.addSpacer(10);
        strip.addSeparator();
        
//        Img homeImg = new Img();
//        homeImg.setSrc("icons/bullet_blue.png");
//        homeImg.setWidth(5);
//        homeImg.setHeight(5);
                
//        strip.addMember(homeImg);
//        strip.addSeparator();
        
        strip.addFill();
        
        
        VLayout p = new VLayout();
        p.setAlign(VerticalAlignment.CENTER);
        DynamicForm form = new DynamicForm();
        
//        form.setNumCols(1);
        p.addMember(form);
        p.setWidth(300);
        
        TextItem path = new TextItem();
        path.setTitle("Path");
        path.setWidth(300);
        path.setValue("");
        path.setTop(30);
        
        form.setItems(path);
        
        strip.setAlign(VerticalAlignment.CENTER);
        strip.addMember(p);
       
        ToolStripButton go = new ToolStripButton();
        go.setTitle("Go");
        
        strip.addButton(go);
        
        Label userName = new Label();
        userName.setContents("okulikov");
        userName.setIcon("icons/bullet_blue.png");
        
        ToolStripButton repoInfo = new ToolStripButton();
        repoInfo.setTitle("Repository information");

        ToolStripButton content = new ToolStripButton();
        content.setTitle("Content");
        
        ToolStripButton query = new ToolStripButton();
        query.setTitle("Query");
        
        strip.addSeparator();
        
        strip.addSpacer(70);
        strip.addButton(repoInfo);
        strip.addButton(content);
        strip.addButton(query);
        strip.addSpacer(70);
        
        strip.addSeparator();
        strip.addMember(userName);
        
        ToolStripButton logout = new ToolStripButton();
        logout.setTitle("Log out");
        
        ToolStripButton save = new ToolStripButton();
        save.setTitle("Save");
        
       
        strip.addButton(logout);
        strip.addButton(save);
        
        addMember(strip);
        setBackgroundColor("#d3d3d3");
        
    }
}
