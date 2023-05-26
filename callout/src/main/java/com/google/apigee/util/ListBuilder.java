package com.google.apigee.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ListBuilder {
  List<String> list;

    public ListBuilder() {
      list = new ArrayList<String>();
    }

    public List<String> processDocument(Document document) {
      return processElement(document.getDocumentElement(), "", 0);
    }

    private List<String> processElement(Element element, String context, int index) {
      String contextForThisElement =
          ((context.equals("")) ? "" : context + ".") + element.getNodeName();

      list.add(contextForThisElement);

      String contextForChildren =
          (index > 0) ? contextForThisElement + "." + index : contextForThisElement;

      NodeList nodeList = element.getChildNodes();
      int len = nodeList.getLength();
      if (len > 0) {
        Map<String, Integer> seen = new HashMap<String, Integer>();

        // two passes.
        // 1. find all the multiply-occuring children.
        for (int i = 0; i < len; i++) {
          Node node = nodeList.item(i);
          if (node.getNodeType() == Node.ELEMENT_NODE) {
            String name = node.getNodeName();
            Integer count = seen.get(name);
            seen.put(name, (count == null) ? 1 : count + 1);
          }
        }

        Map<String, Integer> isarray =
            seen.entrySet().stream()
                .filter(a -> a.getValue() > 1)
                .collect(Collectors.toMap(e -> e.getKey(), e -> 1));

        // 2. emit the context path and value.
        for (int i = 0; i < len; i++) {
          Node node = nodeList.item(i);
          if (node.getNodeType() == Node.ELEMENT_NODE) {
            String name = node.getNodeName();
            if (isarray.containsKey(name)) {
              Integer count = isarray.get(name);
              processElement((Element) node, contextForChildren, count);
              isarray.put(name, count + 1);
            } else {
              processElement((Element) node, contextForChildren, 0);
            }
          }
        }
      }
      return list;
    }
  }
