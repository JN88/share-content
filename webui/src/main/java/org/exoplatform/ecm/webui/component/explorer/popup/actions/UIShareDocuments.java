/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.webui.component.explorer.popup.actions;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.commons.EventUIComponent;
import org.exoplatform.webui.commons.EventUIComponent.EVENTTYPE;
import org.exoplatform.webui.commons.UISpacesSwitcher;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.popup.service.IShareDocumentService;
import org.exoplatform.ecm.webui.component.explorer.popup.service.ShareDocumentService;
import org.exoplatform.ecm.webui.utils.Utils;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 18, 2014 
 */
@ComponentConfig(
                 lifecycle = UIFormLifecycle.class,
                 template =  "classpath:templates/UIShareDocuments.gtmpl",
                 events = {
                   @EventConfig(listeners = UIShareDocuments.ShareActionListener.class),
                   @EventConfig(listeners = UIShareDocuments.CancelActionListener.class),
                   @EventConfig(listeners = UIShareDocuments.RemoveSpaceActionListener.class),
                   @EventConfig(listeners = UIShareDocuments.SelectSpaceActionListener.class, phase=Phase.PROCESS)
                 }
    )
public class UIShareDocuments extends UIForm implements UIPopupComponent{
  private static final Log    LOG                 = ExoLogger.getLogger(UIShareDocuments.class);
  public static class RemoveSpaceActionListener extends EventListener<UIShareDocuments>{

    @Override
    public void execute(Event<UIShareDocuments> event) throws Exception {
      UIShareDocuments uiform = event.getSource();
      uiform.spaces.remove(event.getRequestContext().getRequestParameter(OBJECTID).toString());
      uiform.comment = event.getSource().getChild(UIFormTextAreaInput.class).getValue();
    }

  }

  public static class CancelActionListener extends EventListener<UIShareDocuments>{

    @Override
    public void execute(Event<UIShareDocuments> event) throws Exception {
      event.getSource().getAncestorOfType(UIJCRExplorer.class).cancelAction() ;
    }

  }

  public static class SelectSpaceActionListener extends EventListener<UIShareDocuments>{

    @Override
    public void execute(Event<UIShareDocuments> event) throws Exception {
      List<String> spaces = event.getSource().spaces;
      event.getSource().comment = event.getSource().getChild(UIFormTextAreaInput.class).getValue();
      String space = event.getRequestContext().getRequestParameter(UISpacesSwitcher.SPACE_ID_PARAMETER).toString();
      space = space.split("/")[2];
      if(!spaces.contains(space))
        spaces.add(space);
      
    }

  }

  public static class ShareActionListener extends EventListener<UIShareDocuments>{

    @Override
    public void execute(Event<UIShareDocuments> event) throws Exception {
      if(event.getSource().spaces.size() > 0){
        IShareDocumentService service = (IShareDocumentService)PortalContainer.getInstance().getComponentInstance(ShareDocumentService.class);
        List<String> spaces = event.getSource().spaces;
        Node node = event.getSource().getNode();

        for(String space : spaces){
          if(space.equals("")) continue;
          else service.publicDocumentToSpace(space,node,event.getSource().getChild(UIFormTextAreaInput.class).getValue(),event.getSource().getChild(UIFormSelectBox.class).getValue());
        }
        event.getSource().getAncestorOfType(UIJCRExplorer.class).cancelAction() ;
      }


      
    }

  }
  public UIShareDocuments(){
    try {
      addChild(UISpacesSwitcher.class, null, "SpaceSwitcher");

      getChild(UISpacesSwitcher.class).setShowPortalSpace(false);
      getChild(UISpacesSwitcher.class).setShowUserSpace(false);
      //      getSpace().setCurrentSpaceName("Select a space");
      EventUIComponent temp = new EventUIComponent("UIShareDocuments","SelectSpace",EVENTTYPE.EVENT);      
      getSpace().init(temp);
      ArrayList<SelectItemOption<String>> permOption = new ArrayList<SelectItemOption<String>>();
      //      permOption.add(new SelectItemOption<String>("Can view", PermissionType.READ));
      //      permOption.add(new SelectItemOption<String>("Can modify", PermissionType.SET_PROPERTY +","+PermissionType.ADD_NODE));

      addChild(new UIFormSelectBox("permissionDropDown", "permissionDropDown", permOption));
      getChild(UIFormSelectBox.class).getOptions().add(new SelectItemOption<String>("Can modify", "modify"));
      getChild(UIFormSelectBox.class).getOptions().add(new SelectItemOption<String>("Can view", "read"));
      addChild(new UIFormTextAreaInput("textAreaInput", "textAreaInput", ""));
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
  }
  private String nodePath;
  List<String> spaces = new ArrayList<String>();
  public String comment = "";
  private String repoName;
  public String getDocumentName(){
    String[] arr = nodePath.split("/");
    return arr[arr.length - 1];
  }
  public Node getNode(){
    ShareDocumentService service = (ShareDocumentService)PortalContainer.getInstance().getComponentInstance(ShareDocumentService.class);
    return service.getNodeByPath(repoName,nodePath);

  }
  public String getFileExtension(){
    int index = nodePath.lastIndexOf('.');
    if (index != -1) {
      return nodePath.substring(index);
    } else {
      return "";
    }
  }
  public String getIconURL(){
    try {
      return Utils.getNodeTypeIcon(getNode(), "uiIcon24x24");
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    }
    return null;
  }

  @Override
  public void activate() {



  }
  public UISpacesSwitcher getSpace(){
    return getChild(UISpacesSwitcher.class);
  }
  public List<String> getSpaces(){return spaces;}
  public String getComment(){
    return this.comment;
  }
  @Override
  public void deActivate() {}

  public void setSelectedNode(String nodePath) {
    if(nodePath.contains(":")){
      this.nodePath = nodePath.split(":")[1];
      this.repoName = nodePath.split(":")[0];
    }else this.nodePath = nodePath;
  }
}
