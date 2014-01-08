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

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import java.util.ArrayList;

/**
 *
 * @author kulikov
 */
public class PathNavigator extends HLayout {
    private ArrayList<Segment> segments = new ArrayList();
    
    public PathNavigator() {
        super();
        setWidth100();
        setMargin(5);
        setHeight(25);
        setAlign(Alignment.LEFT);
    }
    
    public void setPath(String path) {
        clean();
        String[] tokens = path.split("/");
        
        //expand number of segments
        int dif = tokens.length;
        for (int i = 0; i < dif; i++) {
            Segment s = new Segment("/" + tokens[i]);
            addMember(s);
            segments.add(s);
        }
        
        //update segments
//        for (int i = 0; i < tokens.length; i++) {
//            segments.get(i).setSegment("/" + tokens[i]);
//        }
        
        this.reflowNow();
        for (int i = 0; i < tokens.length; i++) {
            segments.get(i).redraw();
        }
        
        this.redraw();
//        SC.say("Redraw");
    }
    
    private void clean() {
        for (Segment segment : segments) {
            this.removeMember(segment);
        }
        segments.clear();
//        for (Segment segment : segments) {
//            segment.setVisible(false);
//        }
    }
    
    private class Segment extends Label {

        public Segment(String segment) {
            super(segment);
            this.setAutoWidth();
            this.setAlign(Alignment.LEFT);
            this.addStyleName("clickable");
            
//            this.getElement().getStyle().setCursor(Cursor.DEFAULT);
            this.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    SC.say("Hello");
                }
            });
        }
        
        public void setSegment(String segment) {
            setContents("<span style=\"color:blue;font-size:100%;font-family:serif\">" + segment + "</span>");
//            setContents("<a href=\"\">" + segment + "</a>");
            setVisible(true);
            markForRedraw();
        }
    }
}
