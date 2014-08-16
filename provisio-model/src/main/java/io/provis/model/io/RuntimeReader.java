package io.provis.model.io;

import io.provis.model.ActionDescriptor;
import io.provis.model.ArtifactSet;
import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioningAction;
import io.provis.model.Runtime;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class RuntimeReader {

  private final XStream xstream;
  private final Map<String, ActionDescriptor> actionMap;
  private final Map<String, String> versionMap;

  public RuntimeReader() {
    this(Collections.<ActionDescriptor> emptyList(), Collections.<String, String> emptyMap());
  }

  public RuntimeReader(List<ActionDescriptor> actions) {
    this(actions, Collections.<String, String> emptyMap());
  }

  public RuntimeReader(List<ActionDescriptor> actions, Map<String, String> versionMap) {
    xstream = new XStream();
    xstream.alias("assembly", Runtime.class);
    xstream.alias("runtime", Runtime.class);
    xstream.useAttributeFor(Runtime.class, "id");
    xstream.addImplicitCollection(Runtime.class, "artifactSets");

    xstream.alias("artifactSet", ArtifactSet.class);
    xstream.aliasAttribute(ArtifactSet.class, "directory", "to");
    xstream.addImplicitCollection(ArtifactSet.class, "artifacts");

    xstream.alias("artifact", ProvisioArtifact.class);

    xstream.registerConverter(new RuntimeConverter());
    xstream.registerConverter(new ArtifactConverter());

    for (ActionDescriptor action : actions) {

      // Runtime actions
      // xstream.alias(action.getName(), action.getImplementation());

      // Inform XStream about the attributes we care about for this action
      for (String attributeForProperty : action.attributes()) {
        xstream.useAttributeFor(action.getImplementation(), attributeForProperty);
      }
    }

    this.versionMap = versionMap;
    this.actionMap = Maps.newHashMap();
    for (ActionDescriptor actionDescriptor : actions) {
      this.actionMap.put(actionDescriptor.getName(), actionDescriptor);
    }
  }

  public Runtime read(InputStream inputStream, Map<String, String> variables) {
    return (Runtime) xstream.fromXML(new InterpolatingInputStream(inputStream, variables));
  }

  public Runtime read(InputStream inputStream) {
    return (Runtime) xstream.fromXML(inputStream);
  }

  public class RuntimeConverter implements Converter {

    @Override
    public boolean canConvert(Class type) {
      if (Runtime.class.isAssignableFrom(type)) {
        return true;
      }
      return false;
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      Runtime runtime = new Runtime();
      while (reader.hasMoreChildren()) {
        reader.moveDown();
        if (reader.getNodeName().equals("artifactSet")) {
          runtime.addArtifactSet((ArtifactSet) context.convertAnother(runtime, ArtifactSet.class));
        } else {
          // We have an arbitrary action
          String actionName = reader.getNodeName();
          ActionDescriptor actionDescriptor = actionMap.get(actionName);
          if (actionDescriptor != null) {
            runtime.addAction((ProvisioningAction) context.convertAnother(runtime, actionDescriptor.getImplementation()));
          }
        }
        reader.moveUp();
      }
      return runtime;
    }
  }

  public class ArtifactConverter implements Converter {

    @Override
    public boolean canConvert(Class type) {
      if (ProvisioArtifact.class.isAssignableFrom(type)) {
        return true;
      }
      return false;
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      //
      // Coordinates have the following form:
      //
      // <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>
      //
      // If the user is specifying versionless coordinates they are expecting to glean the version from their dependency
      // management system, like in Maven
      //
      String coordinate = reader.getAttribute("id");
      int coordinateSegments = coordinate.length() - coordinate.replace(":", "").length() + 1;
      if (coordinateSegments == 2) {
        //
        // We only have groupId:artifactId where the extension defaults to "jar" which we need to add because the
        // versionMap is created with the full versionless coordinate.
        //
        coordinate += ":jar";
      }
      //
      // Look at the last element of coordinate and determine if it's a version. If it's not then we need to consult
      // the versionMap to find the appropriate version.
      //
      String lastElement = coordinate.substring(coordinate.lastIndexOf(":") + 1);
      if (!Character.isDigit(lastElement.charAt(0))) {
        String version = versionMap.get(coordinate);
        if (version != null) {
          coordinate += ":" + version;
        }
      }
      ProvisioArtifact artifact = new ProvisioArtifact(coordinate);
      while (reader.hasMoreChildren()) {
        reader.moveDown();
        String actionName = reader.getNodeName();
        ActionDescriptor actionDescriptor = actionMap.get(actionName);
        if (actionDescriptor != null) {
          artifact.addAction((ProvisioningAction) context.convertAnother(artifact, actionDescriptor.getImplementation()));
        }
        reader.moveUp();
      }
      return artifact;
    }
  }
}
