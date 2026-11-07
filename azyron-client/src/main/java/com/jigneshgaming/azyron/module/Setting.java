package com.jigneshgaming.azyron.module;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Strongly-typed configurable setting system for AZYRON CLIENT Mod Settings UI & JSON persistence.
 */
public abstract class Setting<T> {
    private final String key;
    private final String label;
    private final String description;
    protected final T defaultValue;
    protected T value;

    protected Setting(String key, String label, String description, T defaultValue) {
        this.key = key;
        this.label = label;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getKey() { return key; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public T getValue() { return value; }
    public T getDefaultValue() { return defaultValue; }

    public void setValue(T value) {
        if (value != null) {
            this.value = value;
        }
    }

    public void reset() {
        this.value = defaultValue;
    }

    public abstract String serialize();
    public abstract void deserialize(String raw);

    public static final class BooleanSetting extends Setting<Boolean> {
        public BooleanSetting(String key, String label, String description, boolean defaultValue) {
            super(key, label, description, defaultValue);
        }

        public void toggle() {
            this.value = !this.value;
        }

        @Override
        public String serialize() {
            return Boolean.toString(value);
        }

        @Override
        public void deserialize(String raw) {
            if (raw != null) {
                this.value = Boolean.parseBoolean(raw.trim());
            }
        }
    }

    public static final class NumberSetting extends Setting<Double> {
        private final double min;
        private final double max;
        private final double step;

        public NumberSetting(String key, String label, String description, double defaultValue, double min, double max, double step) {
            super(key, label, description, defaultValue);
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public double getMin() { return min; }
        public double getMax() { return max; }
        public double getStep() { return step; }

        public int getIntValue() {
            return (int) Math.round(value);
        }

        public float getFloatValue() {
            return value.floatValue();
        }

        public float getNormalized() {
            if (max <= min) {
                return 0.0f;
            }
            return (float) ((value - min) / (max - min));
        }

        public void setFromNormalized(float t) {
            double clamped = Math.max(0.0, Math.min(1.0, t));
            double rawVal = min + clamped * (max - min);
            if (step > 0.0) {
                rawVal = Math.round(rawVal / step) * step;
            }
            setValue(Math.max(min, Math.min(max, rawVal)));
        }

        @Override
        public void setValue(Double val) {
            if (val != null && !val.isNaN()) {
                this.value = Math.max(min, Math.min(max, val));
            }
        }

        public String getFormattedValue() {
            if (step >= 1.0) {
                return Integer.toString(getIntValue());
            }
            return String.format(Locale.ROOT, "%.2f", value);
        }

        @Override
        public String serialize() {
            return String.format(Locale.ROOT, "%.4f", value);
        }

        @Override
        public void deserialize(String raw) {
            try {
                setValue(Double.parseDouble(raw.trim()));
            } catch (Exception ignored) {
            }
        }
    }

    public static final class ModeSetting extends Setting<String> {
        private final List<String> modes;

        public ModeSetting(String key, String label, String description, String defaultMode, String... modes) {
            super(key, label, description, defaultMode);
            this.modes = Arrays.asList(modes);
        }

        public List<String> getModes() {
            return modes;
        }

        public void cycleNext() {
            int idx = modes.indexOf(value);
            this.value = modes.get((idx + 1) % modes.size());
        }

        public void cyclePrev() {
            int idx = modes.indexOf(value);
            this.value = modes.get((idx - 1 + modes.size()) % modes.size());
        }

        @Override
        public void setValue(String val) {
            if (val != null) {
                for (String m : modes) {
                    if (m.equalsIgnoreCase(val.trim())) {
                        this.value = m;
                        return;
                    }
                }
            }
        }

        @Override
        public String serialize() {
            return value;
        }

        @Override
        public void deserialize(String raw) {
            setValue(raw);
        }
    }
}
