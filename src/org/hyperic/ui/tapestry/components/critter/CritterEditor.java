package org.hyperic.ui.tapestry.components.critter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.annotations.Persist;
import org.apache.tapestry.event.PageBeginRenderListener;
import org.apache.tapestry.event.PageEvent;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.grouping.Critter;
import org.hyperic.hq.grouping.GroupException;
import org.hyperic.hq.grouping.critters.ProtoNameCritterType;
import org.hyperic.hq.grouping.prop.CritterProp;
import org.hyperic.hq.grouping.prop.CritterPropDescription;
import org.hyperic.hq.grouping.prop.CritterPropType;
import org.hyperic.hq.grouping.prop.StringCritterProp;
import org.hyperic.ui.tapestry.components.BaseComponent;

public abstract class CritterEditor 
    extends BaseComponent 
    implements PageBeginRenderListener
{
    private final Log _log = LogFactory.getLog(CritterEditor.class);
    
    // Critter we are currently editing (or -1 if not)
    @Persist
    public abstract void setEditingCritterIndex(int idx);
    public abstract int getEditingCritterIndex();

    // Props we are currently editing.  This is a copy from the critter, since
    // we want to allow people to 'cancel' editing a single critter.
    @Persist
    public abstract void setEditingProps(List props);
    public abstract List getEditingProps();
    
    // A list of {@link EditableCritter}s 
    @Persist
    public abstract void setCritters(List critters);
    public abstract List getCritters();

    public void savePropsClicked(IRequestCycle cycle) {
        int critterIdx = getEditingCritterIndex();
        if (critterIdx == -1) {
            _log.warn("Strange, save was called when I didn't have anything " + 
                      "open to edit!");
            return;
        }

        EditableCritter c = (EditableCritter)getCritters().get(critterIdx);
        
        // Validate that the submitted props are valid for a real critter.
        EditableCritter clone = new EditableCritter(c);
        Map submittedProps = new HashMap();
        for (Iterator i=getEditingProps().iterator(); i.hasNext(); ) {
            EditableCritterProp p = (EditableCritterProp)i.next();
            
            submittedProps.put(p.getId(), p);
        }
        
        clone.setProps(submittedProps);
        Critter critter;
        try {
            critter = makeCritter(clone);
        } catch(GroupException e) {
            _log.warn("Unable to create critter of type [" + clone.getType() +
                      "] from submitted props [" + submittedProps + "]", e);
            // XXX:  TODO:  Need to re-render stuff nicely.
            return;
        }
        
        // Everything checks out, modify our list
        c.setProps(submittedProps);
        c.setConfig(critter.getConfig());
        
        
        editNoCritters();
        cycle.getResponseBuilder().updateComponent("editor");
    }
    
    public void cancelPropsClicked(IRequestCycle cycle) {
        editNoCritters();
        cycle.getResponseBuilder().updateComponent("editor");
    }
    
    /**
     * Returns true if we are currently editing the critter that is returned
     * from getCritter() (i.e. the one we are iterating over in the template)
     */
    public boolean isEditingCritter() {
        EditableCritter c = getCritter();
        
        return c != null && c.getIndex() == getEditingCritterIndex();
    }
    
    public void critterSubmit(IRequestCycle cycle) {
        _log.info("Editing props:");
        for (Iterator i=getEditingProps().iterator(); i.hasNext(); ) {
            EditableCritterProp p = (EditableCritterProp)i.next();
            
            _log.info("Prop [" + p.getName() + "]: " + p.getStringValue());
        }
        cycle.getResponseBuilder().updateComponent("propEdit");
    }
    
    /**
     * Make up a new critter prop with a default value. 
     */
    private EditableCritterProp 
        makeDefaultProp(final CritterPropDescription desc) 
    {
        String stringValue;
        
        if (desc.getType().equals(CritterPropType.STRING)) {
            stringValue = "";
        } else {
            throw new SystemException("Unhandled prop type [" +
                                      desc.getType().getDescription() + "]");
        }
    
        return new EditableCritterProp(desc.getId(), desc.getName(),
                                       desc.getPurpose(), desc.getType(),
                                       stringValue);
    }
    
    /**
     * Make a real critter out of an editable one.
     */
    private Critter makeCritter(EditableCritter c) 
        throws GroupException
    {
        final Map critterProps = new HashMap();

        for (Iterator i=c.getProps().values().iterator(); i.hasNext(); ) {
            EditableCritterProp p = (EditableCritterProp)i.next();
            CritterProp prop;
            
            if (p.getType().equals(CritterPropType.STRING)) {
                prop = new StringCritterProp(p.getId(), p.getStringValue());
            } else {
                _log.warn("Unhandled prop type " + 
                          p.getType().getDescription());
                throw new RuntimeException("Unhandled prop type [" + 
                                           p.getType().getDescription() + "]");
            }
            critterProps.put(p.getId(), prop);
        }
        
        return c.getType().newInstance(critterProps);
    }
    
    public boolean getShowPlusSigns() {
        return getEditingCritterIndex() == -1;
    }
    
    public void addCritterClicked(IRequestCycle cycle, int idx) {
        _log.info("Add critter clicked on index = " + idx);
        cycle.getResponseBuilder().updateComponent("critters");
    }    
    
    // Used for looping
    public abstract void setProp(EditableCritterProp prop);
    public abstract EditableCritterProp getProp();

    // Used for looping
    public abstract void setCritter(EditableCritter critter);
    public abstract EditableCritter getCritter();
 
    public void pageBeginRender(PageEvent event) {
        _log.info("Doing page render");
        if (getCritters() == null) 
            initCritters();
    }
    
    private void editNoCritters(){ 
        setEditingCritterIndex(-1);
        setEditingProps(Collections.EMPTY_LIST);
    }

    private void editCritter(int idx) {
        setEditingCritterIndex(idx);
        EditableCritter c = (EditableCritter)getCritters().get(idx);
        List editingProps = new ArrayList(c.getProps().size());
        
        for (Iterator i=c.getProps().values().iterator(); i.hasNext(); ) {
            EditableCritterProp p = (EditableCritterProp)i.next();
            
            editingProps.add(new EditableCritterProp(p));
        }
        setEditingProps(editingProps);
    }
    
    private void initCritters() {
        _log.info("Initializing critters");

        ProtoNameCritterType protoType = new ProtoNameCritterType();
        Map editableProps = new HashMap();
        
        for (Iterator i = protoType.getPropDescriptions().iterator(); 
             i.hasNext(); ) 
        {
            final CritterPropDescription desc = 
                (CritterPropDescription)i.next();
     
            editableProps.put(desc.getId(), makeDefaultProp(desc));
        }
        
        List res = new ArrayList();

        try {
            EditableCritter editCritter =
                new EditableCritter(0, protoType, editableProps);
        
            Critter c = makeCritter(editCritter);
            editCritter.setConfig(c.getConfig());
            res.add(editCritter);
   
            editCritter = new EditableCritter(1, protoType, 
                                              new HashMap(editableProps)); 
            c = makeCritter(editCritter);
            editCritter.setConfig(c.getConfig());
            res.add(editCritter);
            setCritters(res);
        } catch(GroupException e) {
            _log.warn("Unable to create critter", e);
            setCritters(Collections.EMPTY_LIST);
        }

        editCritter(0);
    }
    
    
    public void renderComponent(IMarkupWriter writer, IRequestCycle cycle){
        super.renderComponent(writer, cycle);
        if (!cycle.isRewinding()) {
            _log.info("Rendered non re-wound");
        } else {
            _log.info("Rendered rewound");
        }
    }
}
