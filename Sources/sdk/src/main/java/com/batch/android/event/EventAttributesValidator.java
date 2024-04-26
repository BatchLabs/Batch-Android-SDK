package com.batch.android.event;

import com.batch.android.BatchEventAttributes;
import com.batch.android.user.AttributeType;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Class that validates that a BatchEventAttributes object is valid
 */
public class EventAttributesValidator {

    /**
     * Class that hold a human readable error.
     */
    private static class ValidationError {

        private final String message;
        private final Breadcrumbs breadcrumbs;

        public ValidationError(String message, Breadcrumbs breadcrumbs) {
            this.message = message;
            this.breadcrumbs = breadcrumbs;
        }

        public String render() {
            String attributePath = String.join(".", breadcrumbs.getItems());
            if (attributePath.isEmpty()) {
                attributePath = "<attributes root>";
            }
            return attributePath + ": " + message;
        }
    }

    /**
     * Breadcrumbs is similar to an ariane thread, keeping track of where we are in an object
     * For example, ["purchased_item", "name"] is the breadcrumb for a "name" attribute in a sub object
     * attribute named "purchased_item".
     */
    private static class Breadcrumbs {

        private final List<String> items;

        public Breadcrumbs(List<String> items) {
            this.items = items;
        }

        public Breadcrumbs appending(String item) {
            List<String> mutatedItems = new ArrayList<>(items);
            mutatedItems.add(item);
            return new Breadcrumbs(mutatedItems);
        }

        public Breadcrumbs appending(int index) {
            List<String> mutatedItems = new ArrayList<>(items);
            mutatedItems.set(mutatedItems.size() - 1, mutatedItems.get(mutatedItems.size() - 1) + "[" + index + "]");
            return new Breadcrumbs(mutatedItems);
        }

        public int getDepth() {
            return items.size();
        }

        public List<String> getItems() {
            return items;
        }
    }

    private static final int LABEL_MAX_LENGTH = 200;
    private static final int TAG_MAX_LENGTH = 64;
    private static final int TAGS_MAX_COUNT = 10;
    private static final int ATTRIBUTES_MAX_COUNT = 20;
    private static final int URL_MAX_LENGTH = 2048;
    private static final int STRING_MAX_LENGTH = 200;
    private static final int ARRAY_ITEMS_MAX_COUNT = 25;
    private static final Pattern attributeNameRegexp = Pattern.compile("^[a-zA-Z0-9_]{1,30}$");

    public static boolean isEventNameValid(String eventName) {
        return attributeNameRegexp.matcher(eventName).matches();
    }

    public static List<String> computeValidationErrors(BatchEventAttributes eventData) {
        List<String> errors = new ArrayList<>();
        List<ValidationError> validationErrors = visitObject(eventData, new Breadcrumbs(new ArrayList<>()));
        for (ValidationError validationError : validationErrors) {
            errors.add(validationError.render());
        }
        return errors;
    }

    private static List<ValidationError> visitObject(BatchEventAttributes eventData, Breadcrumbs breadcrumbs) {
        int depth = breadcrumbs.getDepth();
        if (depth > 3) {
            return Collections.singletonList(
                new ValidationError("Object attributes cannot be nested in more than three levels", breadcrumbs)
            );
        }
        if (eventData.getAttributes().isEmpty() && eventData.getLabel() == null && eventData.getTags() == null) {
            return new ArrayList<>();
        }
        List<ValidationError> errors = new ArrayList<>();
        if (depth > 0) {
            if (eventData.getLabel() != null) {
                errors.add(
                    new ValidationError("Labels are not allowed in sub-objects", breadcrumbs.appending("$label"))
                );
            }
            if (eventData.getTags() != null) {
                errors.add(new ValidationError("Tags are not allowed in sub-objects", breadcrumbs.appending("$tags")));
            }
        } else {
            if (eventData.getLabel() != null) {
                wrapAndMergeErrorMessages(visitLabel(eventData.getLabel()), breadcrumbs.appending("$label"), errors);
            }
            if (eventData.getTags() != null) {
                mergeErrors(visitTags(new ArrayList<>(eventData.getTags()), breadcrumbs.appending("$tags")), errors);
            }
        }
        Map<String, EventTypedAttribute> attributes = eventData.getAttributes();
        if (attributes.size() > ATTRIBUTES_MAX_COUNT) {
            errors.add(
                new ValidationError(
                    "objects cannot hold more than " + ATTRIBUTES_MAX_COUNT + " attributes",
                    breadcrumbs
                )
            );
        }
        for (Map.Entry<String, EventTypedAttribute> entry : attributes.entrySet()) {
            String attributeName = entry.getKey();
            EventTypedAttribute attributeValue = entry.getValue();
            ValidationError attributeNameError = visitAttributeName(attributeName, breadcrumbs);
            if (attributeNameError != null) {
                errors.add(attributeNameError);
                continue;
            }
            Breadcrumbs attributeBreadcrumbs = breadcrumbs.appending(attributeName);
            mergeErrors(visitAttributeValue(attributeValue, attributeBreadcrumbs), errors);
        }
        return errors;
    }

