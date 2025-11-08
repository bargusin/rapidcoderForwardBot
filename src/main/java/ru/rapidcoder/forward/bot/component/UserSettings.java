package ru.rapidcoder.forward.bot.component;

public class UserSettings {

    private boolean waitingForTextInput = false;
    private String expectedInputType;
    private TypedField<Boolean> fieldBoolean = new TypedField<>("fieldBoolean", Boolean.class, false);

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

    public static class TypedField<T> {
        private final String fieldName;
        private final Class<T> fieldType;
        private T value;

        public TypedField(String fieldName, Class<T> fieldType, T value) {
            this.fieldName = fieldName;
            this.fieldType = fieldType;
            this.value = value;
        }

        public String getFieldName() {
            return fieldName;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format("%s: %s = %s", fieldName, fieldType.getSimpleName(), value);
        }
    }
}
