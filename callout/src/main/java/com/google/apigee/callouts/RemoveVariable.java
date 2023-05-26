// RemoveVariable.java
//
// This is the source code for a callout for Apigee, which removes variables from the context.
// Today (26 May 2023) it is not possible to remove variables with builtin mechanisms.
//
// This might be necessary if executing an AccessEntity callout twice in the same proxy.
//
// ------------------------------------------------------------------

package com.google.apigee.callouts;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import com.google.apigee.util.ListBuilder;
import com.google.apigee.util.XmlUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Document;

public class RemoveVariable implements Execution {
  private static final Pattern variableReferencePattern =
      Pattern.compile("(.*?)\\{([^\\{\\} :][^\\{\\} ]*?)\\}(.*?)");

  private Map<String, String> properties; // read-only

  public static Map<String, String> genericizeMap(Map properties) {
    // convert an untyped Map to a generic map
    Map<String, String> m = new HashMap<String, String>();
    Iterator iterator = properties.keySet().iterator();
    while (iterator.hasNext()) {
      Object key = iterator.next();
      Object value = properties.get(key);
      if ((key instanceof String) && (value instanceof String)) {
        m.put((String) key, (String) value);
      }
    }
    return Collections.unmodifiableMap(m);
  }

  public RemoveVariable(Map properties) {
    this.properties = genericizeMap(properties);
  }

  protected boolean _getBooleanProperty(
      MessageContext msgCtxt, String propName, boolean defaultValue) {
    String flag = (String) this.properties.get(propName);
    if (flag != null) flag = flag.trim();
    if (flag == null || flag.equals("")) {
      return defaultValue;
    }
    flag = resolveVariableReferences(flag, msgCtxt);
    if (flag == null || flag.equals("")) {
      return defaultValue;
    }
    return flag.equalsIgnoreCase("true");
  }

  protected boolean getRecurse(MessageContext msgCtxt) {
    return _getBooleanProperty(msgCtxt, "recurse-access-entity", false);
  }

  protected List<String> getVariablesToRemove(MessageContext msgCtxt) throws IllegalStateException {
    // Retrieve a value from a named property, as a string.
    String value = (String) this.properties.get("variables");
    if (value != null) value = value.trim();
    if (value == null || value.equals("")) {
      throw new IllegalStateException("variables property resolves to null or empty.");
    }
    value = resolveVariableReferences(value, msgCtxt);
    if (value == null || value.equals("")) {
      throw new IllegalStateException("variables property resolves to null or empty.");
    }
    return Arrays.asList(value.split("\\s*,\\s*"));
  }

  /*
   *
   * If a property holds one or more segments wrapped with begin and end
   * curlies, eg, {apiproxy.name}, then "resolve" the value by de-referencing
   * the context variable whose name appears between the curlies.
   **/
  protected String resolveVariableReferences(String spec, MessageContext msgCtxt) {
    if (spec == null || spec.equals("")) return spec;
    Matcher matcher = variableReferencePattern.matcher(spec);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(sb, "");
      sb.append(matcher.group(1));
      String ref = matcher.group(2);
      String[] parts = ref.split(":", 2);
      Object v = msgCtxt.getVariable(parts[0]);
      if (v != null) {
        sb.append((String) v);
      } else if (parts.length > 1) {
        sb.append(parts[1]);
      }
      sb.append(matcher.group(3));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  protected void removeVariable(
      final MessageContext msgCtxt, final String varname, boolean recurse) {
    if (recurse) {
      // try to deserialize existing content as XML, then remove all inferred variables
      try {
        Object value = msgCtxt.getVariable(varname);
        if (value != null) {
          Document document = XmlUtils.parseXml(value.toString());
          List<String> names = new ListBuilder().processDocument(document);
          for (String n : names) {
            msgCtxt.removeVariable(varname + "." + n);
          }
        }
      } catch (java.lang.Exception exc1) {
        msgCtxt.setVariable("warning", exc1.toString());
      }
    }
    msgCtxt.removeVariable(varname);
  }

  public ExecutionResult execute(final MessageContext msgCtxt, final ExecutionContext execContext) {
    boolean wantRecurse = getRecurse(msgCtxt);
    for (String variableToRemove : getVariablesToRemove(msgCtxt)) {
      removeVariable(msgCtxt, variableToRemove, wantRecurse);
    }
    return ExecutionResult.SUCCESS;
  }
}