    private static ValidationError visitAttributeName(String name, Breadcrumbs breadcrumbs) {
        String baseError = "invalid attribute name '" + name + "':";
        if (!name.equals(name.toLowerCase())) {
            return new ValidationError(baseError + " object has been tampered with", breadcrumbs);
        }
        if (!attributeNameRegexp.matcher(name).matches()) {
            return new ValidationError(
                baseError +
                " please make sure that the key is made of letters, underscores and numbers only (a-zA-Z0-9_). It also can't be longer than 30 characters",
                breadcrumbs
            );
        }
        return null;
    }

    private static List<ValidationError> visitAttributeValue(EventTypedAttribute attribute, Breadcrumbs breadcrumbs) {
        List<ValidationError> errors = new ArrayList<>();
        ValidationError genericTypecastError = new ValidationError(
            "attribute is not of the right underlying type. this is an internal error and should be reported",
            breadcrumbs
        );
        switch (attribute.type) {
            case URL:
                if (attribute.value instanceof URI) {
                    mergeError(visitAttributeURLValue((URI) attribute.value, breadcrumbs), errors);
                } else {
                    errors.add(genericTypecastError);
                }
                break;
            case STRING:
                if (attribute.value instanceof String) {
                    mergeError(visitAttributeStringValue((String) attribute.value, breadcrumbs), errors);
                } else {
                    errors.add(genericTypecastError);
                }
                break;
            case DOUBLE:
            case LONG:
                if (!(attribute.value instanceof Number)) {
                    errors.add(genericTypecastError);
                }
                break;
            case BOOL:
                if (!(attribute.value instanceof Boolean)) {
                    errors.add(genericTypecastError);
                }
                break;
            case DATE:
                if (!(attribute.value instanceof Long)) {
                    errors.add(genericTypecastError);
                }
                break;
            case OBJECT_ARRAY:
            case STRING_ARRAY:
                if (attribute.value instanceof List<?>) {
                    List<?> anyArrayValue = (List<?>) attribute.value;
                    ValidationError baseArrayError = visitAttributeArrayValueBase(anyArrayValue, breadcrumbs);
                    if (baseArrayError != null) {
                        errors.add(baseArrayError);
                    } else {
                        if (attribute.type == AttributeType.OBJECT_ARRAY) {
                            try {
                                @SuppressWarnings("unchecked")
                                List<BatchEventAttributes> objectArrayValue = (List<BatchEventAttributes>) anyArrayValue;
                                mergeErrors(visitAttributeObjectArrayValue(objectArrayValue, breadcrumbs), errors);
                            } catch (ClassCastException e) {
                                errors.add(genericTypecastError);
                            }
                        } else if (attribute.type == AttributeType.STRING_ARRAY) {
                            try {
                                @SuppressWarnings("unchecked")
                                List<String> stringArrayValue = (List<String>) anyArrayValue;
                                mergeErrors(visitAttributeStringArrayValue(stringArrayValue, breadcrumbs), errors);
                            } catch (ClassCastException e) {
                                errors.add(genericTypecastError);
                            }
                        }
                    }
                } else {
                    errors.add(genericTypecastError);
                }
                break;
            case OBJECT:
                if (attribute.value instanceof BatchEventAttributes) {
                    mergeErrors(visitObject((BatchEventAttributes) attribute.value, breadcrumbs), errors);
                } else {
                    errors.add(genericTypecastError);
                }
                break;
        }
        return errors;
    }

