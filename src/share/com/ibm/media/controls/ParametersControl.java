
package com.ibm.media.controls;

import java.awt.*;
import java.util.*;

import javax.media.*;

public class ParametersControl implements Control {

  Hashtable parameters = new Hashtable();

  /**
   * Returns the parameter's value. If the parameter was not set or
   * doesn't have a default value, returns null.
   *
   * @param param  the parameter's name
   * @return  the parameter's value or null if wasn't set
   */
  public String get(String param) {
    
    return (String)parameters.get(param);
  }

  public void set(String param, String value) {

    parameters.remove(param); // first remove any value that already exist
    parameters.put(param, value);
  }

  /**
   * Get the <code>Component</code> associated with this
   * <code>Control</code> object.
   * For example, this method might return
   * a slider for volume control or a panel containing radio buttons for 
   * CODEC control.
   * The <code>getControlComponent</code> method can return
   * <CODE>null</CODE> if there is no GUI control for
   * this <code>Control</code>.
   */
  public Component getControlComponent() {

    // not implemented yet
    return null;
  }

}
