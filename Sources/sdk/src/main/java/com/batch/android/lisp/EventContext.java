package com.batch.android.lisp;

import androidx.annotation.Nullable;
import com.batch.android.BatchEventData;
import com.batch.android.BatchEventDataPrivateHelper;
import java.util.Map;
import java.util.Set;

public final class EventContext implements EvaluationContext {

  private final String eventName;
  private String eventLabel;
  private BatchEventData data;
  private final boolean isPublicEvent;

  public EventContext(
    String name,
    @Nullable String label,
    @Nullable BatchEventData data
  ) {
    eventName = name;
    if (label != null) {
      eventLabel = label;
    }
    if (data != null) {
      this.data = data;
    }
    isPublicEvent = eventName.startsWith("E.");
  }

  @Override
  public Value resolveVariableNamed(String variableName) {
    if (!variableName.startsWith("e.")) {
      return null;
    }

    switch (variableName) {
      case "e.name":
        return new PrimitiveValue(eventName);
      case "e.label":
        return label();
      case "e.tags":
        return tags();
      case "e.converted":
        return converted();
      default:
        if (variableName.startsWith("e.attr['")) {
          return dataForRawVariableName(variableName);
        }
    }

    return null;
  }

  private Value label() {
    if (isPublicEvent && eventLabel != null) {
      return new PrimitiveValue(eventLabel);
    }

    return PrimitiveValue.nilValue();
  }

  private Value tags() {
    if (isPublicEvent && data != null) {
      Set<String> tags = BatchEventDataPrivateHelper.getTagsFromEventData(data);

      if (tags != null && tags.size() > 0) {
        return new PrimitiveValue(tags);
      }
    }

    return PrimitiveValue.nilValue();
  }

  private Value converted() {
    if (isPublicEvent && data != null) {
      return new PrimitiveValue(
        BatchEventDataPrivateHelper.getConvertedFromLegacyAPIFromEvent(data)
      );
    }

    return PrimitiveValue.nilValue();
  }

  private Value dataForRawVariableName(String variableName) {
    if (isPublicEvent && data != null) {
      Map<String, Value> attributes = BatchEventDataPrivateHelper.getAttributesFromEventData(
        data
      );

      if (attributes == null || attributes.size() == 0) {
        return PrimitiveValue.nilValue();
      }

      String wantedAttributeName = extractAttributeFromVariableName(
        variableName
      );

      if (wantedAttributeName == null) {
        return PrimitiveValue.nilValue();
      }

      Value value = attributes.get(wantedAttributeName);
      if (value != null) {
        return value;
      }
    }

    return PrimitiveValue.nilValue();
  }

  @Nullable
  private String extractAttributeFromVariableName(String variableName) {
    // Assume that we already checked that the string is prefixed by e.attr["

    if (variableName.endsWith("']") && variableName.length() > 10) {
      String extractedName = variableName.substring(
        8,
        8 + variableName.length() - 10
      );
      if (extractedName.length() > 0) {
        return extractedName;
      }
    }

    return null;
  }
}