    private static ValidationError visitAttributeArrayValueBase(List<?> value, Breadcrumbs breadcrumbs) {
        int depth = breadcrumbs.getDepth();
        if (depth > 3) {
            return new ValidationError("array attributes cannot be nested in more than three levels", breadcrumbs);
        }
        if (value.size() > ARRAY_ITEMS_MAX_COUNT) {
            return new ValidationError(
                "array attributes cannot have more than " + ARRAY_ITEMS_MAX_COUNT + " elements",
                breadcrumbs
            );
        }
        return null;
    }

    private static List<ValidationError> visitAttributeStringArrayValue(List<String> array, Breadcrumbs breadcrumbs) {
        List<ValidationError> errors = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            String value = array.get(i);
            Breadcrumbs itemBreadcrumbs = breadcrumbs.appending(i);
            mergeError(visitAttributeStringValue(value, itemBreadcrumbs), errors);
        }
        return errors;
    }

    private static List<ValidationError> visitAttributeObjectArrayValue(
        List<BatchEventAttributes> array,
        Breadcrumbs breadcrumbs
    ) {
        List<ValidationError> errors = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            BatchEventAttributes value = array.get(i);
            Breadcrumbs itemBreadcrumbs = breadcrumbs.appending(i);
            mergeErrors(visitObject(value, itemBreadcrumbs), errors);
        }
        return errors;
    }

    private static ValidationError visitAttributeStringValue(String value, Breadcrumbs breadcrumbs) {
        if (value.trim().isEmpty()) {
            return new ValidationError("string attribute cannot be empty or made of whitespace", breadcrumbs);
        }
        if (value.length() > STRING_MAX_LENGTH) {
            return new ValidationError(
                "string attribute cannot be longer than " + STRING_MAX_LENGTH + " characters",
                breadcrumbs
            );
        }
        if (value.contains("\n")) {
            return new ValidationError("string attribute cannot be multiline", breadcrumbs);
        }
        return null;
    }

    private static ValidationError visitAttributeURLValue(URI value, Breadcrumbs breadcrumbs) {
        if (value.toString().length() > URL_MAX_LENGTH) {
            return new ValidationError(
                "URL attributes cannot be longer than " + URL_MAX_LENGTH + " characters",
                breadcrumbs
            );
        }
        if (value.getScheme() == null || value.getAuthority() == null) {
            return new ValidationError(
                "URL attributes must follow the format 'scheme://[authority][path][?query][#fragment]'",
                breadcrumbs
            );
        }
        return null;
    }

    private static List<String> visitLabel(String label) {
        List<String> errors = new ArrayList<>();
        if (label.length() > LABEL_MAX_LENGTH) {
            errors.add("cannot be longer than 200 characters");
        }
        if (label.trim().isEmpty()) {
            errors.add("cannot be empty or only made of whitespace");
        }
        if (label.contains("\n")) {
            errors.add("cannot be multiline");
        }
        return errors;
    }

    private static List<ValidationError> visitTags(List<String> tags, Breadcrumbs breadcrumbs) {
        List<ValidationError> errors = new ArrayList<>();
        if (tags.size() > TAGS_MAX_COUNT) {
            errors.add(new ValidationError("must not contain more than " + TAGS_MAX_COUNT + " values", breadcrumbs));
        }
        for (int i = 0; i < tags.size(); i++) {
            String tag = tags.get(i);
            String error = visitTag(tag);
            if (error != null) {
                errors.add(new ValidationError(error, breadcrumbs.appending(i)));
            }
        }
        return errors;
    }

    private static String visitTag(String tag) {
        if (tag.trim().isEmpty()) {
            return "tag cannot be empty or made of whitespace";
        }
        if (tag.length() > TAG_MAX_LENGTH) {
            return "tag cannot be longer than " + TAG_MAX_LENGTH;
        }
        if (tag.contains("\n")) {
            return "tag cannot be multiline";
        }
        return null;
    }

    private static void wrapAndMergeErrorMessages(
        List<String> messages,
        Breadcrumbs breadcrumbs,
        List<ValidationError> accumulator
    ) {
        for (String message : messages) {
            accumulator.add(new ValidationError(message, breadcrumbs));
        }
    }

    private static void mergeError(ValidationError error, List<ValidationError> accumulator) {
        if (error != null) {
            accumulator.add(error);
        }
    }

    private static void mergeErrors(List<ValidationError> errors, List<ValidationError> accumulator) {
        accumulator.addAll(errors);
    }
}
