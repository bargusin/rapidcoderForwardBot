package ru.rapidcoder.forward.bot.component;

public class UserSettings {

    private boolean waitingForTextInput = false;
    private String expectedInputType;
    private TypedField<Boolean> fieldBoolean = new TypedField<>("fieldBoolean", Boolean.class);
    private TypedField<String> fieldString = new TypedField<>("fieldString", String.class);

    public boolean isWaitingForTextInput() {
        return waitingForTextInput;
    }

    public void setWaitingForTextInput(boolean waitingForTextInput) {
        this.waitingForTextInput = waitingForTextInput;
    }

    public String getExpectedInputType() {
        return expectedInputType;
    }

    public void setExpectedInputType(String expectedInputType) {
        this.expectedInputType = expectedInputType;
    }

    public TypedField<Boolean> getFieldBoolean() {
        return fieldBoolean;
    }

    public void setFieldBoolean(TypedField<Boolean> fieldBoolean) {
        this.fieldBoolean = fieldBoolean;
    }

    public TypedField<String> getFieldString() {
        return fieldString;
    }

    public void setFieldString(TypedField<String> fieldString) {
        this.fieldString = fieldString;
    }

    public static class TypedField<T> {
        private final String fieldName;
        private final Class<T> fieldType;
        private T value;

        public TypedField(String fieldName, Class<T> fieldType) {
            this.fieldName = fieldName;
            this.fieldType = fieldType;
        }

        public TypedField(String fieldName, Class<T> fieldType, T value) {
            this.fieldName = fieldName;
            this.fieldType = fieldType;
            this.value = value;
        }

        public String getFieldName() {
            return fieldName;
        }

        public Class<T> getFieldType() {
            return fieldType;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public boolean isType(Class<?> type) {
            return fieldType.equals(type);
        }

        @Override
        public String toString() {
            return String.format("%s: %s = %s", fieldName, fieldType.getSimpleName(), value);
        }
    }
}
