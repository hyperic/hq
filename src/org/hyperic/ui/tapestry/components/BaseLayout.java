package org.hyperic.ui.tapestry.components;

import java.util.ArrayList;
import java.util.List;

import org.apache.tapestry.IAsset;
import org.apache.tapestry.annotations.Asset;
import org.apache.tapestry.components.Block;

public abstract class BaseLayout extends BaseComponent {
    public static final String LAYOUT_PANEL_LEFT = "left";

    public static final String LAYOUT_PANEL_CENTER = "center";

    public static final String LAYOUT_PANEL_RIGHT = "right";

    /**
     * 
     * @return the left panel block
     */
    public Block getLeftBlock() {
        return (Block) this.getContainer().getComponent(LAYOUT_PANEL_LEFT);
    }

    /**
     * 
     * @return the center panel block
     */
    public Block getCenterBlock() {
        return (Block) this.getContainer().getComponent(LAYOUT_PANEL_CENTER);
    }

    /**
     * 
     * @return the right panel block
     */
    public Block getRightBlock() {
        return (Block) this.getContainer().getComponent(LAYOUT_PANEL_RIGHT);
    }

    /**
     * Dojo look and feel theme name
     */
    public static final String DOJO_TUNDRA_THEME = "tundra";

    /**
     * Dojo Version
     */
    public static final String DOJO_VERSION = "1.1";

    @Asset("context:js/dojo/" + DOJO_VERSION + "/dojo/resources/dojo.css")
    public abstract IAsset getDojoStyleSheet();

    //Unused currently
    @Asset("context:js/dojo/" + DOJO_VERSION + "/dijit/themes/dijit.css")
    public abstract IAsset getDijitStyleSheet();

    @Asset("context:js/dojo/" + DOJO_VERSION
            + "/dijit/themes/tundra/tundra.css")
    public abstract IAsset getTundraStyleSheet();

    //Unused currently 
    @Asset("context:js/dojo/" + DOJO_VERSION + "/dojox/grid/_grid/Grid.css")
    public abstract IAsset getGridStyleSheet();

    @Asset("context:js/dojo/" + DOJO_VERSION
            + "/dojox/grid/_grid/tundraGrid.css")
    public abstract IAsset getGridTundraStyleSheet();

    public List<IAsset> getDojoTundraThemeSheets() {
        List<IAsset> sheets = new ArrayList<IAsset>();
        sheets.add(getTundraStyleSheet());
        return sheets;
    }

    public List<IAsset> getDojoGridTundraThemeSheets() {
        List<IAsset> sheets = new ArrayList<IAsset>();
        sheets.add(getGridTundraStyleSheet());
        return sheets;
    }

    public List<IAsset> getDojoSheets() {
        List<IAsset> sheets = new ArrayList<IAsset>();
        sheets.add(getDojoStyleSheet());
        return sheets;
    }

    public List<IAsset> getDijitSheets() {
        List<IAsset> sheets = new ArrayList<IAsset>();
        sheets.add(getDijitStyleSheet());
        return sheets;
    }

}
